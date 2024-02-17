package com.jelly.farmhelperv2.pathfinder;

import baritone.api.BaritoneAPI;
import baritone.pathing.movement.CalculationContext;
import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.common.collect.EvictingQueue;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.mixin.client.EntityPlayerAccessor;
import com.jelly.farmhelperv2.mixin.pathfinder.PathfinderAccessor;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.RenderUtils;
import com.jelly.farmhelperv2.util.helper.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
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
    private Vec3 lookingTarget;
    private boolean follow;
    private boolean smooth;
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
    private CalculationContext context;
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
        LogUtils.sendDebug("Cache size: " + WorldCache.getInstance().getWorldCache().size());
        try {
            System.out.println("Starting pathfinding");
            if (context == null) {
                context = new CalculationContext(BaritoneAPI.getProvider().getPrimaryBaritone(), false);
            }
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
                if (!isRunning()) return;
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
                this.path.addAll(finalRoute.stream().map(vec3 -> vec3.addVector(0.5f, 0.1, 0.5)).collect(Collectors.toCollection(CopyOnWriteArrayList::new)));
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
        // get 1 block closer to the player
        Vec3 targetNextPos = new Vec3(target.posX + target.motionX, target.posY + target.motionY, target.posZ + target.motionZ);
        findPath(targetNextPos.addVector(0, this.yModifier, 0), follow, smooth);
    }

    public boolean isRotationInCache(float yaw, float pitch) {
        return lastPositions.stream().anyMatch(position -> Math.abs(position.rotation.getYaw() - yaw) < 1 && Math.abs(position.rotation.getPitch() - pitch) < 1);
    }

    public boolean isPositionInCache(BlockPos pos) {
        return lastPositions.stream().anyMatch(position -> position.pos.equals(pos) || Math.sqrt(position.pos.distanceSq(pos)) < 1);
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
                if (traversable(start.addVector(0, 0.15, 0), end.addVector(0, 0.15, 0)) &&
                        traversable(start.addVector(0, 0.85, 0), end.addVector(0, 0.85, 0)) &&
                        traversable(start.addVector(0, 1.15, 0), end.addVector(0, 1.15, 0)) &&
                        traversable(start.addVector(0, 1.85, 0), end.addVector(0, 1.85, 0))) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = path.indexOf(lastValid);
        }

        return smoothed;
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.1, 0, 0.1),
            new Vec3(0.1, 0, 0.9),
            new Vec3(0.9, 0, 0.1),
            new Vec3(0.9, 0, 0.9)
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
        targetEntity = null;
        yModifier = 0;
        lookingTarget = null;
        state = State.NONE;
        KeyBindUtils.stopMovement(true);
        neededYaw = Integer.MIN_VALUE;
        minimumDelayBetweenSpaces.reset();
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
        tick = (tick + 1) % 8;

        if (tick != 0) return;
        if (isCalculating()) return;
        if (isDecelerating()) return;

        Target target;
        if (lookingTarget != null)
            target = new Target(this.lookingTarget);
        else if (this.targetEntity != null)
            target = new Target(this.targetEntity).additionalY(this.yModifier);
        else
            target = new Target(this.target);

        if (!this.dontRotate) {
            Vec3 lastElement = path.get(Math.max(0, path.size() - 1));
            if (mc.thePlayer.getPositionVector().distanceTo(lastElement) > 2 && !RotationHandler.getInstance().isRotating() && target.getTarget().isPresent()) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        target,
                        (long) (600 + Math.random() * 300),
                        null
                ).randomness(true));
            }
        }

        if (!this.follow) return;
        if (this.targetEntity != null) {
            findPath(this.targetEntity, true, this.smooth, this.yModifier, this.dontRotate);
        } else {
            findPath(this.target, true, this.smooth);
        }
    }

    private final Clock minimumDelayBetweenSpaces = new Clock();

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
        if (path.isEmpty()) {
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
                if (traversable(current.addVector(0, 0.15, 0), escape.addVector(0, 0.15, 0)) &&
                        traversable(current.addVector(0, 0.85, 0), escape.addVector(0, 0.85, 0)) &&
                        traversable(current.addVector(0, 1.15, 0), escape.addVector(0, 1.15, 0)) &&
                        traversable(current.addVector(0, 1.85, 0), escape.addVector(0, 1.85, 0))) {
                    break;
                }
            }
            neededYaw = rotationToEscape;
            if (FarmHelperConfig.flyPathfinderOringoCompatible) {
                List<KeyBinding> keyBindings = new ArrayList<>(KeyBindUtils.getNeededKeyPresses(neededYaw));
                keyBindings.add(mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null);
                keyBindings.add(mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
                KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null, mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
            }
            Multithreading.schedule(() -> KeyBindUtils.stopMovement(true), 500, TimeUnit.MILLISECONDS);
            return;
        }
        if (targetEntity != null) {
            float velocity = (float) Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
            float entityVelocity = (float) Math.sqrt(targetEntity.motionX * targetEntity.motionX + targetEntity.motionZ * targetEntity.motionZ);
            Vec3 targetPos = targetEntity.getPositionVector().addVector(0, this.yModifier, 0);
            float distance = (float) mc.thePlayer.getPositionVector().distanceTo(targetPos);
            System.out.println("Velo: " + velocity);
            System.out.println("TargetPos: " + targetPos);
            System.out.println("Distance: " + distance);
            System.out.println("EntityVelo: " + entityVelocity);
            if (entityVelocity > 0.2) {
                targetPos = targetPos.addVector(targetEntity.motionX * 1.5, targetEntity.motionY, targetEntity.motionZ * 1.5);
            }
            if (willArriveAtDestinationAfterStopping(velocity, targetPos)) {
                stop();
                return;
            }
        } else if ((current.distanceTo(path.get(path.size() - 1)) < 1.5 || ((target != null && mc.thePlayer.getDistance(target.xCoord, target.yCoord, target.zCoord) < 2.5)))) {
            stopAndDecelerate();
            return;
        }
        if (!mc.thePlayer.capabilities.allowFlying) {
            Vec3 lastWithoutY = new Vec3(path.get(path.size() - 1).xCoord, current.yCoord, path.get(path.size() - 1).zCoord);
            if (current.distanceTo(lastWithoutY) < 1) {
                stop();
                LogUtils.sendSuccess("Arrived at destination");
                return;
            }
        }
        Vec3 next = getNext();

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
//        System.out.println("next y: " + next.yCoord + ", current y: " + mc.thePlayer.posY + ", distance y: " + distanceY);
        double distanceZ = next.zCoord - mc.thePlayer.posZ;
        float yaw = neededYaw * (float) Math.PI / 180.0f;
        double relativeDistanceX = distanceX * Math.cos(yaw) + distanceZ * Math.sin(yaw);
        double relativeDistanceZ = -distanceX * Math.sin(yaw) + distanceZ * Math.cos(yaw);
        VerticalDirection verticalDirection = shouldChangeHeight(relativeDistanceX, relativeDistanceZ);

        if (mc.thePlayer.capabilities.allowFlying) { // flying + walking
            if (fly(next, current)) return;
            if (verticalDirection.equals(VerticalDirection.HIGHER)) {
                keyBindings.add(mc.gameSettings.keyBindJump);
                System.out.println("Raising 1");
            } else if (verticalDirection.equals(VerticalDirection.LOWER)) {
                keyBindings.add(mc.gameSettings.keyBindSneak);
                System.out.println("Lowering 1");
            } else if ((getBlockUnder() instanceof BlockCactus || distanceY > 0.25) && (((EntityPlayerAccessor) mc.thePlayer).getFlyToggleTimer() == 0 || mc.gameSettings.keyBindJump.isKeyDown())) {
                keyBindings.add(mc.gameSettings.keyBindJump);
                System.out.println("Raising 2");
            } else if (distanceY < -0.25) {
                Block blockUnder = getBlockUnder();
                if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.isFlying && !(blockUnder instanceof BlockCactus) && !(blockUnder instanceof BlockSoulSand)) {
                    keyBindings.add(mc.gameSettings.keyBindSneak);
                    System.out.println("Lowering 2");
                }
            }
        } else { // only walking
            if (shouldJump(next, current)) {
                mc.thePlayer.jump();
            }
        }

        mc.thePlayer.setSprinting(FarmHelperConfig.sprintWhileFlying && neededKeys.contains(mc.gameSettings.keyBindForward) && !neededKeys.contains(mc.gameSettings.keyBindSneak) && current.distanceTo(new Vec3(next.xCoord, current.yCoord, next.zCoord)) > 6);

