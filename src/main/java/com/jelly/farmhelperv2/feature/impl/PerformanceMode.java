package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import net.minecraft.client.Minecraft;

public class PerformanceMode implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static PerformanceMode instance;

    public static PerformanceMode getInstance() {
        if (instance == null) {
            instance = new PerformanceMode();
        }
        return instance;
    }

    private int renderDistanceBefore = 0;
    private int maxFpsBefore = 0;

    private boolean enabled = false;

    @Override
    public String getName() {
        return "Performance Mode";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        enabled = true;
        renderDistanceBefore = mc.gameSettings.renderDistanceChunks;
        maxFpsBefore = mc.gameSettings.limitFramerate;
        mc.gameSettings.renderDistanceChunks = 1;
        mc.gameSettings.limitFramerate = FarmHelperConfig.performanceModeMaxFPS;
        mc.addScheduledTask(() -> mc.renderGlobal.loadRenderers());
        IFeature.super.start();
    }

    @Override
    public void stop() {
        enabled = false;
        mc.gameSettings.renderDistanceChunks = renderDistanceBefore;
        mc.gameSettings.limitFramerate = maxFpsBefore;
        renderDistanceBefore = 0;
        maxFpsBefore = 0;
        mc.addScheduledTask(() -> mc.renderGlobal.loadRenderers());
        IFeature.super.stop();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.performanceMode;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }
}
