package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.client.Minecraft;

public class AutoSlugSell implements IFeature {

    private static AutoSlugSell instance;
    public static AutoSlugSell getInstance() {
        if (instance == null) {
            instance = new AutoSlugSell();
        }

        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private State state = State.STARTING;
    private final Clock delay = new Clock();

    @Override
    public String getName() {
        return "AutoSlugSell";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        state = State.STARTING;
        delay.reset();
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) {
            return;
        }

        enabled = false;
        state = State.STARTING;

        LogUtils.sendWarning("[AutoSlugSell] Stopping.");
    }

    @Override
    public void stop() {
        if (!enabled) {
            return;
        }

        enabled = false;
        state = State.SUSPEND;
        delay.reset();

        LogUtils.sendWarning("[AutoSlugSell] Stopping.");
    }

    enum State {
        STARTING,
        WALK_1,
        WALK_2,
        WALK_3,
        ROTATE,
        CLICK_GEORGE,
        CLICK_PET,
        SELL_PET,
        SELL_VERIFY,
        SUSPEND
    }
}
