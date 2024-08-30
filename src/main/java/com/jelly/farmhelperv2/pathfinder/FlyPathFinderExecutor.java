package com.jelly.farmhelperv2.pathfinder;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.common.collect.EvictingQueue;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.feature.impl.LagDetector;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.mixin.client.EntityPlayerAccessor;
import com.jelly.farmhelperv2.mixin.pathfinder.PathfinderAccessor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FlyPathFinderExecutor {
    private static FlyPathFinderExecutor instance;

    public static FlyPathFinderExecutor getInstance() {
        if (instance == null) {
            instance = new FlyPathFinderExecutor();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private Thread pathfinderTask;
    private Thread timeoutTask;
    @Getter
    private State state = State.NONE;
    private int tick = 0;
    private final CopyOnWriteArrayList<Vec3> path = new CopyOnWriteArrayList<>();
    private Vec3 target;
    private Entity targetEntity;
    private boolean follow;
    private boolean smooth;
    @Setter
    private boolean sprinting = false;
    @Setter
    @Getter
    private boolean useAOTV = false;
    @Getter
    private long lastTpTime = 0;
    private final FlyNodeProcessor flyNodeProcessor = new FlyNodeProcessor();
    private final PathFinder pathFinder = new PathFinder(flyNodeProcessor);
    @Getter
    private float neededYaw = Integer.MIN_VALUE;
    private final int MAX_DISTANCE = 1500;
    private int ticksAtLastPos = 0;
    private Vec3 lastPosCheck = new Vec3(0, 0, 0);
    private float yModifier = 0;
    private final Clock stuckBreak = new Clock();
    private final Clock stuckCheckDelay = new Clock();
    @Getter
    @Setter
    private boolean dontRotate = false;
    private final EvictingQueue<Position> lastPositions = EvictingQueue.create(100);
    private Position lastPosition;


    public void findPath(Vec3 pos, boolean follow, boolean smooth) {
        if (mc.thePlayer.getDistance(pos.xCoord, pos.yCoord, pos.zCoord) < 1) {
            stop();
            LogUtils.sendSuccess("Already at destination");
            return;
        }
        lastPosition = new Position(mc.thePlayer.getPosition(), new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
        lastPositions.add(lastPosition);
        state = State.CALCULATING;
        this.follow = follow;
        this.target = pos;
        this.smooth = smooth;
        try {
            pathfinderTask = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                float maxDistance = (float) Math.min(mc.thePlayer.getPositionVector().distanceTo(pos) + 5, MAX_DISTANCE);
                LogUtils.sendDebug("Distance to target: " + maxDistance);
                LogUtils.sendDebug("Pathfinding to " + pos);
                PathEntity route = ((PathfinderAccessor) pathFinder).createPath(mc.theWorld, mc.thePlayer, pos.xCoord, pos.yCoord, pos.zCoord, maxDistance);
                if (route == null) {
                    LogUtils.sendError("Failed to find path to " + pos);
                    state = State.FAILED;
                    double distance = mc.thePlayer.getPositionVector().distanceTo(this.target);
                    if (distance > MAX_DISTANCE) {
                        LogUtils.sendError("Distance to target is too far. Distance: " + distance + ", Max distance: " + MAX_DISTANCE);
                        stop();
                    }
                    return;
                }
                if (!isRunning()) {
                    stop();
                    return;
                }
                LogUtils.sendDebug("Pathfinding took " + (System.currentTimeMillis() - startTime) + "ms");
                startTime = System.currentTimeMillis();
                List<Vec3> finalRoute = new ArrayList<>();
                for (int i = 0; i < route.getCurrentPathLength(); i++) {
                    PathPoint pathPoint = route.getPathPointFromIndex(i);
                    finalRoute.add(new Vec3(pathPoint.xCoord, pathPoint.yCoord, pathPoint.zCoord));
                }
                if (smooth) {
                    finalRoute = smoothPath(finalRoute);
                }
                this.path.clear();
                this.path.addAll(finalRoute.stream().map(vec3 -> vec3.addVector(0.5f, 0.15, 0.5)).collect(Collectors.toCollection(CopyOnWriteArrayList::new)));
                state = State.PATHING;
                LogUtils.sendDebug("Path smoothing took " + (System.currentTimeMillis() - startTime) + "ms");
                if (timeoutTask != null) {
                    timeoutTask.stop();
                }
            });
            pathfinderTask.start();
            timeoutTask = new Thread(() -> {
                try {
                    Thread.sleep(10_000);
                    if (isCalculating()) {
                        LogUtils.sendError("Pathfinding took too long");
                        RotationHandler.getInstance().reset();
                        state = State.FAILED;
                        if (pathfinderTask != null) {
                            pathfinderTask.stop();
                        }
                    }
                } catch (InterruptedException e) {
                    LogUtils.sendDebug("Pathfinding finished before timeout");
                }
            });
            timeoutTask.start();
        } catch (Exception e) {
            LogUtils.sendError("Pathfinding took too long");
            if (!this.follow)
                stop();
        }
    }

    public void findPath(Entity target, boolean follow, boolean smooth) {
        this.targetEntity = target;
        this.yModifier = 0;
        findPath(new Vec3(target.posX, target.posY, target.posZ), follow, smooth);
    }

    public void findPath(Entity target, boolean follow, boolean smooth, float yModifier, boolean dontRotate) {
        this.targetEntity = target;
        this.yModifier = yModifier;
        this.dontRotate = dontRotate;
        if (Math.abs(target.motionX) > 0.15 || Math.abs(target.motionZ) > 0.15) {
            Vec3 targetNextPos = new Vec3(target.posX + target.motionX, target.posY + target.motionY, target.posZ + target.motionZ);
            findPath(targetNextPos.addVector(0, this.yModifier, 0), follow, smooth);
        } else {
            Vec3 targetPos = new Vec3(target.posX, target.posY, target.posZ);
            Rotation rotation = RotationHandler.getInstance().getRotation(targetPos, mc.thePlayer.getPositionVector());
            Vec3 direction = AngleUtils.getVectorForRotation(0, rotation.getYaw());
            targetPos = targetPos.addVector(direction.xCoord * 1.2, 0.5, direction.zCoord * 1.2);
            findPath(targetPos.addVector(0, this.yModifier, 0), follow, smooth);
        }
    }

    public boolean isRotationInCache(float yaw, float pitch) {
        return lastPositions.stream().anyMatch(position -> position.pos.distanceSq(mc.thePlayer.getPosition()) <= 1 && Math.abs(position.rotation.getYaw() - yaw) < 1 && Math.abs(position.rotation.getPitch() - pitch) < 1);
    }

    public boolean isPositionInCache(BlockPos pos) {
        return lastPositions.stream().anyMatch(position -> position.pos.equals(pos));
    }

    private List<Vec3> smoothPath(List<Vec3> path) {
        if (path.size() < 2) {
            return path;
        }
        List<Vec3> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        int lowerIndex = 0;
        while (lowerIndex < path.size() - 2) {
            Vec3 start = path.get(lowerIndex);
            Vec3 lastValid = path.get(lowerIndex + 1);
            for (int upperIndex = lowerIndex + 2; upperIndex < path.size(); upperIndex++) {
                Vec3 end = path.get(upperIndex);
                if (traversable(start.addVector(0, 0.1, 0), end.addVector(0, 0.1, 0)) &&
                        traversable(start.addVector(0, 0.9, 0), end.addVector(0, 0.9, 0)) &&
                        traversable(start.addVector(0, 1.1, 0), end.addVector(0, 1.1, 0)) &&
                        traversable(start.addVector(0, 1.9, 0), end.addVector(0, 1.9, 0))) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = path.indexOf(lastValid);
        }

        return smoothed;
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.05, 0, 0.05),
            new Vec3(0.05, 0, 0.95),
            new Vec3(0.95, 0, 0.05),
            new Vec3(0.95, 0, 0.95)
    };

    private boolean traversable(Vec3 from, Vec3 to) {
        for (Vec3 offset : BLOCK_SIDE_MULTIPLIERS) {
            Vec3 fromVec = new Vec3(from.xCoord + offset.xCoord, from.yCoord + offset.yCoord, from.zCoord + offset.zCoord);
            Vec3 toVec = new Vec3(to.xCoord + offset.xCoord, to.yCoord + offset.yCoord, to.zCoord + offset.zCoord);
            MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(fromVec, toVec, false, true, false);

            if (trace != null) {
                return false;
            }
        }

        return true;
    }

    public boolean isPathing() {
        return state == State.PATHING;
    }

    public boolean isCalculating() {
        return state == State.CALCULATING;
    }

    public boolean isDecelerating() {
        return state == State.DECELERATING || state == State.WAITING_FOR_DECELERATION;
    }

    public boolean isRunning() {
        return isPathing() || isCalculating() || isDecelerating() || stuckBreak.isScheduled();
    }

    public boolean isPathingOrDecelerating() {
        return isPathing() || isDecelerating() || stuckBreak.isScheduled();
    }

    public void stop() {
        RotationHandler.getInstance().reset();
        path.clear();
        target = null;
        tped = true;
        aotvDelay.reset();
        targetEntity = null;
        yModifier = 0;
        lastTpTime = 0;
        state = State.NONE;
        KeyBindUtils.stopMovement(true);
        loweringRaisingDelay.reset();
        neededYaw = Integer.MIN_VALUE;
        if (pathfinderTask != null) {
            pathfinderTask.interrupt();
            pathfinderTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.interrupt();
            timeoutTask = null;
        }
        ticksAtLastPos = 0;
        lastPosCheck = new Vec3(0, 0, 0);
        stuckBreak.reset();
        stuckCheckDelay.reset();
        dontRotate = false;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (isRunning()) {
            stop();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (path.isEmpty()) return;
        if (target == null) return;
        tick = (tick + 1) % 12;

        if (tick != 0) return;
        if (isCalculating()) return;
        if (isDecelerating()) return;

        if (!this.follow) return;
        if (this.targetEntity != null) {
            findPath(this.targetEntity, true, this.smooth, this.yModifier, this.dontRotate);
        } else {
            findPath(this.target, true, this.smooth);
        }
    }

    private final Clock loweringRaisingDelay = new Clock();
    private final Clock aotvDelay = new Clock();
    private boolean tped = true;

    @SubscribeEvent
    public void onTickNeededYaw(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (state == State.NONE) return;
        if (state == State.FAILED) {
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            neededYaw = Integer.MIN_VALUE;
            return;
        }
        ArrayList<Vec3> copyPath = new ArrayList<>(path);
        if (copyPath.isEmpty()) {
            KeyBindUtils.stopMovement(true);
            return;
        }
        if (stuckBreak.isScheduled() && !stuckBreak.passed()) return;
        Vec3 current = mc.thePlayer.getPositionVector();
        BlockPos currentPos = mc.thePlayer.getPosition();
        lastPosition = new Position(currentPos, new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
        lastPositions.add(lastPosition);
        if (checkForStuck(current)) {
            LogUtils.sendDebug("Stuck");
            stuckBreak.schedule(800);
            float rotationToEscape;
            for (rotationToEscape = 0; rotationToEscape < 360; rotationToEscape += 20) {
                Vec3 escape = current.addVector(Math.cos(Math.toRadians(rotationToEscape)), 0, Math.sin(Math.toRadians(rotationToEscape)));
                if (traversable(current.addVector(0, 0.1, 0), escape.addVector(0, 0.1, 0)) &&
                        traversable(current.addVector(0, 0.9, 0), escape.addVector(0, 0.9, 0)) &&
                        traversable(current.addVector(0, 1.1, 0), escape.addVector(0, 1.1, 0)) &&
                        traversable(current.addVector(0, 1.9, 0), escape.addVector(0, 1.9, 0))) {
                    break;
                }
            }
            neededYaw = rotationToEscape;
            if (FarmHelperConfig.flyPathfinderOringoCompatible) {
                List<KeyBinding> keyBindings = new ArrayList<>(KeyBindUtils.getNeededKeyPresses(neededYaw));
                keyBindings.add(mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null);
                keyBindings.add(mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
                Vec3 above = current.addVector(0, mc.thePlayer.height + 0.5f, 0);
                Vec3 below = current.addVector(0, -0.5f, 0);
                MovingObjectPosition traceAbove = mc.theWorld.rayTraceBlocks(current, above, false, true, false);
                MovingObjectPosition traceBelow = mc.theWorld.rayTraceBlocks(current, below, false, true, false);
                if (traceBelow == null || traceBelow.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                    keyBindings.add(mc.gameSettings.keyBindSneak);
                } else if (traceAbove == null || traceAbove.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                    keyBindings.add(mc.gameSettings.keyBindJump);
                }
                KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null, mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
            }
            Multithreading.schedule(() -> KeyBindUtils.stopMovement(true), 500, TimeUnit.MILLISECONDS);
            return;
        }
        Vec3 lastElem = copyPath.get(copyPath.size() - 1);
        if (targetEntity != null) {
            if (targetEntity instanceof EntityArmorStand) {
                Entity properEntity = PlayerUtils.getEntityCuttingOtherEntity(targetEntity, e -> !(e instanceof EntityArmorStand));
                if (properEntity != null) {
                    targetEntity = properEntity;
                }
            }
            float entityVelocity = (float) Math.sqrt(targetEntity.motionX * targetEntity.motionX + targetEntity.motionZ * targetEntity.motionZ);
            Vec3 targetPos = targetEntity.getPositionVector().addVector(0, this.yModifier, 0);
            if (entityVelocity > 0.1) {
                targetPos = targetPos.addVector(targetEntity.motionX * 1.3, targetEntity.motionY, targetEntity.motionZ * 1.3);
            }
            float distance = (float) mc.thePlayer.getPositionVector().distanceTo(targetPos);
            float distancePath = (float) mc.thePlayer.getPositionVector().distanceTo(lastElem);
            if (willArriveAtDestinationAfterStopping(lastElem) && entityVelocity < 0.15) {
                stop();
                return;
            }
            if ((distance < 1 && entityVelocity > 0.15) || (distancePath < 0.5 && entityVelocity < 0.15)) {
                stop();
                return;
            }
        } else if (willArriveAtDestinationAfterStopping(lastElem) || mc.thePlayer.getDistance(lastElem.xCoord, lastElem.yCoord, lastElem.zCoord) < 0.3) {
            stop();
            return;
        }
        if (!mc.thePlayer.capabilities.allowFlying) {
            Vec3 lastWithoutY = new Vec3(lastElem.xCoord, current.yCoord, lastElem.zCoord);
            if (current.distanceTo(lastWithoutY) < 1) {
                stop();
                LogUtils.sendSuccess("Arrived at destination");
                return;
            }
        }
        Vec3 next = getNext(copyPath);

        if (!RotationHandler.getInstance().isRotating() && mc.thePlayer.getDistance(next.xCoord, next.yCoord, next.zCoord) > 2) {
            Target target;
            if (this.targetEntity != null)
                target = new Target(this.targetEntity).additionalY(this.yModifier);
            else if (this.neededYaw != Integer.MIN_VALUE) {
                Vec3 directionHeading = AngleUtils.getVectorForRotation(4, this.neededYaw);
                Vec3 directionHeadingPlayer = mc.thePlayer.getPositionEyes(1).addVector(directionHeading.xCoord * 5, directionHeading.yCoord * 5, directionHeading.zCoord * 5);
                target = new Target(directionHeadingPlayer);
            } else {
                target = new Target(this.target).additionalY(this.yModifier);
            }

            if (!this.dontRotate && target.getTarget().isPresent() && !path.isEmpty()) {
                Vec3 lastElement = path.get(Math.max(0, path.size() - 1));
                Rotation rot = RotationHandler.getInstance().getRotation(target.getTarget().get());
                if (mc.thePlayer.getPositionVector().distanceTo(lastElement) > 2 && target.getTarget().isPresent() && RotationHandler.getInstance().shouldRotate(rot, 3)) {
                    float distanceTo = RotationHandler.getInstance().distanceTo(rot);
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            rot,
                            (long) (FarmHelperConfig.getRandomFlyPathExecutionerRotationTime() * (Math.max(1, distanceTo / 90))),
                            null
                    ));
                }
            }

            if (FarmHelperConfig.useAoteVInPestsDestroyer && tped && useAOTV && aotvDelay.passed() && mc.thePlayer.getDistance(next.xCoord, mc.thePlayer.getPositionVector().yCoord, next.zCoord) > 12 && !RotationHandler.getInstance().isRotating() && isFrontClean()) {
                int aotv = InventoryUtils.getSlotIdOfItemInHotbar("Aspect of the Void", "Aspect of the End");
                if (aotv != mc.thePlayer.inventory.currentItem) {
                    mc.thePlayer.inventory.currentItem = aotv;
                    aotvDelay.schedule(150);
                } else {
                    KeyBindUtils.rightClick();
                    tped = false;
                    lastTpTime = System.currentTimeMillis();
                }
            }
        }

        Rotation rotation = RotationHandler.getInstance().getRotation(current, next);
        List<KeyBinding> keyBindings = new ArrayList<>();
        List<KeyBinding> neededKeys = KeyBindUtils.getNeededKeyPresses(rotation.getYaw());

        neededYaw = rotation.getYaw();
        keyBindings.add(mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null);
        keyBindings.add(mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
        if (FarmHelperConfig.flyPathfinderOringoCompatible) {
            keyBindings.addAll(neededKeys);
        } else {
            keyBindings.add(mc.gameSettings.keyBindForward);
        }

        double distanceX = next.xCoord - mc.thePlayer.posX;
        double distanceY = next.yCoord - mc.thePlayer.posY;
        double distanceZ = next.zCoord - mc.thePlayer.posZ;
        float yaw = neededYaw * (float) Math.PI / 180.0f;
        double relativeDistanceX = distanceX * Math.cos(yaw) + distanceZ * Math.sin(yaw);
        double relativeDistanceZ = -distanceX * Math.sin(yaw) + distanceZ * Math.cos(yaw);
        VerticalDirection verticalDirection = shouldChangeHeight(relativeDistanceX, relativeDistanceZ);

        if (mc.thePlayer.capabilities.allowFlying) { // flying + walking
            if (fly(next, current)) return;
            if (verticalDirection.equals(VerticalDirection.HIGHER)) {
                keyBindings.add(mc.gameSettings.keyBindJump);
                loweringRaisingDelay.schedule(750);
            } else if (verticalDirection.equals(VerticalDirection.LOWER)) {
                keyBindings.add(mc.gameSettings.keyBindSneak);
                loweringRaisingDelay.schedule(750);
            } else if (loweringRaisingDelay.passed() && (getBlockUnder() instanceof BlockCactus || distanceY > 0.5) && (((EntityPlayerAccessor) mc.thePlayer).getFlyToggleTimer() == 0 || mc.gameSettings.keyBindJump.isKeyDown())) {
                keyBindings.add(mc.gameSettings.keyBindJump);
            } else if (loweringRaisingDelay.passed() && distanceY < -0.5) {
                Block blockUnder = getBlockUnder();
                if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.isFlying && !(blockUnder instanceof BlockCactus) && !(blockUnder instanceof BlockSoulSand)) {
                    keyBindings.add(mc.gameSettings.keyBindSneak);
                }
            }
        } else { // only walking
            if (shouldJump(next, current)) {
                mc.thePlayer.jump();
            }
        }

        if (sprinting) {
            keyBindings.add(mc.gameSettings.keyBindSprint);
        }

        if (neededYaw != Integer.MIN_VALUE)
            KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
        else
            KeyBindUtils.stopMovement(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTeleportPacket(ReceivePacketEvent event) {
        if (!isRunning()) return;
        if (event.packet instanceof S08PacketPlayerPosLook) {
            System.out.println("Tped");
            lastTpTime = System.currentTimeMillis() - 50;
            Multithreading.schedule(() -> {
                if (isRunning()) {
                    aotvDelay.schedule(100 + Math.random() * 60);
                    tped = true;
                }
            }, 50, TimeUnit.MILLISECONDS);
        }
    }

    public boolean hasJustTped() {
        return lastTpTime + LagDetector.getInstance().getLaggingTime() + 500 > System.currentTimeMillis();
    }

    private boolean isFrontClean() {
        Vec3 direction = mc.thePlayer.getLookVec();
        Vec3 tpPosition = mc.thePlayer.getPositionEyes(1).addVector(direction.xCoord * 10, direction.yCoord * 10, direction.zCoord * 10);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), tpPosition, false, true, false);
        return mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public boolean isTping() {
        return !tped;
    }

    private boolean willArriveAtDestinationAfterStopping(Vec3 targetPos) {
        return predictStoppingPosition().distanceTo(targetPos) < 0.75;
    }

    private Vec3 predictStoppingPosition() {
        PlayerSimulation playerSimulation = new PlayerSimulation(mc.theWorld);
        playerSimulation.copy(mc.thePlayer);
        playerSimulation.isFlying = true;
        playerSimulation.rotationYaw = neededYaw != Integer.MIN_VALUE && !FarmHelperConfig.flyPathfinderOringoCompatible ? neededYaw : mc.thePlayer.rotationYaw;
        for (int i = 0; i < 30; i++) {
            playerSimulation.onLivingUpdate();
            if (Math.abs(playerSimulation.motionX) < 0.01D && Math.abs(playerSimulation.motionZ) < 0.01D) {
                break;
            }
        }
        return new Vec3(playerSimulation.posX, playerSimulation.posY, playerSimulation.posZ);
    }

    private Block getBlockUnder() {
        Vec3 current = mc.thePlayer.getPositionVector();
        Vec3 direction = current.addVector(0, 0.5, 0);
        MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(current, direction, false, true, false);
        if (trace != null) {
            return mc.theWorld.getBlockState(trace.getBlockPos()).getBlock();
        }
        return null;
    }

    public VerticalDirection shouldChangeHeight(double relativeDistanceX, double relativeDistanceZ) {
        if (Math.abs(relativeDistanceX) < 0.75 && Math.abs(relativeDistanceZ) < 0.75) {
            System.out.println("No need to avoid blocks");
            return VerticalDirection.NONE;
        }

        BlocksInFront blocksInFront = getCollidingBlocks();

        if (isBlockInFront(blocksInFront.leftUp) || isBlockInFront(blocksInFront.centerUp) || isBlockInFront(blocksInFront.rightUp)) {
            return VerticalDirection.LOWER;
        }

        if (isBlockInFront(blocksInFront.leftDown) || isBlockInFront(blocksInFront.centerDown) || isBlockInFront(blocksInFront.rightDown) || isFenceDown(blocksInFront.fenceGateTrace)) {
            return VerticalDirection.HIGHER;
        }
        return VerticalDirection.NONE;
    }

    private boolean isBlockInFront(MovingObjectPosition trace) {
        return trace != null && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BlockUtils.hasCollision(trace.getBlockPos());
    }

    private boolean isFenceDown(MovingObjectPosition trace) {
        return trace != null && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BlockUtils.hasCollision(trace.getBlockPos()) && BlockUtils.getBlock(trace.getBlockPos()) instanceof BlockFenceGate;
    }

    public enum VerticalDirection {
        HIGHER,
        LOWER,
        NONE
    }

    private boolean checkForStuck(Vec3 positionVec3) {
        if (!stuckCheckDelay.passed()) return false;
        if (this.ticksAtLastPos > 15) {
            this.ticksAtLastPos = 0;
            this.lastPosCheck = positionVec3;
            return positionVec3.squareDistanceTo(this.lastPosCheck) < 2.25;
        }
        double diff = positionVec3.squareDistanceTo(this.lastPosCheck);
        if (diff < 2.25) {
            this.ticksAtLastPos++;
        } else {
            this.ticksAtLastPos = 0;
            this.lastPosCheck = positionVec3;
        }
        stuckCheckDelay.schedule(100);
        return false;
    }

    private boolean shouldJump(Vec3 next, Vec3 current) {
        int jumpBoost = mc.thePlayer.getActivePotionEffect(Potion.jump) != null ? mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1 : 0;
        return next.yCoord - current.yCoord > 0.25 + jumpBoost * 0.1 && mc.thePlayer.onGround && next.yCoord - current.yCoord < jumpBoost * 0.1 + 0.5;
    }

    private final Clock flyDelay = new Clock();

    private boolean fly(Vec3 next, Vec3 current) {
        if (mc.thePlayer.motionY < -0.0784000015258789 || BlockUtils.getRelativeBlock(0, 0, 0).getMaterial().isLiquid())
            if (flyDelay.passed()) {
                if (!mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = true;
                    mc.thePlayer.sendPlayerAbilities();
                }
                flyDelay.reset();
            } else if (flyDelay.isScheduled()) {
                return true;
            }
        if (mc.thePlayer.onGround && next.yCoord - current.yCoord > 0.5) {
            mc.thePlayer.jump();
            flyDelay.schedule(180 + (long) (Math.random() * 180));
            return true;
        } else {
            Vec3 closestToPlayer;
            try {
                closestToPlayer = path.stream().min(Comparator.comparingDouble((vec1) -> vec1.distanceTo(mc.thePlayer.getPositionVector()))).orElse(path.get(0));
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
            if (next.yCoord - closestToPlayer.yCoord > 0.5) {
                if (!flyDelay.isScheduled()) {
                    flyDelay.schedule(180 + (long) (Math.random() * 180));
                }
                return !mc.thePlayer.capabilities.isFlying;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onDraw(RenderWorldLastEvent event) {
        ArrayList<Vec3> copyPath = new ArrayList<>(path);
        if (copyPath.isEmpty()) return;
        if (!isRunning()) return;
        if (FarmHelperConfig.streamerMode) return;
        RenderManager renderManager = mc.getRenderManager();
        Vec3 current = mc.thePlayer.getPositionVector();
        Vec3 next = getNext(copyPath);
        AxisAlignedBB currenNode = new AxisAlignedBB(current.xCoord - 0.05, current.yCoord - 0.05, current.zCoord - 0.05, current.xCoord + 0.05, current.yCoord + 0.05, current.zCoord + 0.05);
        AxisAlignedBB nextBB = new AxisAlignedBB(next.xCoord - 0.05, next.yCoord - 0.05, next.zCoord - 0.05, next.xCoord + 0.05, next.yCoord + 0.05, next.zCoord + 0.05);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        currenNode = currenNode.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        nextBB = nextBB.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        RenderUtils.drawBox(currenNode, Color.GREEN);
        RenderUtils.drawBox(nextBB, Color.BLUE);
        for (int i = 0; i < copyPath.size() - 1; i++) {
            Vec3 from = new Vec3(copyPath.get(i).xCoord, copyPath.get(i).yCoord, copyPath.get(i).zCoord);
            Vec3 to = new Vec3(copyPath.get(i + 1).xCoord, copyPath.get(i + 1).yCoord, copyPath.get(i + 1).zCoord);
            from = from.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            RenderUtils.drawTracer(from, to, Color.RED);
        }
        if (!FarmHelperConfig.debugMode) return;
        BlocksInFront blocksInFront = getCollidingBlocks();
        drawCollidingBlock(blocksInFront.leftUp, renderManager, blocksInFront.leftUpTarget);
        drawCollidingBlock(blocksInFront.centerUp, renderManager, blocksInFront.centerUpTarget);
        drawCollidingBlock(blocksInFront.rightUp, renderManager, blocksInFront.rightUpTarget);
        drawCollidingBlock(blocksInFront.leftDown, renderManager, blocksInFront.leftDownTarget);
        drawCollidingBlock(blocksInFront.centerDown, renderManager, blocksInFront.centerDownTarget);
        drawCollidingBlock(blocksInFront.rightDown, renderManager, blocksInFront.rightDownTarget);
        drawCollidingBlock(blocksInFront.fenceGateTrace, renderManager, blocksInFront.fenceGateCheck);
    }

    private final Color blockedColor = new Color(255, 0, 0, 100);
    private final Color freeColor = new Color(0, 255, 0, 100);

    public void drawCollidingBlock(MovingObjectPosition mop, RenderManager renderManager, Vec3 target) {
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BlockUtils.hasCollision(mop.getBlockPos())) {
            BlockPos blockPos = mop.getBlockPos();
            RenderUtils.drawBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1).offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ), blockedColor);
        } else {
            RenderUtils.drawBox(new AxisAlignedBB(target.xCoord, target.yCoord, target.zCoord, target.xCoord + 0.1, target.yCoord + 0.1, target.zCoord + 0.1).offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ), freeColor);
        }
    }

    public BlocksInFront getCollidingBlocks() {
        Vec3 directionGoing = AngleUtils.getVectorForRotation(0, neededYaw);
        Vec3 directionGoingLeft = AngleUtils.getVectorForRotation(0, neededYaw - 20);
        Vec3 directionGoingRight = AngleUtils.getVectorForRotation(0, neededYaw + 20);
        Vec3 target = mc.thePlayer.getPositionVector().addVector(directionGoing.xCoord * 0.75, -0.1, directionGoing.zCoord * 0.75);
        Vec3 targetLeft = mc.thePlayer.getPositionVector().addVector(directionGoingLeft.xCoord * 0.75, -0.1, directionGoingLeft.zCoord * 0.75);
        Vec3 targetRight = mc.thePlayer.getPositionVector().addVector(directionGoingRight.xCoord * 0.75, -0.1, directionGoingRight.zCoord * 0.75);
        Vec3 targetUp = mc.thePlayer.getPositionVector().addVector(directionGoing.xCoord * 0.75, mc.thePlayer.height + 0.1, directionGoing.zCoord * 0.75);
        Vec3 targetLeftUp = mc.thePlayer.getPositionVector().addVector(directionGoingLeft.xCoord * 0.75, mc.thePlayer.height + 0.1, directionGoingLeft.zCoord * 0.75);
        Vec3 targetRightUp = mc.thePlayer.getPositionVector().addVector(directionGoingRight.xCoord * 0.75, mc.thePlayer.height + 0.1, directionGoingRight.zCoord * 0.75);
        Vec3 fenceGateCheck = mc.thePlayer.getPositionVector().addVector(directionGoing.xCoord * 0.25, -0.75, directionGoing.zCoord * 0.25);
        MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), target, false, true, false);
        MovingObjectPosition traceLeft = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), targetLeft, false, true, false);
        MovingObjectPosition traceRight = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), targetRight, false, true, false);
        MovingObjectPosition traceUp = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.height, 0), targetUp, false, true, false);
        MovingObjectPosition traceLeftUp = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.height, 0), targetLeftUp, false, true, false);
        MovingObjectPosition traceRightUp = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector().addVector(0, mc.thePlayer.height, 0), targetRightUp, false, true, false);
        MovingObjectPosition fenceGateTrace = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), fenceGateCheck, false, true, false);
        return new BlocksInFront(targetLeftUp, traceLeftUp, targetUp, traceUp, targetRightUp, traceRightUp, targetLeft, traceLeft, target, trace, targetRight, traceRight, fenceGateCheck, fenceGateTrace);
    }

    private Vec3 getNext(ArrayList<Vec3> path) {
        if (path.isEmpty()) {
            return mc.thePlayer.getPositionVector();
        }
        try {
            Vec3 current = mc.thePlayer.getPositionVector();
            Vec3 closestToPlayer = path.stream().min(Comparator.comparingDouble(vec -> vec.distanceTo(current))).orElse(path.get(path.size() - 2));
            return path.get(path.indexOf(closestToPlayer) + 1);
        } catch (IndexOutOfBoundsException e) {
            return path.get(path.size() - 1);
        }
    }

    public enum State {
        NONE,
        CALCULATING,
        FAILED,
        PATHING,
        DECELERATING,
        WAITING_FOR_DECELERATION
    }

    public static class Position {
        public BlockPos pos;
        public Rotation rotation;

        Position(BlockPos pos, Rotation rotation) {
            this.pos = pos;
            this.rotation = rotation;
        }
    }

    public static class BlocksInFront {
        public Vec3 leftUpTarget;
        public MovingObjectPosition leftUp;
        public Vec3 centerUpTarget;
        public MovingObjectPosition centerUp;
        public Vec3 rightUpTarget;
        public MovingObjectPosition rightUp;
        public Vec3 leftDownTarget;
        public MovingObjectPosition leftDown;
        public Vec3 centerDownTarget;
        public MovingObjectPosition centerDown;
        public Vec3 rightDownTarget;
        public MovingObjectPosition rightDown;
        public Vec3 fenceGateCheck;
        public MovingObjectPosition fenceGateTrace;

        public BlocksInFront(Vec3 leftUpTarget, MovingObjectPosition leftUp, Vec3 centerUpTarget, MovingObjectPosition centerUp, Vec3 rightUpTarget, MovingObjectPosition rightUp, Vec3 leftDownTarget, MovingObjectPosition leftDown, Vec3 centerDownTarget, MovingObjectPosition centerDown, Vec3 rightDownTarget, MovingObjectPosition rightDown, Vec3 fenceGateCheck, MovingObjectPosition fenceGateTrace) {
            this.leftUpTarget = leftUpTarget;
            this.leftUp = leftUp;
            this.centerUpTarget = centerUpTarget;
            this.centerUp = centerUp;
            this.rightUpTarget = rightUpTarget;
            this.rightUp = rightUp;
            this.leftDownTarget = leftDownTarget;
            this.leftDown = leftDown;
            this.centerDownTarget = centerDownTarget;
            this.centerDown = centerDown;
            this.rightDownTarget = rightDownTarget;
            this.rightDown = rightDown;
            this.fenceGateCheck = fenceGateCheck;
            this.fenceGateTrace = fenceGateTrace;
        }
    }
}
