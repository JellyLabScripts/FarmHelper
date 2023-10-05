package com.github.may2beez.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.*;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
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
        lines.add("Scheduler: " + Scheduler.getInstance().isToggled());
        if (Scheduler.getInstance().isToggled()) {
            lines.add("  State: " + LogUtils.capitalize(Scheduler.getInstance().getSchedulerState().toString()));
            lines.add("  Clock: " + Scheduler.getInstance().getSchedulerClock().getRemainingTime());
            lines.add("  isFarming: " + Scheduler.getInstance().isFarming());
        }
        lines.add("Buffs");
        lines.add("   Cookie: " + GameStateHandler.getInstance().getCookieBuffState());
        lines.add("   God Pot: " + GameStateHandler.getInstance().getGodPotState());
        if (AutoCookie.getInstance().isToggled()) {
            lines.add("AutoCookie");
            lines.add("   Main State: " + AutoCookie.getInstance().getMainState());
            lines.add("   Movie Cookie State: " + AutoCookie.getInstance().getMoveCookieState());
            lines.add("   Bazaar State: " + AutoCookie.getInstance().getBazaarState());
            lines.add("   Clock: " + AutoCookie.getInstance().getAutoCookieDelay().getRemainingTime());
            lines.add("   Timeout clock: " + AutoCookie.getInstance().getTimeoutClock().getRemainingTime());
        }
        if (AntiStuck.getInstance().isRunning()) {
            lines.add("AntiStuck");
            lines.add("   State: " + AntiStuck.getInstance().getUnstuckState());
            lines.add("   Delay between change state: " + AntiStuck.getInstance().getDelayBetweenMovementsClock().getRemainingTime());
            lines.add("   Attempts: " + AntiStuck.getInstance().getUnstuckAttempts());
            lines.add("   Unstuck Attempts reset in: " + AntiStuck.getInstance().getUnstuckAttemptsClock().getRemainingTime());
        }
        if (LagDetector.getInstance().isLagging()) {
            lines.add("LagDetector");
            lines.add("   Lagging for: " + LagDetector.getInstance().getLaggingTime());
        }
        if (DesyncChecker.getInstance().isToggled()) {
            lines.add("Desync Checker");
            lines.add("   Clicked blocks: " + DesyncChecker.getInstance().getClickedBlocks().size());
            lines.add("   Desync: " + DesyncChecker.getInstance().isRunning());
        }
        if (AutoSell.getInstance().isRunning()) {
            lines.add("AutoSell");
            lines.add("   Sacks State: " + AutoSell.getInstance().getSacksState());
            lines.add("   Bazaar State: " + AutoSell.getInstance().getBazaarState());
            lines.add("   NPC State: " + AutoSell.getInstance().getNpcState());
        }
    }
}
