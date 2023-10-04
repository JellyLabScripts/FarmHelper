package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.hud.ProfitCalculatorHUD;
import net.minecraft.client.Minecraft;

public class ProfitCalculator implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static ProfitCalculator instance;
    public static ProfitCalculator getInstance() {
        if (instance == null) {
            instance = new ProfitCalculator();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Profit Calculator";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        if (!ProfitCalculatorHUD.resetStatsBetweenDisabling) {
            MacroHandler.getInstance().getMacroingTimer().pause();
        }
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    public void resetProfits() {

    }
}
