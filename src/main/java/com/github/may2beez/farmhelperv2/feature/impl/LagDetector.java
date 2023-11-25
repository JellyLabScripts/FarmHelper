package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/*
    Credits to Yuro for this superb class
*/
public class LagDetector implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static LagDetector instance;
    public static LagDetector getInstance() {
        if (instance == null) {
            instance = new LagDetector();
        }
        return instance;
    }
    @Override
    public String getName() {
        return "Lag Detector";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false; // that one is always running, no need to start
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    private final Clock recentlyLagged = new Clock();
    private long lastReceivedPacketTime = 0;

    public boolean isLagging() {
        return System.currentTimeMillis() - lastReceivedPacketTime > 500;
    }

    public boolean wasJustLagging() {
        return recentlyLagged.isScheduled() && !recentlyLagged.passed();
    }

    public long getLaggingTime() {
        return System.currentTimeMillis() - lastReceivedPacketTime;
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        lastReceivedPacketTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (lastReceivedPacketTime == 0) return;
        if (isLagging()) {
            recentlyLagged.schedule(500);
        }
        if (recentlyLagged.isScheduled() && recentlyLagged.passed()) {
            recentlyLagged.reset();
        }
    }
}
