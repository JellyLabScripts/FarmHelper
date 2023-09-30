package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;

public class AntiStuck implements IFeature {

    private static AntiStuck instance;
    public static AntiStuck getInstance() {
        if (instance == null) {
            instance = new AntiStuck();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "AntiStuck";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isActivated() {
        return false;
    }
}
