package com.jelly.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.impl.*;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.FlyPathfinder;

import java.util.List;
import java.util.Map;

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
            lines.add("Rotating: " + macro.getRotation().isRotating());
        });
        lines.add("Current plot: " + GameStateHandler.getInstance().getCurrentPlot());
        lines.add("Directions: ");
        lines.add("   Forward: " + GameStateHandler.getInstance().isFrontWalkable());
        lines.add("   Backward: " + GameStateHandler.getInstance().isBackWalkable());
        lines.add("   Left: " + GameStateHandler.getInstance().isLeftWalkable());
        lines.add("   Right: " + GameStateHandler.getInstance().isRightWalkable());
        lines.add("   Not moving: " + GameStateHandler.getInstance().notMoving());
        lines.add("   HasPassedSinceStopped: " + GameStateHandler.getInstance().hasPassedSinceStopped());
        if (Scheduler.getInstance().isToggled()) {
            lines.add("Scheduler: ");
            lines.add("  State: " + LogUtils.capitalize(Scheduler.getInstance().getSchedulerState().toString()));
            lines.add("  Clock: " + Scheduler.getInstance().getSchedulerClock().getRemainingTime());
            lines.add("  isFarming: " + Scheduler.getInstance().isFarming());
        }
        lines.add("Buffs");
        lines.add("   Cookie: " + GameStateHandler.getInstance().getCookieBuffState());
        lines.add("   God Pot: " + GameStateHandler.getInstance().getGodPotState());
        final boolean isRepellentFailsafe = GameStateHandler.getInstance().getPestRepellentState() == GameStateHandler.BuffState.FAILSAFE;
        lines.add("   Repellent: " + GameStateHandler.getInstance().getPestRepellentState() +
                (isRepellentFailsafe ? "" : AutoRepellent.repellentFailsafeClock.passed() ? "" : " & FAILSAFE"));
        if (AutoCookie.getInstance().isRunning()) {
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
        lines.add("Emergency: " + Failsafe.getInstance().getEmergency());
        if (VisitorsMacro.getInstance().isRunning()) {
            lines.add("Visitors Macro");
            lines.add("   State: " + VisitorsMacro.getInstance().getVisitorsState());
            lines.add("   Travel State: " + VisitorsMacro.getInstance().getTravelState());
            lines.add("   Compactor State: " + VisitorsMacro.getInstance().getCompactorState());
            lines.add("   Visitors State: " + VisitorsMacro.getInstance().getVisitorsState());
            lines.add("   Buy State: " + VisitorsMacro.getInstance().getBuyState());
            lines.add("   Clock: " + VisitorsMacro.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck: " + VisitorsMacro.getInstance().getStuckClock().getRemainingTime());
        }
        if (AutoGodPot.getInstance().isRunning()) {
            lines.add("AutoGodPot");
            lines.add("   Mode: " + AutoGodPot.getInstance().getGodPotMode());
            lines.add("   AH State: " + AutoGodPot.getInstance().getAhState());
            lines.add("   Going To AH State: " + AutoGodPot.getInstance().getGoingToAHState());
            lines.add("   Consume Pot State: " + AutoGodPot.getInstance().getConsumePotState());
            lines.add("   Clock: " + AutoGodPot.getInstance().getDelayClock().getRemainingTime());
        }
        if (FarmHelperConfig.highlightPlotWithPests && !PestsDestroyer.getInstance().getPestsPlotMap().isEmpty()) {
            lines.add("Pests:");
            for (Map.Entry<PestsDestroyer.Plot, Integer> pest : PestsDestroyer.getInstance().getPestsPlotMap().entrySet()) {
                lines.add("   Plot: " + pest.getKey().name + " - " + pest.getValue());
            }
        }
        if (PestsDestroyer.getInstance().isRunning()) {
            lines.add("Pests Destroyer");
            lines.add("   State: " + PestsDestroyer.getInstance().getState());
            lines.add("   Clock: " + PestsDestroyer.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck clock: " + PestsDestroyer.getInstance().getStuckClock().getRemainingTime());
            PestsDestroyer.getInstance().getCurrentEntityTarget().ifPresent(target -> lines.add(
                    String.format("   Current Entity Target: %.2f %.2f %.2f", target.getPositionVector().xCoord, target.getPositionVector().yCoord, target.getPositionVector().zCoord))
            );
        }
        if (FlyPathfinder.getInstance().isRunning()) {
            lines.add("FlyPathfinder");
            lines.add("   Path size: " + FlyPathfinder.getInstance().getPathBlocks().size());
        }
        if (AutoSprayonator.getInstance().isToggled()) {
            lines.add("Auto Sprayonator");
            lines.add("   Running: " + AutoSprayonator.getInstance().isRunning());
            lines.add("   State: " + AutoSprayonator.getInstance().getSprayState());
            lines.add("   Item: " + AutoSprayonator.getInstance().getSprayItem());
            lines.add("   Clock: " + AutoSprayonator.getInstance().getSprayonatorDelay().getRemainingTime());
            lines.add("   Check Plot State: " + AutoSprayonator.getInstance().getCheckPlotState());
            lines.add("   Skymart State: " + AutoSprayonator.getInstance().getSkymartPurchaseState());
            lines.add("   Bazaar State: " + AutoSprayonator.getInstance().getBazaarPurchaseState());
            lines.add("   GUI State: " + AutoSprayonator.getInstance().getCurrentGuiState());
            AutoSprayonator.PlotData plotData = AutoSprayonator.getInstance().getSprayonatorPlotStates().get(GameStateHandler.getInstance().getCurrentPlot());
            if (plotData != null) {
                lines.add("   Plot Sprayed: " + plotData.isSprayed());
                lines.add("   Plot Spray Clock: " + plotData.getSprayClock().getRemainingTime());
                lines.add("   Plot Spray Item: " + plotData.getSprayItem());
            }
        }
    }
}
