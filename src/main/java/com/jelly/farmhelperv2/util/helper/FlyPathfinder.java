package com.jelly.farmhelperv2.util.helper;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.pathing.calc.AbstractNodeCostSearch;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Not yet finished :(
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
    private final List<BetterBlockPos> pathBlocks = new ArrayList<>();
    private static final RotationHandler rotation = RotationHandler.getInstance();

    public List<BetterBlockPos> getPathTo(Goal goal) {
        return getPathTo(goal, false);
    }

    public List<BetterBlockPos> getPathTo(Goal goal, boolean onlyTurns) {
        if (context == null) {
            context = new CalculationContext(BaritoneAPI.getProvider().getPrimaryBaritone(), true);
        }
        this.goal = goal;
        List<BetterBlockPos> tempList = new ArrayList<>();
        BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
        AbstractNodeCostSearch finder = new FlyAStar(playerPos.getX(), playerPos.getY(), playerPos.getZ(), goal, context);
        long primaryTimeout = Baritone.settings().primaryTimeoutMS.value;
        long failureTimeout = Baritone.settings().failureTimeoutMS.value;
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            Baritone.getExecutor().execute(() -> {
                PathCalculationResult calcResult = finder.calculate(primaryTimeout, failureTimeout);
                Optional<IPath> path = calcResult.getPath();
                path.ifPresent(iPath -> tempList.addAll(iPath.positions()));
                LogUtils.sendDebug("Path: " + tempList.size());
            });
            while (!finder.isFinished()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
        });
        future.join();
        pathBlocks.clear();
        if (onlyTurns) {
            pathBlocks.addAll(getOnlyTurns3(getOnlyTurns(tempList)));
        } else {
            pathBlocks.addAll(tempList);
        }
        return pathBlocks;
    }

    enum Direction {
        X,
        Y,
        Z
    }

    public List<BetterBlockPos> getOnlyTurns(List<BetterBlockPos> list) {
        if (list.isEmpty()) {
            LogUtils.sendDebug("Path is empty");
            return new ArrayList<>();
        }
        ArrayList<BetterBlockPos> turns = new ArrayList<>();
        BetterBlockPos lastPos = list.get(0);
        turns.add(lastPos);
        Direction lastDirection = Direction.X;
        for (BetterBlockPos pos : list) {
            if (pos.getX() != lastPos.getX()) {
                if (lastDirection != Direction.X) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.X;
            } else if (pos.getY() != lastPos.getY()) {
                if (lastDirection != Direction.Y) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.Y;
            } else if (pos.getZ() != lastPos.getZ()) {
                if (lastDirection != Direction.Z) {
                    turns.add(lastPos);
                }
                lastDirection = Direction.Z;
            }
            lastPos = pos;
        }
        turns.add(lastPos);
        return new ArrayList<>(turns);
    }

    public List<BetterBlockPos> getOnlyTurns2(List<BetterBlockPos> list) {
        if (list.size() < 3) return list;
        List<BetterBlockPos> tempList = new ArrayList<>();
        tempList.add(list.get(0));
        for (int i = 1; i < list.size() - 1; i++) {
            BetterBlockPos current = list.get(i);
            BetterBlockPos next = list.get(i + 1);
            BetterBlockPos previous = list.get(i - 1);
            if (current.getX() != next.getX() && current.getX() != previous.getX()) {
                tempList.add(current);
            } else if (current.getY() != next.getY() && current.getY() != previous.getY()) {
                tempList.add(current);
            } else if (current.getZ() != next.getZ() && current.getZ() != previous.getZ()) {
                tempList.add(current);
            }
        }
        tempList.add(list.get(list.size() - 1));
        return tempList;
    }

    public List<BetterBlockPos> getOnlyTurns3(List<BetterBlockPos> list) {
        if (list.size() < 3) return list;
        List<BetterBlockPos> tempList = new ArrayList<>();
        tempList.add(list.get(0));
        for (int i = 2; i < list.size() - 2; i++) {
            BetterBlockPos current = list.get(i);
            BetterBlockPos previous = list.get(i - 1);
            if (Math.sqrt(Math.pow(current.getX() - previous.getX(), 2) + Math.pow(current.getY() - previous.getY(), 2) + Math.pow(current.getZ() - previous.getZ(), 2)) > 1.5) {
//                MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(new Vec3(current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5), new Vec3(previous.getX() + 0.5, previous.getY() + 0.5, previous.getZ() + 0.5));
//                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    tempList.add(current);
//                }
            }
        }
        tempList.add(list.get(list.size() - 1));
        return tempList;
    }

    public boolean hasGoal() {
        return goal != null;
    }

    public void stop() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        goal = null;
        pathBlocks.clear();
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
        for (BetterBlockPos pos : pathBlocks) {
            int value = 255 - 50 * pathBlocks.indexOf(pos);
            if (value < 0) value = 0;
            RenderUtils.drawBlockBox(pos, new Color(value, 0, 0, 150));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (pathBlocks.isEmpty()) {
            if (isPathing()) {
                stop();
                LogUtils.sendDebug("Fly pathing stopped");
            }
            return;
        }
        // get distance to X, Y and Z of the next block
        double distanceX = pathBlocks.get(0).getX() - mc.thePlayer.posX + 0.5;
        double distanceY = pathBlocks.get(0).getY() - mc.thePlayer.posY + 0.5;
        double distanceZ = pathBlocks.get(0).getZ() - mc.thePlayer.posZ + 0.5;
        // swap X and Z based on looking direction
        float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f;
        double x = distanceX * Math.cos(yaw) + distanceZ * Math.sin(yaw);
        double z = -distanceX * Math.sin(yaw) + distanceZ * Math.cos(yaw);
        if (mc.thePlayer.onGround) {
            stop();
            KeyBindUtils.stopMovement();
            return;
        }
        double distance = Math.abs(Math.sqrt(x * x + z * z));
        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSprint, distance > 5);
        if (distance > FarmHelperConfig.flightSpeedAccelerationDistance) {
            mc.thePlayer.capabilities.isFlying = true;
            KeyBindUtils.holdThese(
                    x < 0 ? mc.gameSettings.keyBindRight : null,
                    x > 0 ? mc.gameSettings.keyBindLeft : null,
                    z < 0 ? mc.gameSettings.keyBindBack : null,
                    z > 0 ? mc.gameSettings.keyBindForward : null,
                    distanceY > 0.25 ? mc.gameSettings.keyBindJump : null,
                    distanceY < -0.25 ? mc.gameSettings.keyBindSneak : null);
        } else if (distance > FarmHelperConfig.flightSpeedDecelerationDistance) {
            decelerate();
        } else {
            pathBlocks.remove(0);
            if (!rotation.isRotating() && pathBlocks.size() > 2) {
                Vec3 target = new Vec3(pathBlocks.get(2).getX() + 0.5, pathBlocks.get(2).getY() + 0.5, pathBlocks.get(2).getZ() + 0.5);
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(rotation.getRotation(target, true).getYaw(), rotation.getRotation(target, true).getPitch()),
                                750, null
                        )
                );
            }
            KeyBindUtils.stopMovement();
        }
    }

    public void decelerate() {
        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;
        if (motionX > FarmHelperConfig.flightSpeedDecelerationThreshold) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
        } else if (motionX < -FarmHelperConfig.flightSpeedDecelerationThreshold) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
        }
        if (motionZ > FarmHelperConfig.flightSpeedDecelerationThreshold) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
        } else if (motionZ < -FarmHelperConfig.flightSpeedDecelerationThreshold) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
        }
    }

    public double calculateDecelerationDistance(double deceleration) { // 0.0064
        return (Math.sqrt(getPlayerSpeed())) / (2 * deceleration);
    }

    private double getPlayerSpeed() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionY * mc.thePlayer.motionY + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }
}
