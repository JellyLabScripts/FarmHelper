package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro<T> {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;
    public boolean savedLastState = false;
    public T currentState = null;
    public final Rotation rotation = new Rotation();
    public boolean rotated = false;

    public final Thread unstuckThread = new Thread(() -> {
        try {
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
            Thread.sleep(500);
            KeyBindUtils.stopMovement();
            Thread.sleep(20);
            KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
            Thread.sleep(200);
            KeyBindUtils.stopMovement();
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(3500);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    });


    public void toggle(boolean pause) {
        if (pause) {
            enabled = false;
            onDisable();
        } else {
            toggle();
        }
    }

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
            rotated = false;
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
    }

    public void onEnable() {
        lastTp.reset();
        isTping = false;
        beforeTeleportationPos = null;
        FarmHelper.gameState.newRandomValueToWait();
        FarmHelper.gameState.scheduleNotMoving();
    }

    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    public void onTick() {
        checkForTeleport();
        if (isStandingOnRewarpLocation()) {
            triggerWarpGarden();
        }
        if (!isRewarpLocationSet()) {
            LogUtils.scriptLog("Your rewarp position is not set!");
            toggle();
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
        LogUtils.debugLog("Saving last state before disabling macro");
        stateBeforeFailsafe = currentState;
        savedLastState = true;
        if (MacroHandler.isMacroing && MacroHandler.currentMacro.enabled)
            toggle();
    }

    public void restoreState() {
        LogUtils.debugLog("Restoring last state before disabling macro");
        if (stateBeforeFailsafe != null) {
            currentState = stateBeforeFailsafe;
            stateBeforeFailsafe = null;
        }
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
        System.out.println("Here");
        if (FarmHelper.gameState.canChangeDirection() && beforeTeleportationPos == null) {
            LogUtils.debugLog("Warping to spawn point");
            mc.thePlayer.sendChatMessage(FarmHelper.gameState.wasInGarden ? "/warp garden" : "/is");
            isTping = true;
            beforeTeleportationPos = mc.thePlayer.getPosition();
        }
    }

    public T changeState(T newState) {
        LogUtils.debugLog("Changing state from " + currentState + " to " + newState);
        currentState = newState;
        return newState;
    }
}
