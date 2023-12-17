package com.jelly.farmhelperv2.util.helper;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.calc.FlyAStar;
import baritone.pathing.movement.CalculationContext;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
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
import java.util.Optional;

public class FlyPathfinder {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FlyPathfinder instance;

    public static FlyPathfinder getInstance() {
        if (instance == null) {
            instance = new FlyPathfinder();
        }
        return instance;
    }

    @Getter
    @Setter
    private Goal goal;
    private CalculationContext context;
    @Getter
    private final List<BlockPos> pathBlocks = new ArrayList<>();
    private final List<BlockPos> realPath = new ArrayList<>();
    private static final RotationHandler rotation = RotationHandler.getInstance();
    private Clock antiStuckDelay = new Clock();
    private int stuckCounterWithoutMotion = 0;
    private int stuckCounterWithMotion = 0;
    private boolean shouldRecalculateLater = false;
    public boolean hasFailed = false;
    private Vec3 lastPlayerPos;

    public void getPathTo(Goal goal) {
        if (context == null) {
            context = new CalculationContext(BaritoneAPI.getProvider().getPrimaryBaritone(), true);
        }
        this.goal = goal;
        if (goal == null) {
            LogUtils.sendError("Goal == null");
            return;
        }
        List<BlockPos> tempList = new ArrayList<>();
        BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        FlyAStar finder = new FlyAStar(playerPos.getX(), playerPos.getY(), playerPos.getZ(), goal, context);
        BaritoneAPI.getSettings().movementTimeoutTicks.value = 500;
        PathCalculationResult calcResult = finder.calculate(500L, 2000L);
        if (goal.heuristic(playerPos) < 2) {
            LogUtils.sendDebug("Goal is too close to the player. Disabling pathfinder.");
            return;
        }
        if (calcResult.getType().equals(PathCalculationResult.Type.SUCCESS_SEGMENT)) {
            shouldRecalculateLater = true;
            LogUtils.sendDebug("PathCalculationResult == SUCCESS_SEGMENT");
        } else if (!calcResult.getType().equals(PathCalculationResult.Type.SUCCESS_TO_GOAL)) {
            hasFailed = true;
            LogUtils.sendError("PathCalculationResult == " + calcResult.getType());
            return;
        }
        hasFailed = false;
        Optional<IPath> path = calcResult.getPath();
        if (path.isPresent() && path.get().positions().isEmpty()) {
            LogUtils.sendError("The path is empty!");
            return;
        }
        path.ifPresent(iPath -> tempList.addAll(iPath.positions()));
        realPath.clear();
        pathBlocks.clear();
        realPath.addAll(tempList);
        pathBlocks.addAll(smoothPath(tempList));
    }

    public boolean hasGoal() {
        return goal != null;
    }

