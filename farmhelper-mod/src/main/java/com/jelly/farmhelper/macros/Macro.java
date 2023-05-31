package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
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
        return MiscConfig.rewarpPosX != 0 && MiscConfig.rewarpPosY != 0 && MiscConfig.rewarpPosZ != 0;
    }

    public boolean isStandingOnRewarpLocation() {
        BlockPos currentPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos rewarpPos = new BlockPos(MiscConfig.rewarpPosX, MiscConfig.rewarpPosY, MiscConfig.rewarpPosZ);
        System.out.println("currentPos: " + currentPos);
        System.out.println("rewarpPos: " + rewarpPos);
        System.out.println("Distance: " + Math.sqrt(currentPos.distanceSqToCenter(rewarpPos.getX(), rewarpPos.getY(), rewarpPos.getZ())));
        return Math.sqrt(currentPos.distanceSqToCenter(rewarpPos.getX(), rewarpPos.getY(), rewarpPos.getZ())) < 1;
    }

    public boolean isSpawnLocationSet() {
        return MiscConfig.spawnPosX != 0 && MiscConfig.spawnPosY != 0 && MiscConfig.spawnPosZ != 0;
    }

    public boolean isStandingOnSpawnLocation() {
        BlockPos currentPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos spawnPos = new BlockPos(MiscConfig.spawnPosX, MiscConfig.spawnPosY, MiscConfig.spawnPosZ);
        System.out.println("currentPos: " + currentPos);
        System.out.println("spawnPos: " + spawnPos);
        System.out.println("Distance: " + Math.sqrt(currentPos.distanceSqToCenter(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ())));
        return Math.sqrt(currentPos.distanceSqToCenter(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ())) < 1;
    }

    public void setSpawnLocation() {
        if (mc.thePlayer == null) return;
        ConfigHandler.set("spawnPosX", mc.thePlayer.getPosition().getX());
        ConfigHandler.set("spawnPosY", mc.thePlayer.getPosition().getY());
        ConfigHandler.set("spawnPosZ", mc.thePlayer.getPosition().getZ());
        MiscConfig.spawnPosX = mc.thePlayer.getPosition().getX();
        MiscConfig.spawnPosY = mc.thePlayer.getPosition().getY();
        MiscConfig.spawnPosZ = mc.thePlayer.getPosition().getZ();
    }

    public boolean cantPauseNow() {
        return false;
    }
}
