package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class Scheduler implements IFeature {
    private static Scheduler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    @Getter
    private final Clock schedulerClock = new Clock();
    @Getter
    @Setter
    private SchedulerState schedulerState = SchedulerState.NONE;

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

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
        schedulerClock.schedule((long) (FarmHelperConfig.schedulerFarmingTime * 60_000f + (Math.random() * FarmHelperConfig.schedulerFarmingTimeRandomness * 60_000f)));
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

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public boolean isFarming() {
        return !FarmHelperConfig.enableScheduler || schedulerState == SchedulerState.FARMING;
    }

    public String getStatusString() {
        if (FarmHelperConfig.enableScheduler) {
            return (schedulerState == SchedulerState.FARMING ? (EnumChatFormatting.GREEN + "Farming") : (EnumChatFormatting.DARK_AQUA + "Break")) + EnumChatFormatting.RESET + " for " +
                    EnumChatFormatting.GOLD + LogUtils.formatTime(Math.max(schedulerClock.getRemainingTime(), 0)) + EnumChatFormatting.RESET + (schedulerClock.isPaused() ? " (Paused)" : "");
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

    public void farmingTime() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        schedulerState = SchedulerState.FARMING;
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
        }
        MacroHandler.getInstance().resumeMacro();
        schedulerClock.schedule((long) ((FarmHelperConfig.schedulerFarmingTime * 60_000f) + (Math.random() * FarmHelperConfig.schedulerFarmingTimeRandomness * 60_000f)));
    }

    public void breakTime() {
        schedulerState = SchedulerState.BREAK;
        MacroHandler.getInstance().pauseMacro(true);
        schedulerClock.schedule((long) ((FarmHelperConfig.schedulerBreakTime * 60_000f) + (Math.random() * FarmHelperConfig.schedulerBreakTimeRandomness * 60_000f)));
        KeyBindUtils.stopMovement();
        Multithreading.schedule(() -> {
            long randomTime4 = FarmHelperConfig.getRandomRotationTime();
            this.rotation.easeTo(
                    new RotationConfiguration(
                            new Rotation(
                                    (float) (mc.thePlayer.rotationYaw + Math.random() * 20 - 10),
                                    (float) Math.min(90, Math.max(-90, (mc.thePlayer.rotationPitch + Math.random() * 30 - 15)))
                            ),
                            randomTime4, null
                    )
            );
            if (FarmHelperConfig.openInventoryOnSchedulerBreaks) {
                Multithreading.schedule(InventoryUtils::openInventory, randomTime4 + 350, TimeUnit.MILLISECONDS);
            }
        }, (long) (300 + Math.random() * 200), TimeUnit.MILLISECONDS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelperConfig.enableScheduler || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.getInstance().getCurrentMacro().isPresent())
            return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (FarmHelperConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().inJacobContest() && !GameStateHandler.getInstance().isWasInJacobContest()) {
            LogUtils.sendDebug("[Scheduler] Jacob contest started, pausing scheduler");
            schedulerClock.pause();
            if (schedulerState == SchedulerState.BREAK) {
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                }
                MacroHandler.getInstance().resumeMacro();
                pause();
            }
            GameStateHandler.getInstance().setWasInJacobContest(true);
            return;
        } else if (FarmHelperConfig.pauseSchedulerDuringJacobsContest && GameStateHandler.getInstance().isWasInJacobContest() && !GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendDebug("[Scheduler] Jacob contest ended, resuming scheduler");
            schedulerClock.resume();
            if (schedulerState == SchedulerState.BREAK) {
                MacroHandler.getInstance().pauseMacro();
            }
            GameStateHandler.getInstance().setWasInJacobContest(false);
            return;
        }

        if (MacroHandler.getInstance().isMacroToggled() && MacroHandler.getInstance().isCurrentMacroEnabled() && schedulerState == SchedulerState.FARMING && !schedulerClock.isPaused() && schedulerClock.passed()) {
            if (MacroHandler.getInstance().getCurrentMacro().get().getCurrentState().equals(AbstractMacro.State.SWITCHING_LANE)) {
                LogUtils.sendDebug("[Scheduler] Macro is switching row, won't pause.");
                return;
            }
            LogUtils.sendDebug("[Scheduler] Farming time has passed, stopping");
            breakTime();
        } else if (MacroHandler.getInstance().isMacroToggled() && schedulerState == SchedulerState.BREAK && !schedulerClock.isPaused() && schedulerClock.passed()) {
            LogUtils.sendDebug("[Scheduler] Break time has passed, starting");
            farmingTime();
        }
    }

    public enum SchedulerState {
        NONE,
        FARMING,
        BREAK
    }
}
