package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Scheduler implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static Scheduler instance;
    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    @Getter
    private final Clock schedulerClock = new Clock();

    @Override
    public String getName() {
        return "Scheduler";
    }

    @Override
    public boolean isRunning() {
        return schedulerClock.isScheduled();
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
        schedulerState = SchedulerState.FARMING;
        schedulerClock.schedule(TimeUnit.MINUTES.toMillis(FarmHelperConfig.schedulerFarmingTime));
        if (FarmHelperConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            schedulerClock.pause();
        }
    }

    @Override
    public void stop() {
        schedulerState = SchedulerState.NONE;
        schedulerClock.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        GameStateHandler.getInstance().setWasInJacobContest(false);
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enableScheduler;
    }

    public enum SchedulerState {
        NONE,
        FARMING,
        BREAK
    }

    @Getter
    private SchedulerState schedulerState = SchedulerState.NONE;

    public boolean isFarming() {
        return !FarmHelperConfig.enableScheduler || schedulerState == SchedulerState.FARMING;
    }

    public String getStatusString() {
        if (FarmHelperConfig.enableScheduler) {
            return (schedulerState == SchedulerState.FARMING ? "Farming" : "Break") + " for "
                    + LogUtils.formatTime(Math.max(schedulerClock.getRemainingTime(), 0)) + (schedulerClock.isPaused() ? " (Paused)" : "");
        } else {
            return "Farming";
        }
    }

    public void pause() {
        LogUtils.sendDebug("[Scheduler] Pausing");
        schedulerClock.pause();
    }

    public void resume() {
        LogUtils.sendDebug("[Scheduler] Resuming");
        schedulerClock.resume();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelperConfig.enableScheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.getInstance().getCurrentMacro().isPresent())
            return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (FarmHelperConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest() && !schedulerClock.isPaused()) {
            schedulerClock.pause();
            GameStateHandler.getInstance().setWasInJacobContest(true);
        } else if (FarmHelperConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().isWasInJacobContest() && !GameStateHandler.getInstance().inJacobContest() && schedulerClock.isPaused()) {
            schedulerClock.resume();
            GameStateHandler.getInstance().setWasInJacobContest(false);
        }

        if (MacroHandler.getInstance().isMacroToggled() && MacroHandler.getInstance().isCurrentMacroEnabled() && schedulerState == SchedulerState.FARMING && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Farming time has passed, stopping");
            MacroHandler.getInstance().pauseMacro();
            schedulerState = SchedulerState.BREAK;
            schedulerClock.schedule((long) (FarmHelperConfig.schedulerBreakTime + (Math.random() * FarmHelperConfig.schedulerBreakTimeRandomness)));
        } else if (MacroHandler.getInstance().isMacroToggled() && schedulerState == SchedulerState.BREAK && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Break time has passed, starting");
            schedulerState = SchedulerState.FARMING;
            schedulerClock.schedule((long)(FarmHelperConfig.schedulerFarmingTime + (Math.random() * FarmHelperConfig.schedulerFarmingTimeRandomness)));
            MacroHandler.getInstance().resumeMacro();
        }
    }
}
