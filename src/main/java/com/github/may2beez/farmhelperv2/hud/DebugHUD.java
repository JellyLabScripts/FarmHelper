package com.github.may2beez.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.Scheduler;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.BlockUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;

import java.util.List;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (!FarmHelperConfig.debugMode) return;
        lines.add("§lFarmHelper Debug HUD");
        lines.add("Location: " + GameStateHandler.getInstance().getLocation());
        MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> {
            lines.add("Current state: " + macro.getCurrentState());
            lines.add("Rotating: " + macro.getRotation().rotating);
        });
        lines.add("Scheduler: " + FarmHelperConfig.enableScheduler);
        if (FarmHelperConfig.enableScheduler) {
            lines.add("  State: " + LogUtils.capitalize(Scheduler.getInstance().getSchedulerState().toString()));
            lines.add("  Clock: " + Scheduler.getInstance().getSchedulerClock().getRemainingTime());
            lines.add("  isFarming: " + Scheduler.getInstance().isFarming());
        }
        if (MacroHandler.getInstance().getCrop() != null) {
            lines.add("Crop: " + LogUtils.capitalize(MacroHandler.getInstance().getCrop().toString()));
            lines.add("   LeftCropReady:" + BlockUtils.leftCropIsReady());
            lines.add("   RightCropReady:" + BlockUtils.rightCropIsReady());
        }
        lines.add("Walkables");
        lines.add("   LeftWalkable:" + GameStateHandler.getInstance().isLeftWalkable());
        lines.add("   RightWalkable:" + GameStateHandler.getInstance().isRightWalkable());
        lines.add("   FrontWalkable:" + GameStateHandler.getInstance().isFrontWalkable());
        lines.add("   BackWalkable:" + GameStateHandler.getInstance().isBackWalkable());
        lines.add("Buffs");
        lines.add("   Cookie: " + GameStateHandler.getInstance().isCookieBuffActive());
        lines.add("   God Pot: " + GameStateHandler.getInstance().isGodPotActive());
    }
}
