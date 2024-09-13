package com.jelly.farmhelperv2.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.jelly.farmhelperv2.FarmHelper;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.failsafe.impl.GuestVisitFailsafe;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.feature.impl.*;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;

import java.util.List;

public class DebugHUD extends TextHud {
    public DebugHUD() {
        super(true, 1f, 10f, 0.5f, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (!FarmHelperConfig.debugMode) return;
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        lines.add("§lFarmHelper v" + FarmHelper.VERSION + " Debug HUD");
        lines.add("wasGuestInGarden: " + GuestVisitFailsafe.getInstance().wasGuestInGarden);
        lines.add("Jacob's Contest Collected: " + GameStateHandler.getInstance().getJacobsContestCropNumber());
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent())
            lines.add("Server closing in: " + GameStateHandler.getInstance().getServerClosingSeconds().get());
        if (MovRecPlayer.getInstance().isRunning()) {
            lines.add("MovRec Yaw Difference: " + MovRecPlayer.getYawDifference());
        }
        lines.add("Bountiful: " + ProfitCalculator.getInstance().getBountifulProfit());
        ItemStack heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem();
        lines.add("Cultivating: " + GameStateHandler.getInstance().getCurrentCultivating().getOrDefault(heldItem != null ? heldItem.getDisplayName() : "", 0L));
        lines.add("Purse: " + GameStateHandler.getInstance().getCurrentPurse());
        lines.add("Copper: " + GameStateHandler.getInstance().getCopper());

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
        lines.add("Walkable directions: ");
        lines.add("   Forward: " + GameStateHandler.getInstance().isFrontWalkable());
        lines.add("   Backward: " + GameStateHandler.getInstance().isBackWalkable());
        lines.add("   Left: " + GameStateHandler.getInstance().isLeftWalkable());
        lines.add("   Right: " + GameStateHandler.getInstance().isRightWalkable());
        lines.add("   Not moving: " + GameStateHandler.getInstance().notMoving());
        lines.add("   HasPassedSinceStopped: " + GameStateHandler.getInstance().hasPassedSinceStopped());
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
//        if (DesyncChecker.getInstance().isToggled()) {
//            lines.add("Desync Checker");
//            lines.add("   Clicked blocks: " + DesyncChecker.getInstance().getClickedBlocks().size());
//            lines.add("   Desync: " + DesyncChecker.getInstance().isRunning());
//        }
        if (AutoRepellent.getInstance().isRunning()) {
            lines.add("AutoRepellent");
            lines.add("   State: " + AutoRepellent.getInstance().getState());
            lines.add("   Clock: " + AutoRepellent.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Not enough copper: " + AutoRepellent.getInstance().isNotEnoughCopper());
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
            lines.add("Blocking Main Thread: " + FeatureManager.getInstance().getPauseExecutionFeatures().size());
            for (IFeature feature : FeatureManager.getInstance().getPauseExecutionFeatures()) {
                lines.add("   " + feature.getName());
            }
            lines.add("Running Features:");
            FeatureManager.getInstance().getCurrentRunningFeatures().forEach(feature -> lines.add("   " + feature.getName()));
        }
        if (PestsDestroyer.getInstance().isRunning()) {
            lines.add("Pests Destroyer");
            lines.add("   State: " + PestsDestroyer.getInstance().getState());
            lines.add("   Clock: " + PestsDestroyer.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck clock: " + PestsDestroyer.getInstance().getStuckClock().getRemainingTime());
            lines.add("   Use AOTV: " + FlyPathFinderExecutor.getInstance().isUseAOTV());
            if (PestsDestroyer.getInstance().getCantReachPest() != 0)
                lines.add("   Can't reach pests for: " + PestsDestroyer.getInstance().getCantReachPest());
            PestsDestroyer.getInstance().getCurrentEntityTarget().ifPresent(target -> lines.add(
                    String.format("   Current Entity Target: %.2f %.2f %.2f", target.getPositionVector().xCoord, target.getPositionVector().yCoord, target.getPositionVector().zCoord))
            );
//            lines.add("PD OTT:");
//            lines.add("   scheduled: " + PestsDestroyerOnTheTrack.getInstance().getDelayStart().isScheduled());
//            lines.add("   remaining: " + PestsDestroyerOnTheTrack.getInstance().getDelayStart().getRemainingTime());
//            lines.add("   stuck remaining: " + PestsDestroyerOnTheTrack.getInstance().getStuckTimer().getRemainingTime());
//            lines.add("   entities: ");
//            for (Tuple<Entity, Double> entity : PestsDestroyerOnTheTrack.getInstance().getEntities()) {
//                lines.add("   " + entity.getFirst().getPosition() + " Yaw diff: " + String.format("%.2f", entity.getSecond()));
//            }
        }
        if (AutoPestExchange.getInstance().isRunning()) {
            lines.add("Auto Pest Hunter");
            lines.add("   State: " + AutoPestExchange.getInstance().getNewState());
            lines.add("   Clock: " + AutoPestExchange.getInstance().getDelayClock().getRemainingTime());
            lines.add("   Stuck clock: " + AutoPestExchange.getInstance().getStuckClock().getRemainingTime());
        }
        if (BPSTracker.getInstance().isRunning()) {
            lines.add("BPSTracker");
            lines.add("   BPS: " + BPSTracker.getInstance().getBPS());
            lines.add("   BPS queue size: " + BPSTracker.getInstance().bpsQueue.size());
            lines.add("   Blocks broken: " + BPSTracker.getInstance().blocksBroken);
            lines.add("   Total blocks broken: " + BPSTracker.getInstance().totalBlocksBroken);
            lines.add("   Paused: " + BPSTracker.getInstance().isPaused);
            lines.add("   isResumingScheduled: " + BPSTracker.getInstance().isResumingScheduled);
            lines.add("   Pause start time: " + BPSTracker.getInstance().pauseStartTime);
            lines.add("   Last known BPS: " + BPSTracker.getInstance().lastKnownBPS);
            lines.add("   Elapsed time: " + BPSTracker.getInstance().elapsedTime);
            if (!BPSTracker.getInstance().bpsQueue.isEmpty()
                    && BPSTracker.getInstance().bpsQueue.getFirst() != null
                    && BPSTracker.getInstance().bpsQueue.getLast() != null) {
                lines.add("   First timestamp: " + BPSTracker.getInstance().bpsQueue.getFirst().getSecond());
                lines.add("   Last timestamp: " + BPSTracker.getInstance().bpsQueue.getLast().getSecond());
            }
        }
    }
}
