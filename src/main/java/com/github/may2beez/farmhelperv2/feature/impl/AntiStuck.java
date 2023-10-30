package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.KeyBindUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.Timer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Getter
public class AntiStuck implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();

    private static AntiStuck instance;

    public static AntiStuck getInstance() {
        if (instance == null) {
            instance = new AntiStuck();
        }
        return instance;
    }

    enum UnstuckState {
        NONE,
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        DISABLE
    }

    private UnstuckState unstuckState = UnstuckState.NONE;

    private boolean enabled = false;

    private int unstuckAttempts = 0;

    private final Clock unstuckAttemptsClock = new Clock();
    private final Clock delayBetweenMovementsClock = new Clock();
    private final Clock dontCheckForAntistuckClock = new Clock();
    private final Timer notMovingTimer = new Timer();

    @Override
    public String getName() {
        return "AntiStuck";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) return;
        unstuckAttempts++;
        notMovingTimer.schedule();
        if (unstuckAttempts > 2 && FarmHelperConfig.rewarpAt3FailesAntistuck) {
            LogUtils.sendWarning("[Anti Stuck] Failed to unstuck 3 times, returning on spawn");
            MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> macro.triggerWarpGarden(true));
            unstuckAttempts = 0;
            dontCheckForAntistuckClock.schedule(2_500);
            return;
        }
        LogUtils.sendWarning("[Anti Stuck] Enabled");
        enabled = true;
        unstuckState = UnstuckState.NONE;
        notMovingTimer.schedule();
        unstuckAttemptsClock.schedule(30_000);
        KeyBindUtils.stopMovement();
    }

    @Override
    public void stop() {
        if (enabled) {
            LogUtils.sendWarning("[Anti Stuck] Disabled");
        }
        enabled = false;
        delayBetweenMovementsClock.reset();
        unstuckState = UnstuckState.NONE;
        notMovingTimer.reset();
        dontCheckForAntistuckClock.schedule(2_000);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        unstuckAttempts = 0;
        unstuckAttemptsClock.reset();
        lastX = 10000;
        lastZ = 10000;
        lastY = 10000;
        notMovingTimer.reset();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enableAntiStuck;
    }

    private double lastX = 10000;
    private double lastZ = 10000;
    private double lastY = 10000;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled() ||
                !MacroHandler.getInstance().isCurrentMacroEnabled() ||
                MacroHandler.getInstance().isTeleporting() ||
                FeatureManager.getInstance().isAnyOtherFeatureEnabled(this) ||
                mc.currentScreen != null ||
                !isToggled()) {
            notMovingTimer.schedule();
            lastX = 10000;
            lastZ = 10000;
            lastY = 10000;
            dontCheckForAntistuckClock.schedule(2_000);
            return;
        }
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (unstuckAttemptsClock.passed()) {
            unstuckAttempts = 0;
            unstuckAttemptsClock.reset();
        }

        if (dontCheckForAntistuckClock.isScheduled() && !dontCheckForAntistuckClock.passed()) {
            notMovingTimer.schedule();
            lastX = 10000;
            lastZ = 10000;
            lastY = 10000;
            return;
        }
        if (dontCheckForAntistuckClock.isScheduled() && dontCheckForAntistuckClock.passed()) {
            dontCheckForAntistuckClock.reset();
        }

        if (enabled) return;

        double dx = Math.abs(mc.thePlayer.posX - lastX);
        double dz = Math.abs(mc.thePlayer.posZ - lastZ);
        double dy = Math.abs(mc.thePlayer.posY - lastY);

        if (dx < 1 && dz < 1 && dy < 1 && !Failsafe.getInstance().isEmergency() && notMovingTimer.isScheduled() && mc.currentScreen == null) {
            if (notMovingTimer.hasPassed((long) (FarmHelperConfig.timeBetweenChangingRows + FarmHelperConfig.randomTimeBetweenChangingRows + 1_250)) && !Failsafe.getInstance().isTouchingDirtBlock()) {
                start();
            }
        } else {
            notMovingTimer.schedule();
            lastX = mc.thePlayer.posX;
            lastZ = mc.thePlayer.posZ;
            lastY = mc.thePlayer.posY;
        }
    }

    @SubscribeEvent
    public void onTickUnstuck(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (mc.currentScreen != null) return;
        if (!enabled) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;

        if (delayBetweenMovementsClock.isScheduled() && !delayBetweenMovementsClock.passed()) return;

        switch (unstuckState) {
            case NONE:
                KeyBindUtils.stopMovement();
                KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                unstuckState = UnstuckState.LEFT;
                delayBetweenMovementsClock.schedule(300 + (int) (Math.random() * 250));
                break;
            case LEFT:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindSneak);
                unstuckState = UnstuckState.RIGHT;
                delayBetweenMovementsClock.schedule(500 + (int) (Math.random() * 150));
                break;
            case RIGHT:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight, mc.gameSettings.keyBindSneak);
                unstuckState = MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().additionalCheck() ? UnstuckState.FORWARD : UnstuckState.BACKWARD;
                delayBetweenMovementsClock.schedule(500 + (int) (Math.random() * 150));
                break;
            case FORWARD:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindSneak);
                unstuckState = MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().additionalCheck() ? UnstuckState.BACKWARD : UnstuckState.DISABLE;
                delayBetweenMovementsClock.schedule(500 + (int) (Math.random() * 150));
                break;
            case BACKWARD:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, mc.gameSettings.keyBindSneak);
                unstuckState = MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().additionalCheck() ? UnstuckState.DISABLE : UnstuckState.FORWARD;
                delayBetweenMovementsClock.schedule(500 + (int) (Math.random() * 150));
                break;
            case DISABLE:
                KeyBindUtils.stopMovement();
                stop();
                dontCheckForAntistuckClock.schedule(2_500);
                break;
        }
    }
}
