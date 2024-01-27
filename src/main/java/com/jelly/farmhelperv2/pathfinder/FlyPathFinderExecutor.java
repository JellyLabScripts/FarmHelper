package com.jelly.farmhelperv2.pathfinder;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.mixin.client.EntityPlayerAccessor;
import com.jelly.farmhelperv2.mixin.pathfinder.PathfinderAccessor;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.RenderUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
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
import java.util.List;
import java.util.concurrent.*;
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
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pathfinderTask;
    private ScheduledFuture<?> timeoutTask;
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
    private final int MAX_DISTANCE = 400;
    private int ticksAtLastPos = 0;
    private Vec3 lastPosCheck = new Vec3(0, 0, 0);
    private float yModifier = 0;
    private final Clock stuckBreak = new Clock();
    private final Clock stuckCheckDelay = new Clock();
    @Getter
    @Setter
    private boolean dontRotate = false;

    public void findPath(Vec3 pos, boolean follow, boolean smooth) {
        if (mc.thePlayer.getDistance(pos.xCoord, pos.yCoord, pos.zCoord) < 1) {
            stop();
            LogUtils.sendSuccess("Already at destination");
            return;
        }
        state = State.CALCULATING;
        this.follow = follow;
        this.target = pos;
        this.smooth = smooth;
        LogUtils.sendDebug("Cache size: " + WorldCache.getInstance().getWorldCache().size());
        try {
            System.out.println("Starting pathfinding");
            pathfinderTask = executor.schedule(() -> {
                long startTime = System.currentTimeMillis();
                int maxDistance = Math.min(MAX_DISTANCE, (int) mc.thePlayer.getPositionVector().distanceTo(pos) + 5);
                LogUtils.sendDebug("Max distance: " + maxDistance);
                LogUtils.sendDebug("Pathfinding to " + pos);
                PathEntity route = ((PathfinderAccessor) pathFinder).createPath(mc.theWorld, mc.thePlayer, pos.xCoord, pos.yCoord, pos.zCoord, maxDistance);
                if (!isRunning()) return;
                LogUtils.sendDebug("Pathfinding took " + (System.currentTimeMillis() - startTime) + "ms");
                if (route == null) {
                    state = State.FAILED;
                    LogUtils.sendError("Failed to find path to " + pos);
                    double distance = mc.thePlayer.getPositionVector().distanceTo(this.target);
                    if (distance > maxDistance) {
                        LogUtils.sendError("Distance to target is too far. Distance: " + distance + ", Max distance: " + maxDistance);
                        stop();
                    }
                    return;
                }
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
                    timeoutTask.cancel(true);
                }
            }, 0, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LogUtils.sendError("Pathfinding took too long");
            RotationHandler.getInstance().reset();
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

    public List<Vec3> smoothPath(List<Vec3> path) {
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

    public boolean traversable(Vec3 from, Vec3 to) {
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
            pathfinderTask.cancel(true);
            pathfinderTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
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
        if (target == null) return;
        if (!this.follow) return;
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
            Vec3 lastElement = path.get(path.size() - 1);
            if (mc.thePlayer.getPositionVector().distanceTo(lastElement) > 2 && !RotationHandler.getInstance().isRotating() && target.getTarget().isPresent()) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        target,
                        (long) (600 + Math.random() * 300),
                        null
                ).randomness(true));
            }
        }

        if (this.targetEntity != null) {
            findPath(this.targetEntity, this.follow, this.smooth, this.yModifier, this.dontRotate);
        } else {
            findPath(this.target, this.follow, this.smooth);
        }
    }

    private final Clock minimumDelayBetweenSpaces = new Clock();

    @SubscribeEvent
    public void onTickNeededYaw(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (state == State.NONE) return;
        if (state == State.FAILED) {
            KeyBindUtils.stopMovement(true);
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
            System.out.println(mc.thePlayer.getPositionVector().distanceTo(targetEntity.getPositionVector().addVector(0, this.yModifier, 0)));
            float velocity = (float) Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
            if (velocity < 0.1 && mc.thePlayer.getPositionVector().distanceTo(targetEntity.getPositionVector().addVector(0, this.yModifier, 0)) < 2.5) {
                stopAndDecelerate();
                return;
            } else if (velocity > 0.2 && mc.thePlayer.getPositionVector().distanceTo(targetEntity.getPositionVector().addVector(0, this.yModifier, 0)) < 1) {
                stop();
                return;
            }
        } else if ((current.distanceTo(path.get(path.size() - 1)) < 2 || ((target != null && mc.thePlayer.getDistance(target.xCoord, target.yCoord, target.zCoord) < 2.5)))) {
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
                System.out.println("Raising");
            } else if (verticalDirection.equals(VerticalDirection.LOWER)) {
                if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.isFlying) {
                    keyBindings.add(mc.gameSettings.keyBindSneak);
                    System.out.println("Lowering");
                }
            } else {
                if ((getBlockUnder() instanceof BlockCactus || distanceY > 0.15) && (((EntityPlayerAccessor) mc.thePlayer).getFlyToggleTimer() == 0 || mc.gameSettings.keyBindJump.isKeyDown())) {
                    keyBindings.add(mc.gameSettings.keyBindJump);
                    System.out.println("Raising 2");
                } else if (distanceY < -0.15) {
                    if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.isFlying && !(getBlockUnder() instanceof BlockCactus)) {
                        keyBindings.add(mc.gameSettings.keyBindSneak);
                        System.out.println("Lowering 2");
                    }
                }
            }
        } else { // only walking
            if (shouldJump(next, current)) {
                mc.thePlayer.jump();
            }
        }

        mc.thePlayer.setSprinting(FarmHelperConfig.sprintWhileFlying && neededKeys.contains(mc.gameSettings.keyBindForward) && current.distanceTo(new Vec3(next.xCoord, current.yCoord, next.zCoord)) > 6);

