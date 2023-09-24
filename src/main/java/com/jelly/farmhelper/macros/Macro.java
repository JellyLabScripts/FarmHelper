package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.*;
import com.jelly.farmhelper.hud.DebugHUD;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro<T> {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;
    public boolean savedLastState = false;
    public T currentState = null;
    public final Rotation rotation = new Rotation();
    public int layerY = 0;
    public float yaw;
    public float pitch;
    private boolean rotated = false;
    protected boolean rotatedAfterStart = false;

    public BlockPos beforeTeleportationPos = null;
    public final Clock lastTp = new Clock();

    public boolean isTping = false;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 2) {
            LogUtils.sendDebug("Teleported!");
            if (yaw == -2137 && pitch == -2137) {
                onEnable();
            }
            currentState = changeState(calculateDirection());
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.reset();
            rotated = false;
            FarmHelper.gameState.scheduleNotMoving(750);
            Antistuck.stuck = false;
            Antistuck.notMovingTimer.schedule();
            lastTp.schedule(1_500);
            if (!PlayerUtils.isSpawnLocationSet() || (mc.thePlayer.getPositionVector().distanceTo(PlayerUtils.getSpawnLocation()) > 1.5)) {
                PlayerUtils.setSpawnLocation();
            }
            if (VisitorsMacro.canEnableMacro(false)) {
                VisitorsMacro.enableMacro(false);
            }
        }
    }

    public void onEnable() {
        lastTp.reset();
        isTping = false;
        rotated = true;
        rotatedAfterStart = false;
        beforeTeleportationPos = null;
        FarmHelper.gameState.scheduleNotMoving(750);
        Antistuck.stuck = false;
        Antistuck.notMovingTimer.schedule();
        Antistuck.unstuckTries = 0;
        Antistuck.unstuckThreadIsRunning = false;
        Desync.clickedBlocks.clear();
        layerY = mc.thePlayer.getPosition().getY();
        rotation.reset();
        if (mc.thePlayer.capabilities.isFlying) {
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.sendPlayerAbilities();
        }
        if (VisitorsMacro.canEnableMacro(false)) {
            VisitorsMacro.enableMacro(false);
        }
    }

    public void onDisable() {
        KeyBindUtils.stopMovement();
        if (Antistuck.unstuckThreadIsRunning) {
            Antistuck.unstuckThreadIsRunning = false;
            if (Antistuck.unstuckThreadInstance != null) {
                Antistuck.unstuckThreadInstance.interrupt();
            }
        }
        currentState = null;
    }

    public void onTick() {
        checkForTeleport();
        if (isStandingOnRewarpLocation() && !FailsafeNew.emergency) {
            triggerWarpGarden();
            return;
        }
        if (!isRewarpLocationSet()) {
            LogUtils.sendError("Your rewarp position is not set!");
            MacroHandler.disableMacro();
        }
        if (mc.thePlayer.getPosition().getY() < -5) {
            LogUtils.sendError("Build a wall between rewarp point and the void to prevent falling out of the garden! Disabling the macro...");
            MacroHandler.disableMacro();
            triggerWarpGarden(true);
            return;
        }
        if (lastTp.isScheduled() && lastTp.passed()) {
            lastTp.reset();
        }
    }

    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }


    public void onChatMessageReceived(String msg) {
    }

    public void onOverlayRender(RenderGameOverlayEvent event) {
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    private T stateBeforeFailsafe = null;

    public void saveLastStateBeforeDisable() {
        LogUtils.sendDebug("Saving last state before disabling the macro");
        stateBeforeFailsafe = currentState;
        savedLastState = true;
        if (MacroHandler.isMacroing && MacroHandler.currentMacro.enabled) {
            enabled = false;
            onDisable();
        }
    }

    public void restoreState() {
        LogUtils.sendDebug("Restoring last state before enabling macro");
        if (stateBeforeFailsafe != null) {
            changeState(stateBeforeFailsafe);
            stateBeforeFailsafe = null;
        }
        FarmHelper.gameState.scheduleNotMoving(750);
    }

    public void triggerTpCooldown() {
        lastTp.schedule(1500);
    }

    public boolean isRewarpLocationSet() {
        return !Config.rewarpList.isEmpty();
    }

    public boolean isStandingOnRewarpLocation() {
        if (Config.rewarpList.isEmpty()) return false;
        Rewarp closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Rewarp rewarp : Config.rewarpList) {
            double distance = rewarp.getDistance(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
            if (distance < closestDistance) {
                closest = rewarp;
                closestDistance = distance;
            }
        }
        if (closest == null) return false;
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 rewarpPos = new Vec3(closest.getX() + 0.5, closest.getY() + 0.5, closest.getZ() + 0.5);
        return playerPos.distanceTo(rewarpPos) <= FarmHelper.config.rewarpMaxDistance;
    }

    public void triggerWarpGarden() {
        triggerWarpGarden(false);
    }

    public void triggerWarpGarden(boolean force) {
        KeyBindUtils.stopMovement();
        isTping = true;
        if (force || FarmHelper.gameState.canChangeDirection() && beforeTeleportationPos == null) {
            LogUtils.sendDebug("Warping to spawn point");
            mc.thePlayer.sendChatMessage("/warp garden");
            beforeTeleportationPos = mc.thePlayer.getPosition();
        }
    }

    public T changeState(T newState) {
        LogUtils.sendDebug("Changing state from " + currentState + " to " + newState);
        DebugHUD.prevState = String.valueOf(currentState);
        currentState = newState;
        DebugHUD.currentState = String.valueOf(currentState);
        return newState;
    }

    public void checkForRotationAfterTp() {
        // Check for rotation after teleporting back to spawn point
        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && !rotated) {
            if (FarmHelper.config.rotateAfterWarped) {
                yaw = AngleUtils.get360RotationYaw(yaw + 180);
            } else {
                if (FarmHelper.config.dontRotateAfterWarping) {
                    LogUtils.sendDebug("Not rotating after warping");
                    return;
                }
            }
            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                rotation.easeTo(yaw, pitch, (long) (500 + Math.random() * 200));
            }
            rotated = true;
            LogUtils.sendDebug("Rotating");
        }
    }

    public boolean needAntistuck(boolean lastMoveBack) {
        if (LagDetection.isLagging() || LagDetection.wasJustLagging()) return false;
        if (Antistuck.stuck && !FailsafeNew.emergency) {
            unstuck(lastMoveBack);
            return true;
        }
        return false;
    }

    public void unstuck(boolean lastMoveBack) {
        if (!Antistuck.unstuckThreadIsRunning) {
            Antistuck.stuck = true;
            Antistuck.unstuckLastMoveBack = lastMoveBack;
            Antistuck.unstuckThreadIsRunning = true;
            if (Antistuck.unstuckTries >= 2 && FarmHelper.config.rewarpAt3FailesAntistuck) {
                LogUtils.sendWarning("Macro was continuously getting stuck! Warping to garden...");
                triggerWarpGarden(true);
                yaw = -2137;
                pitch = -2137;
                Antistuck.unstuckTries = 0;
                return;
            }
            LogUtils.sendWarning("Macro is stuck! Turning on antistuck procedure...");
            Antistuck.unstuckThreadInstance = new Thread(Antistuck.unstuckRunnable, "antistuck");
            KeyBindUtils.stopMovement();
            Antistuck.unstuckThreadInstance.start();
        } else {
            LogUtils.sendDebug("Unstuck thread is alive!");
        }
    }

    public T calculateDirection() {
        return null;
    }
}
