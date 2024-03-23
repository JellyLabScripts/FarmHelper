package com.jelly.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.failsafe.impl.GuestVisitFailsafe;
import com.jelly.farmhelperv2.failsafe.impl.LowerAvgBpsFailsafe;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.*;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.helper.FlyPathfinder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;

import java.util.List;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (!FarmHelperConfig.debugMode) return;
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        lines.add("§lFarmHelper v" + FarmHelper.VERSION + " Debug HUD");
        lines.add("wasGuestOnGarden: " + GuestVisitFailsafe.getInstance().wasGuestOnGarden);
        lines.add("Jacob's Contest Collected: " + GameStateHandler.getInstance().getJacobsContestCropNumber());
        if (MovRecPlayer.getInstance().isRunning()) {
            lines.add("Yaw Difference: " + MovRecPlayer.getYawDifference());
        }
        lines.add("Bountiful: " + ProfitCalculator.getInstance().getBountifulProfit());
        lines.add("Purse: " + GameStateHandler.getInstance().getCurrentPurse());
        lines.add("Copper: " + GameStateHandler.getInstance().getCopper());
        lines.add("PD OTT scheduled: " + PestsDestroyerOnTheTrack.getInstance().getDelayStart().isScheduled());
        lines.add("PD OTT remaining: " + PestsDestroyerOnTheTrack.getInstance().getDelayStart().getRemainingTime());
        lines.add("PD OTT stuck remaining: " + PestsDestroyerOnTheTrack.getInstance().getStuckTimer().getRemainingTime());
        lines.add("PD OTT entities: ");
        for (Tuple<Entity, Double> entity : PestsDestroyerOnTheTrack.getInstance().getEntities()) {
            lines.add("   " + entity.getFirst().getPosition() + " Yaw diff: " + String.format("%.2f", entity.getSecond()));
        }
        lines.add("Buffs:");
        lines.add("   God Pot: " + GameStateHandler.getInstance().getGodPotState());
        lines.add("   Cookie: " + GameStateHandler.getInstance().getCookieBuffState());
        lines.add("   Pest Hunter: " + GameStateHandler.getInstance().getPestHunterBonus());
        lines.add("   Pest Repellent: " + GameStateHandler.getInstance().getPestRepellentState());
        lines.add("Location: " + GameStateHandler.getInstance().getLocation());
//        lines.add("Pests in Vacuum: " + GameStateHandler.getInstance().getPestsFromVacuum());
        MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> {
            lines.add("Current state: " + macro.getCurrentState());
            lines.add("Rotating: " + macro.getRotation().isRotating());
        });
//        lines.add("Current plot: " + GameStateHandler.getInstance().getCurrentPlot());
        lines.add("Directions: ");
        lines.add("   Forward: " + GameStateHandler.getInstance().isFrontWalkable());
        lines.add("   Backward: " + GameStateHandler.getInstance().isBackWalkable());
        lines.add("   Left: " + GameStateHandler.getInstance().isLeftWalkable());
        lines.add("   Right: " + GameStateHandler.getInstance().isRightWalkable());
        lines.add("   Not moving: " + GameStateHandler.getInstance().notMoving());
        lines.add("   HasPassedSinceStopped: " + GameStateHandler.getInstance().hasPassedSinceStopped());
        ItemStack heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        lines.add("Cultivating: " + GameStateHandler.getInstance().getCurrentCultivating().getOrDefault(heldItem != null ? heldItem.getDisplayName() : "", 0L));
//        if (Scheduler.getInstance().isToggled()) {
//            lines.add("Scheduler: ");
//            lines.add("  State: " + LogUtils.capitalize(Scheduler.getInstance().getSchedulerState().toString()));
//            lines.add("  Clock: " + Scheduler.getInstance().getSchedulerClock().getRemainingTime());
//            lines.add("  isFarming: " + Scheduler.getInstance().isFarming());
//        }
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
//        lines.add("LagDetector");
//        lines.add("   TPS: " + LagDetector.getInstance().getTickRate());
//        if (LagDetector.getInstance().isLagging()) {
//            lines.add("   Lagging for: " + LagDetector.getInstance().getLaggingTime());
//        }
        lines.add("Average BPS: " + LowerAvgBpsFailsafe.getInstance().getAverageBPS());
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
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent())
            lines.add("Emergency: " + (FailsafeManager.getInstance().triggeredFailsafe.map(failsafe -> failsafe.getType().name()).orElse("None")));
        if (VisitorsMacro.getInstance().isRunning()) {
            lines.add("Visitors Macro");
            lines.add("   State: " + VisitorsMacro.getInstance().getMainState());
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
        if (!FeatureManager.getInstance().getCurrentRunningFeatures().isEmpty()) {
            lines.add("Running Features:");
            FeatureManager.getInstance().getCurrentRunningFeatures().forEach(feature -> lines.add("   " + feature.getName()));
        }
        if (PestsDestroyer.getInstance().isRunning()) {
            lines.add("Pests Destroyer");
            lines.add("   State: " + PestsDestroyer.getInstance().getState());
            lines.add("   Clock: " + PestsDestroyer.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck clock: " + PestsDestroyer.getInstance().getStuckClock().getRemainingTime());
            if (PestsDestroyer.getInstance().getCantReachPest() != 0)
                lines.add("   Can't reach pests for: " + PestsDestroyer.getInstance().getCantReachPest());
            PestsDestroyer.getInstance().getCurrentEntityTarget().ifPresent(target -> lines.add(
                    String.format("   Current Entity Target: %.2f %.2f %.2f", target.getPositionVector().xCoord, target.getPositionVector().yCoord, target.getPositionVector().zCoord))
            );
        }
        if (FlyPathfinder.getInstance().isRunning()) {
            lines.add("FlyPathfinder");
            lines.add("   Path size: " + FlyPathfinder.getInstance().getPathBlocks().size());
            lines.add(String.format("   Player speed: %.2f", FlyPathfinder.getInstance().getPlayerSpeed()));
            lines.add("   Goal: " + FlyPathfinder.getInstance().getGoal());
            lines.add("   Deceleration:");
            lines.add("      Left: " + FlyPathfinder.getInstance().isDeceleratingLeft);
            lines.add("      Right: " + FlyPathfinder.getInstance().isDeceleratingRight);
            lines.add("      Forward: " + FlyPathfinder.getInstance().isDeceleratingForward);
            lines.add("      Backward: " + FlyPathfinder.getInstance().isDeceleratingBackward);
        }
        if (AutoSprayonator.getInstance().isRunning()) {
            lines.add("Auto Sprayonator");
            lines.add("   Enable Delay: " + AutoSprayonator.getInstance().getEnableDelay().getRemainingTime());
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
        if (AutoPestExchange.getInstance().isRunning()) {
            lines.add("Auto Pest Hunter");
            lines.add("   State: " + AutoPestExchange.getInstance().getNewState());
            lines.add("   Clock: " + AutoPestExchange.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck clock: " + AutoPestExchange.getInstance().getStuckClock().getRemainingTime());
        }
    }
}
