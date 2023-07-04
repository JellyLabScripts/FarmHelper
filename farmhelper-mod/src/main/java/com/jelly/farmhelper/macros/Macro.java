package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;
    public boolean savedLastState = false;


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

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onLastRender() {
    }

    public void onChatMessageReceived(String msg) {
    }

    public void onOverlayRender(RenderGameOverlayEvent event) {
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    public void saveLastStateBeforeDisable() {
        LogUtils.debugLog("Saving last state before disabling macro");
        savedLastState = true;
        if (MacroHandler.isMacroing && MacroHandler.currentMacro.enabled)
            toggle();
    }

    public void restoreState() {
    }

    public void triggerTpCooldown() {
    }

    public boolean isRewarpLocationSet() {
//        for (Rewarp rewarp : Config.rewarpList) {
//          System.out.println("FarmHelper: isRewarpLocationSet: rewarp: " + rewarp.toString());
//        }
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
        return Math.sqrt(currentPos.distanceSqToCenter(closest.getX(), closest.getY(), closest.getZ())) < 1;
    }

    public boolean isSpawnLocationSet() {
        return FarmHelper.config.spawnPosX != 0 && FarmHelper.config.spawnPosY != 0 && FarmHelper.config.spawnPosZ != 0;
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
        FarmHelper.config.save();
    }

    public boolean cantPauseNow() {
        return false;
    }
}
