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
import com.jelly.farmhelperv2.util.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
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
            pathBlocks.addAll(getOnlyTurns(tempList));
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
        Set<BetterBlockPos> turns = new HashSet<>();
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
        if (distance > FarmHelperConfig.flightSpeedAccelerationDistance) {
            mc.thePlayer.capabilities.isFlying = true;
            KeyBindUtils.holdThese(
                    x < 0 ? mc.gameSettings.keyBindRight : null,
                    x > 0 ? mc.gameSettings.keyBindLeft : null,
                    z < 0 ? mc.gameSettings.keyBindBack : null,
                    z > 0 ? mc.gameSettings.keyBindForward : null,
                    distanceY > 0.25 ? mc.gameSettings.keyBindJump : null,
                    distanceY < -0.25 ? mc.gameSettings.keyBindSneak : null,
                    distance > 5 ? mc.gameSettings.keyBindSprint : null);
        } else if (distance > FarmHelperConfig.flightSpeedDecelerationDistance) {
            decelerate();
        } else {
            pathBlocks.remove(0);
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
