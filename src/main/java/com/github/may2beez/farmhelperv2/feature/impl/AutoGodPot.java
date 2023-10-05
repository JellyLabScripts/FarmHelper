package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
import net.minecraft.client.Minecraft;

public class AutoGodPot implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoGodPot instance;
    public static AutoGodPot getInstance() {
        if (instance == null) {
            instance = new AutoGodPot();
        }
        return instance;
    }
    
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
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
        return false;
    }
}