    public void stop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        goal = null;
        pathBlocks.clear();
        rotation.reset();
        antiStuckDelay.reset();
        stuckCounterWithoutMotion = 0;
        stuckCounterWithMotion = 0;
        shouldRecalculateLater = false;
        hasFailed = false;
    }

    public void restart() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        KeyBindUtils.stopMovement();
        getPathTo(goal);
        stuckCounterWithoutMotion = 0;
        stuckCounterWithMotion = 0;
    }

    public boolean isPathing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
    }

    public boolean isRunning() {
        return !pathBlocks.isEmpty();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!FarmHelperConfig.debugMode) return;
        if (pathBlocks.isEmpty()) return;
        for (BlockPos pos : realPath) {
            RenderUtils.drawBlockBox(pos, new Color(255, 255, 0, 50));
        }
        for (BlockPos pos : pathBlocks) {
            int value = 255 - 50 * pathBlocks.indexOf(pos);
            if (value < 0) value = 0;
            RenderUtils.drawBlockBox(pos, new Color(value, 0, 0, 150));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (pathBlocks.isEmpty()) {
            if (isPathing()) {
                stop();
                LogUtils.sendDebug("Fly pathfinding stopped");
            }
            return;
        }
        if (!mc.thePlayer.capabilities.allowFlying) {
            LogUtils.sendError("Player is not allowed to fly. Disabling pathfinder.");
            stop();
            return;
        } else if (!mc.thePlayer.capabilities.isFlying && BlockUtils.getRelativeBlock(0, -1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, -1, 1)))
            mc.thePlayer.capabilities.isFlying = true;
        double distance3d = mc.thePlayer.getDistance(pathBlocks.get(0).getX() + 0.5, pathBlocks.get(0).getY() + 0.5, pathBlocks.get(0).getZ() + 0.5);
        if (distance3d < 0.5) {
            pathBlocks.remove(0);
            rotation.reset();
            if (!pathBlocks.isEmpty()) {
                Vec3 target = new Vec3(pathBlocks.get(0).getX() + 0.5, pathBlocks.get(0).getY() + 0.5, pathBlocks.get(0).getZ() + 0.5);
                float yaw = (FarmHelperConfig.flightLockRotationToMultipliersOf45Degrees ? AngleUtils.getClosest45(rotation.getRotation(target, true).getYaw()) : rotation.getRotation(target, true).getYaw());
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(yaw, rotation.getRotation(target, true).getPitch()),
                                750, null
                        )
                );
            } else if (shouldRecalculateLater) {
                shouldRecalculateLater = false;
                LogUtils.sendDebug("Recalculating the path...");
                getPathTo(goal);
            } else {
                LogUtils.sendDebug("Path is empty. Stopping pathfinder.");
                stop();
            }
            KeyBindUtils.stopMovement();
            return;
        }
        if (antiStuckDelay.isScheduled() && !antiStuckDelay.passed())
            return;
        if (mc.thePlayer.onGround
                && BlockUtils.getRelativeBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 0, 1))
                && BlockUtils.getRelativeBlock(0, 1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 1, 1))
                && !BlockUtils.getRelativeBlock(0, -1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, -1, 1))) {
            LogUtils.sendDebug("Jumping");
            mc.thePlayer.jump();
            antiStuckDelay.schedule(200);
            return;
        }
        if (isStuckWithMotion()) {
            LogUtils.sendDebug("Player is stuck with motion. Resetting pathfinder.");
            antiStuckDelay.schedule(500);
            restart();
            return;
        }
        if (isStuck()) {
            LogUtils.sendDebug("Player is stuck. Resetting pathfinder.");
            antiStuckDelay.schedule(500);
            restart();
            return;
        }
        double distanceX = pathBlocks.get(0).getX() - mc.thePlayer.posX + 0.5;
        double distanceY = pathBlocks.get(0).getY() - mc.thePlayer.posY + 0.5;
        double distanceZ = pathBlocks.get(0).getZ() - mc.thePlayer.posZ + 0.5;
        // swap X and Z based on looking direction
        float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f;
        double relativeDistanceX = distanceX * Math.cos(yaw) + distanceZ * Math.sin(yaw);
        double relativeDistanceZ = -distanceX * Math.sin(yaw) + distanceZ * Math.cos(yaw);
        double relativeMotionX = mc.thePlayer.motionX * Math.cos(yaw) + mc.thePlayer.motionZ * Math.sin(yaw);
        double relativeMotionZ = -mc.thePlayer.motionX * Math.sin(yaw) + mc.thePlayer.motionZ * Math.cos(yaw);
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, relativeDistanceX < -FarmHelperConfig.flightAllowedOvershootThreshold);
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, relativeDistanceX > FarmHelperConfig.flightAllowedOvershootThreshold);
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, relativeDistanceZ > FarmHelperConfig.flightAllowedOvershootThreshold);
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, relativeDistanceZ < -FarmHelperConfig.flightAllowedOvershootThreshold);
        if (shouldChangeHeight(relativeDistanceX, relativeDistanceZ) == VerticalDirection.NONE) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, distanceY > 0.25);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, distanceY < -0.25 && !mc.thePlayer.onGround && BlockUtils.getRelativeBlock(0, -1, 0).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, -1, 0)));
        } else if (shouldChangeHeight(relativeDistanceX, relativeDistanceZ) == VerticalDirection.HIGHER) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, true);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
        } else if (shouldChangeHeight(relativeDistanceX, relativeDistanceZ) == VerticalDirection.LOWER) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindJump, false);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
        }
        isDeceleratingLeft = relativeMotionX < 0 && decelerationReached(relativeMotionX, relativeDistanceX);
        if (isDeceleratingLeft) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, false);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, true);
        }
        isDeceleratingRight = relativeMotionX > 0 && decelerationReached(relativeMotionX, relativeDistanceX);
        if (isDeceleratingRight) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindLeft, false);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindRight, true);
        }
        isDeceleratingForward = relativeMotionZ < 0 && decelerationReached(relativeMotionZ, relativeDistanceZ);
        if (isDeceleratingForward) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, true);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, false);
        }
        isDeceleratingBackward = relativeMotionZ > 0 && decelerationReached(relativeMotionZ, relativeDistanceZ);
        if (isDeceleratingBackward) {
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindForward, false);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindBack, true);
        }
        boolean isSprinting = relativeDistanceZ > 12 && !mc.gameSettings.keyBindSneak.isKeyDown();
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, isSprinting);
        if (!isSprinting) // to avoid omni sprinting
            mc.thePlayer.setSprinting(false);
    }

    public boolean isDeceleratingLeft = false;
    public boolean isDeceleratingRight = false;
    public boolean isDeceleratingForward = false;
    public boolean isDeceleratingBackward = false;

    public VerticalDirection shouldChangeHeight(double relativeDistanceX, double relativeDistanceZ) {
        if (Math.abs(relativeDistanceX) < 0.75 && Math.abs(relativeDistanceZ) < 0.75) {
            return VerticalDirection.NONE;
        }
        if (mc.thePlayer.posY % 1 > 0.5
                && !BlockUtils.getRelativeFullBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 0, 1))
                && BlockUtils.getRelativeFullBlock(0, 1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 1, 1))
                && BlockUtils.getRelativeFullBlock(0, 2, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 2, 1))
        ) {
            return VerticalDirection.HIGHER;
        } else if (mc.thePlayer.posY % 1 < 0.5 && mc.thePlayer.posY % 1 > 0.201
                && !BlockUtils.getRelativeFullBlock(0, 2, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 2, 1))
                && BlockUtils.getRelativeFullBlock(0, 1, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 1, 1))
                && BlockUtils.getRelativeFullBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeFullBlockPos(0, 0, 1))
        ) {
            return VerticalDirection.LOWER;
        }
        return VerticalDirection.NONE;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        stop();
    }

    public enum VerticalDirection {
        HIGHER,
        LOWER,
        NONE
    }

    public double getPlayerSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionY * mc.thePlayer.motionY + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public float getDistanceTo(BlockPos target) {
        System.out.println("Goal: " + ((GoalNear) goal).getGoalPos().toString());
        System.out.println("Target: " + target.toString());
        System.out.println("Distance: " + (float) Math.sqrt(((GoalNear) goal).getGoalPos().distanceSq(target.getX(), target.getY(), target.getZ())));
        return (float) Math.sqrt(((GoalNear) goal).getGoalPos().distanceSq(target.getX(), target.getY(), target.getZ()));
    }

    public boolean isStuck() {
        if (getPlayerSpeed() < 0.1 && !mc.thePlayer.onGround)
            stuckCounterWithoutMotion++;
        else
            stuckCounterWithoutMotion = 0;
        if (stuckCounterWithoutMotion > FarmHelperConfig.flightMaxStuckTimeWithoutMotion) {
            stuckCounterWithoutMotion = 0;
            return true;
        }
        return false;
    }

    public boolean isStuckWithMotion() {
        if (lastPlayerPos == null || getPlayerSpeed() > 0.5) {
            lastPlayerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            return false;
        }
        Vec3 currentPlayerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (currentPlayerPos.distanceTo(lastPlayerPos) < FarmHelperConfig.flightMaximumStuckDistanceThreshold)
            stuckCounterWithMotion++;
        else {
            stuckCounterWithMotion = 0;
            lastPlayerPos = currentPlayerPos;
        }
        if (stuckCounterWithMotion > FarmHelperConfig.flightMaxStuckTimeWithMotion) {
            stuckCounterWithMotion = 0;
            return true;
        }
        return false;
    }

    //region Path smoothing - Nirox version
    IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    public List<BlockPos> smoothPath(List<BlockPos> path) {
        if (path.size() < 2) {
            return path;
        }
        long startTime = System.currentTimeMillis();
        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        int lowerIndex = 0;
        while (lowerIndex < path.size() - 2) {
            BlockPos start = path.get(lowerIndex);
            BlockPos lastValid = path.get(lowerIndex + 1);
            for (int upperIndex = lowerIndex + 2; upperIndex < path.size(); upperIndex++) {
                BlockPos end = path.get(upperIndex);
                if (traversable(start, end) && traversable(start.add(0, 1, 0), end.add(0, 1, 0))) {
                    lastValid = end;
                }
            }
            smoothed.add(lastValid);
            lowerIndex = path.indexOf(lastValid);
        }

        long endTime = System.currentTimeMillis();
        LogUtils.sendDebug("Direction change points found in " + (endTime - startTime) + "ms");
        return smoothed;
    }

    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            new Vec3(0.5, 0.5, 0.1),
            new Vec3(0.5, 0.5, 0.9),
            new Vec3(0.1, 0.5, 0.5),
            new Vec3(0.9, 0.5, 0.5)
    };

    public boolean traversable(BlockPos from, BlockPos to) {
        for (Vec3 offset : BLOCK_SIDE_MULTIPLIERS) {
            Vec3 fromVec = new Vec3(from.getX() + offset.xCoord, from.getY() + offset.yCoord, from.getZ() + offset.zCoord);
            Vec3 toVec = new Vec3(to.getX() + offset.xCoord, to.getY() + offset.yCoord, to.getZ() + offset.zCoord);
            MovingObjectPosition trace = baritone.getPlayerContext().world().rayTraceBlocks(fromVec, toVec, false, true, false);

            if (trace != null) {
                return false;
            }
        }

        return true;
    }
    //endregion

    //region Path smoothing - my version
    private static boolean rayTraceBlocks(BlockPos start, BlockPos end) {
        for (int i = 0; i < 8; i++) {
            Vec3 startVec = getBlockCorners(start)[i];
            Vec3 endVec = getBlockCorners(end)[i];

            MovingObjectPosition result = Minecraft.getMinecraft().theWorld.rayTraceBlocks(
                    new Vec3(startVec.xCoord, startVec.yCoord, startVec.zCoord),
                    new Vec3(endVec.xCoord, endVec.yCoord, endVec.zCoord));
            if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                return true;
            }
        }
        return false;
    }

    public static Vec3[] getBlockCorners(BlockPos blockPos) {
        Vec3[] corners = new Vec3[8];
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();

        for (int i = 0; i < 8; i++) {
            double xOffset = (i & 1) == 0 ? 0.1 : 0.9;
            double yOffset = (i & 2) == 0 ? 0.1 : 0.9;
            double zOffset = (i & 4) == 0 ? 0.1 : 0.9;
            corners[i] = new Vec3(x + xOffset, y + yOffset, z + zOffset);
        }

        return corners;
    }

    public static List<BlockPos> findDirectionChangePoints(List<BlockPos> path) {
        long startTime = System.currentTimeMillis();
        List<BlockPos> newDirectionChangePoints = new ArrayList<>();
        if (path.size() < 2) {
            return path;
        }

        newDirectionChangePoints.add(path.get(0));

        for (int i = 0; i < path.size(); i++) {
            BlockPos currentPos = path.get(i);

            for (int j = i + 1; j < path.size(); j++) {
                BlockPos otherPos = path.get(j);
                if (rayTraceBlocks(currentPos, otherPos)) {
                    newDirectionChangePoints.add(path.get(j - 1));
                    i = j - 2;
                    break;
                }
            }
        }

        newDirectionChangePoints.add(path.get(path.size() - 1));
        long endTime = System.currentTimeMillis();
        LogUtils.sendDebug("Direction change points found in " + (endTime - startTime) + "ms");
        return newDirectionChangePoints;
    }
    //endregion

    //region Deceleration
    public boolean decelerationReached(double motion, double distance) {
        double reachedMotion = 0.1;
        int offset = FarmHelperConfig.flightDecelerationOffset;
        for (int i = 0; i < decelerationDistances.length - 1 - offset; i++) {
            if (Math.abs(motion) > reachedMotion && decelerationDistances[i + offset] > Math.abs(distance)) {
                return true;
            }
            reachedMotion += 0.01;
        }
        return false;
    }

    private final double[] decelerationDistances = {
        0.039000000804662704,
        0.02443408966064453,
        0.05181508883833885,
        0.07919608801603317,
        0.10657709091901779,
        0.13395808637142181,
        0.16133908927440643,
        0.14573527872562408,
        0.18065199255943298,
        0.21556870639324188,
        0.2504854202270508,
        0.2854021191596985,
        0.3203188478946686,
        0.30623555183410645,
        0.3500385582447052,
        0.3918127715587616,
        0.433586984872818,
        0.475361168384552,
        0.5171353816986084,
        0.5589095950126648,
        0.551683783531189,
        0.6006367802619934,
        0.6486513018608093,
        0.69666588306427,
        0.7446804046630859,
        0.7926949262619019,
        0.8407094478607178,
        0.8887239694595337,
        0.8877385258674622,
        0.9431252479553223,
        0.996818482875824,
        1.0505117177963257,
        1.1042048931121826,
        1.157898187637329,
        1.211591362953186,
        1.2652846574783325,
        1.3189778327941895,
        1.3236711025238037,
        1.3859915733337402,
        1.444852352142334,
        1.5037132501602173,
        1.562574028968811,
        1.6214348077774048,
        1.680295705795288,
        1.7391564846038818,
        1.7980173826217651,
        1.8078782558441162,
        1.8723225593566895,
        1.9358859062194824,
        1.9994492530822754,
        2.0630125999450684,
        2.1265759468078613,
        2.1901392936706543,
        2.2537026405334473,
        2.3172659873962402,
        2.380829334259033,
        2.4443929195404053,
        2.458956241607666,
        2.5300827026367188,
        2.5979254245758057,
        2.6657681465148926,
        2.7336108684539795,
        2.8014533519744873,
        2.869296073913574,
        2.937138795852661,
        3.004981517791748,
        3.072824001312256,
        3.1406667232513428,
        3.1595094203948975,
        3.232480525970459,
        3.3042173385620117,
        3.3759541511535645,
        3.447690963745117,
        3.51942777633667,
        3.5911645889282227,
        3.6629014015197754,
        3.734638214111328,
        3.806375026702881,
        3.8781118392944336,
        3.9498486518859863,
        4.021585464477539,
        4.044322490692139,
        4.122203826904297,
        4.197484493255615,
        4.272765159606934,
        4.348045349121094,
        4.423326015472412,
        4.4986066818237305,
        4.573886871337891,
        4.649167537689209,
        4.724448204040527,
        4.7997283935546875,
        4.875009059906006,
        4.950289726257324,
        5.025569915771484,
        5.051850318908691,
        5.133279323577881,
        5.211784839630127,
        5.290289878845215,
        5.368795394897461,
        5.447300434112549,
        5.525805950164795,
        5.604310989379883,
        5.682816028594971,
        5.761321544647217,
        5.839826583862305,
        5.918332099914551,
        5.996837139129639,
        6.075342655181885,
        6.153847694396973,
        6.183352947235107,
        6.266881465911865,
        6.348320960998535,
        6.429760932922363,
        6.511200428009033,
        6.592640399932861,
        6.6740803718566895,
        6.755519866943359,
        6.8369598388671875,
        6.918399810791016,
        6.9998393058776855,
        7.081279277801514,
        7.162718772888184,
        7.244158744812012,
        7.32559871673584,
        7.40703821182251,
        7.439478397369385,
        7.520917892456055,
        7.607735633850098,
        7.691845893859863,
        7.775956153869629,
        7.8600664138793945,
        7.94417667388916,
        8.028286933898926,
        8.112397193908691,
        8.196507453918457,
        8.280617713928223,
        8.364727973937988,
        8.448838233947754,
        8.53294849395752,
        8.617058753967285,
        8.70116901397705,
        8.7852783203125,
    };
    //endregion
}