//        System.out.println("Buttons: " + keyBindings.stream().map(keyBinding -> keyBinding == null ? "null" : keyBinding.getKeyDescription()).collect(Collectors.joining(", ")));
        if (neededYaw != Integer.MIN_VALUE)
            KeyBindUtils.holdThese(keyBindings.toArray(new KeyBinding[0]));
        else
            KeyBindUtils.stopMovement(true);
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
        BlockPos relative = new BlockPos(0, 0, 1);
        if (mc.thePlayer.posY % 1 > 0.5
                && !BlockUtils.isFree(relative.getX(), relative.getY(), relative.getZ(), mc.theWorld)
                && !BlockUtils.isFree(relative.getX(), relative.getY() + 1, relative.getZ(), mc.theWorld)
                && !BlockUtils.isFree(relative.getX(), relative.getY() + 2, relative.getZ(), mc.theWorld)
        ) {
            return VerticalDirection.HIGHER;
        } else if (mc.thePlayer.posY % 1 < 0.5 && mc.thePlayer.posY % 1 > 0.201
                && !BlockUtils.isFree(relative.getX(), relative.getY() + 2, relative.getZ(), mc.theWorld)
                && !BlockUtils.isFree(relative.getX(), relative.getY() + 1, relative.getZ(), mc.theWorld)
                && !BlockUtils.isFree(relative.getX(), relative.getY(), relative.getZ(), mc.theWorld)
        ) {
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
            Vec3 closestToPlayer = path.stream().min((vec1, vec2) -> (int) (vec1.distanceTo(mc.thePlayer.getPositionVector()) - vec2.distanceTo(mc.thePlayer.getPositionVector()))).orElse(path.get(0));
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
        try {
            return path.get(1);
        } catch (IndexOutOfBoundsException e) {
            return path.get(Math.max(0, path.size() - 1));
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
}
