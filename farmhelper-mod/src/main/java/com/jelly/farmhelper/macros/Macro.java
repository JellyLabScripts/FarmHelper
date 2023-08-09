package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
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


    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
            if (savedLastState) {
                savedLastState = false;
                restoreState();
            }
        } else {
            onDisable();
        }
    }

    public BlockPos beforeTeleportationPos = null;
    public final Clock lastTp = new Clock();

    public boolean isTping = false;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 2) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.reset();
            lastTp.schedule(1_000);
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
    }

    public void onEnable() {
        lastTp.reset();
        isTping = false;
        beforeTeleportationPos = null;
        FarmHelper.gameState.scheduleNotMoving(750);
        Antistuck.stuck = false;
        Antistuck.cooldown.schedule(3500);
        Antistuck.unstuckThreadIsRunning = false;
        layerY = mc.thePlayer.getPosition().getY();
        rotation.reset();
        if (mc.thePlayer.capabilities.isFlying) {
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.sendPlayerAbilities();
        }
    }

    public void onDisable() {
        KeyBindUtils.stopMovement();
        UngrabUtils.regrabMouse();
    }

    public void onTick() {
        checkForTeleport();
        if (isStandingOnRewarpLocation()) {
            triggerWarpGarden();
            return;
        }
        if (!isRewarpLocationSet()) {
            LogUtils.scriptLog("Your rewarp position is not set!");
            MacroHandler.disableCurrentMacro();
        }
    }

    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }


    public void onChatMessageReceived(String msg) {
    }

    double prevX = 0;
    double prevZ = 0;
    double prevY = 0;
    double prevTime = 0;
    double blocksPerSecond = 0;

    public void onOverlayRender(RenderGameOverlayEvent event) {
        /*// Count moved blocks per second and display on screen
        double deltaX = mc.thePlayer.posX - prevX;
        double deltaY = mc.thePlayer.posY - prevY;
        double deltaZ = mc.thePlayer.posZ - prevZ;

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        long currentTime = System.currentTimeMillis();
        double elapsedTime = (currentTime - prevTime) / 1000.0; // Convert milliseconds to seconds

        double speed = distance / elapsedTime;

        if (elapsedTime > 0.1) {
            prevX = mc.thePlayer.posX;
            prevY = mc.thePlayer.posY;
            prevZ = mc.thePlayer.posZ;
            prevTime = currentTime;
            blocksPerSecond = speed;
        }

        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            String speedString = String.format("%.2f", blocksPerSecond);
            String text = "Speed: " + speedString + " blocks/s";
            int x = 2;
            int y = 2;
            int color = 0xffffff;
            mc.fontRendererObj.drawString(text, x, y, color);
        }*/
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    private T stateBeforeFailsafe = null;

    public void saveLastStateBeforeDisable() {
        LogUtils.debugLog("Saving last state before disabling macro");
        stateBeforeFailsafe = currentState;
        savedLastState = true;
        if (MacroHandler.isMacroing && MacroHandler.currentMacro.enabled)
            toggle();
    }

    public void restoreState() {
        LogUtils.debugLog("Restoring last state before disabling macro");
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
        return Config.rewarpList.size() > 0;
    }

    public boolean isStandingOnRewarpLocation() {
        if (Config.rewarpList.size() == 0) return false;
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
        BlockPos currentPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        return Math.sqrt(currentPos.distanceSqToCenter(closest.getX(), closest.getY(), closest.getZ())) < 1.5;
    }

    public boolean isSpawnLocationSet() {
        return FarmHelper.config.isSpawnpointSet;
    }

    public boolean isStandingOnSpawnLocation() {
        BlockPos currentPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos spawnPos = new BlockPos(FarmHelper.config.spawnPosX, FarmHelper.config.spawnPosY, FarmHelper.config.spawnPosZ);
        return Math.sqrt(currentPos.distanceSqToCenter(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ())) < 1;
    }

    public void setSpawnLocation() {
        if (mc.thePlayer == null) return;
        FarmHelper.config.spawnPosX = mc.thePlayer.getPosition().getX();
        FarmHelper.config.spawnPosY = mc.thePlayer.getPosition().getY();
        FarmHelper.config.spawnPosZ = mc.thePlayer.getPosition().getZ();
        FarmHelper.config.isSpawnpointSet = true;
        FarmHelper.config.save();
    }

    public boolean cantPauseNow() {
        return false;
    }


    public void triggerWarpGarden() {
        KeyBindUtils.stopMovement();
        isTping = true;
        if (FarmHelper.gameState.canChangeDirection() && beforeTeleportationPos == null) {
            LogUtils.debugLog("Warping to spawn point");
            mc.thePlayer.sendChatMessage(FarmHelper.gameState.wasInGarden ? "/warp garden" : "/is");
            beforeTeleportationPos = mc.thePlayer.getPosition();
        }
    }

    public T changeState(T newState) {
        LogUtils.debugLog("Changing state from " + currentState + " to " + newState);
        currentState = newState;
        return newState;
    }

    public void checkForRotationAfterTp() {
        // Check for rotation after teleporting back to spawn point
        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating) {
            yaw = AngleUtils.getClosest(yaw);
            if (FarmHelper.config.rotateAfterWarped)
                yaw = AngleUtils.get360RotationYaw(yaw + 180);
            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                rotation.easeTo(yaw, pitch, (long) (500 + Math.random() * 200));
            }
        }
    }

    public void checkForRotationFailsafe() {
        // Check for rotation check failsafe
        boolean flag = AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FarmHelper.config.rotationCheckSensitivity
                || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity;

        if(!Failsafe.emergency && flag && lastTp.passed() && !rotation.rotating) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }
    }

    public boolean isStuck() {
        if (Antistuck.stuck) {
            if (!Antistuck.unstuckThreadIsRunning) {
                Antistuck.unstuckThreadIsRunning = true;
                LogUtils.debugLog("Stuck!");
                new Thread(Antistuck.unstuckThread).start();
            } else {
                LogUtils.debugLog("Unstuck thread is alive!");
            }
            return true;
        }
        return false;
    }

    public void unstuck() {
        if (!Antistuck.unstuckThreadIsRunning) {
            Antistuck.stuck = true;
            Antistuck.unstuckThreadIsRunning = true;
            LogUtils.debugLog("Stuck!");
            new Thread(Antistuck.unstuckThread).start();
        } else {
            LogUtils.debugLog("Unstuck thread is alive!");
        }
    }
}