//        System.out.println("Buttons: " + keyBindings.stream().map(keyBinding -> keyBinding == null ? "null" : keyBinding.getKeyDescription()).collect(Collectors.joining(", ")));
        if (neededYaw != Integer.MIN_VALUE)
            KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
        else
            KeyBindUtils.stopMovement(true);
    }

    private boolean willArriveAtDestinationAfterStopping(float velocity, Vec3 targetPos) {
        Vec3 stoppingPosition = predictStoppingPosition();
        double stoppingDistance = stoppingPosition.distanceTo(targetPos);
        return stoppingDistance < 0.5 && velocity > 0.1;
    }

    private Vec3 predictStoppingPosition() {
        PlayerSimulation playerSimulation = new PlayerSimulation(mc.theWorld);
        playerSimulation.copy(mc.thePlayer);
        playerSimulation.isFlying = true;
        playerSimulation.rotationYaw = mc.thePlayer.rotationYaw;
        for (int i = 0; i < 30; i++) {
            playerSimulation.moveForward = -mc.thePlayer.moveForward;
            playerSimulation.onLivingUpdate();
            if (playerSimulation.motionX < 0.005D || playerSimulation.motionX > -0.005D && playerSimulation.motionZ < 0.005D || playerSimulation.motionZ > -0.005D) {
                break;
            }
        }
        return new Vec3(playerSimulation.posX, playerSimulation.posY, playerSimulation.posZ);
    }

    private void stopAndDecelerate() {
        float velocity = (float) Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        LogUtils.sendDebug("Stopping and decelerating. Velocity: " + velocity);
        if (Math.abs(velocity) < 0.1) {
            LogUtils.sendSuccess("Arrived at destination");
            stop();
        } else {
            state = State.DECELERATING;
            if (this.target == null) {
                LogUtils.sendDebug("????????");
                stop();
                return;
            }

            float rotationBasedOnMotion = (float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90;
            LogUtils.sendDebug("Decelerating. Rotation based on motion: " + rotationBasedOnMotion);
            neededYaw = rotationBasedOnMotion;
            mc.thePlayer.setSprinting(false);
            if (FarmHelperConfig.flyPathfinderOringoCompatible) {
                List<KeyBinding> keyBindings = new ArrayList<>(KeyBindUtils.getOppositeKeys(KeyBindUtils.getNeededKeyPresses(neededYaw)));
                keyBindings.add(mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null);
                keyBindings.add(mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
                KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
            } else {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, mc.gameSettings.keyBindUseItem.isKeyDown() ? mc.gameSettings.keyBindUseItem : null, mc.gameSettings.keyBindAttack.isKeyDown() ? mc.gameSettings.keyBindAttack : null);
            }
        }
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
            return VerticalDirection.NONE;
        }
        Vec3 directionGoing = AngleUtils.getVectorForRotation(0, neededYaw);
        Vec3 target = mc.thePlayer.getPositionVector().addVector(directionGoing.xCoord * 0.6, -0.05, directionGoing.zCoord * 0.6);
        MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), target, false, true, false);
        if (trace != null && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return VerticalDirection.HIGHER;
        }
        Vec3 targetUp = mc.thePlayer.getPositionVector().addVector(directionGoing.xCoord * 0.6, mc.thePlayer.height + 0.05, directionGoing.zCoord * 0.6);
        MovingObjectPosition traceUp = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionVector(), targetUp, false, true, false);
        if (traceUp != null && traceUp.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return VerticalDirection.LOWER;
        }
        return VerticalDirection.NONE;
    }

    public enum VerticalDirection {
        HIGHER,
        LOWER,
        NONE
    }

    private boolean checkForStuck(Vec3 positionVec3) {
        if (!stuckCheckDelay.passed()) return false;
        if (this.ticksAtLastPos > 25) {
            this.ticksAtLastPos = 0;
            this.lastPosCheck = positionVec3;
            return positionVec3.squareDistanceTo(this.lastPosCheck) < 2.25;
        }
        double diff = positionVec3.squareDistanceTo(this.lastPosCheck);
        if (diff < 2.25) {
            this.ticksAtLastPos++;
            System.out.println(this.ticksAtLastPos);
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
            flyDelay.schedule(80 + (long) (Math.random() * 80));
            return true;
        } else {
            Vec3 closestToPlayer;
            try {
                closestToPlayer = path.stream().min((vec1, vec2) -> (int) (vec1.distanceTo(mc.thePlayer.getPositionVector()) - vec2.distanceTo(mc.thePlayer.getPositionVector()))).orElse(path.get(0));
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
            if (next.yCoord - closestToPlayer.yCoord > 0.5) {
                if (!flyDelay.isScheduled()) {
                    flyDelay.schedule(80 + (long) (Math.random() * 80));
                }
                return !mc.thePlayer.capabilities.isFlying;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onDraw(RenderWorldLastEvent event) {
        if (path.isEmpty()) return;
        if (!isRunning()) return;
        RenderManager renderManager = mc.getRenderManager();
        Vec3 current = mc.thePlayer.getPositionVector();
        Vec3 next = getNext();
        AxisAlignedBB currenNode = new AxisAlignedBB(current.xCoord - 0.05, current.yCoord - 0.05, current.zCoord - 0.05, current.xCoord + 0.05, current.yCoord + 0.05, current.zCoord + 0.05);
        AxisAlignedBB nextBB = new AxisAlignedBB(next.xCoord - 0.05, next.yCoord - 0.05, next.zCoord - 0.05, next.xCoord + 0.05, next.yCoord + 0.05, next.zCoord + 0.05);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        currenNode = currenNode.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        nextBB = nextBB.offset(-rendermanager.viewerPosX, -rendermanager.viewerPosY, -rendermanager.viewerPosZ);
        RenderUtils.drawBox(currenNode, Color.GREEN);
        RenderUtils.drawBox(nextBB, Color.BLUE);
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 from = new Vec3(path.get(i).xCoord, path.get(i).yCoord, path.get(i).zCoord);
            Vec3 to = new Vec3(path.get(i + 1).xCoord, path.get(i + 1).yCoord, path.get(i + 1).zCoord);
            from = from.addVector(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ);
            RenderUtils.drawTracer(from, to, Color.RED);
        }
    }

    private Vec3 getNext() {
        if (path.isEmpty()) {
            return mc.thePlayer.getPositionVector();
        }
        try {
            Vec3 current = mc.thePlayer.getPositionVector();
            Vec3 closestToPlayer = path.stream().min(Comparator.comparingDouble(vec -> vec.distanceTo(current))).orElse(path.get(0));
            return path.get(path.indexOf(closestToPlayer) + 1);
        } catch (IndexOutOfBoundsException e) {
            return mc.thePlayer.getPositionVector();
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
}
