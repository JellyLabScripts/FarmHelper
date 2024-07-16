package com.jelly.farmhelperv2.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.AutoSell;
import com.jelly.farmhelperv2.feature.impl.LagDetector;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class FullInventoryFailsafe extends Failsafe {
    private static FullInventoryFailsafe instance;
    private final Clock clock = new Clock();

    public static FullInventoryFailsafe getInstance() {
        if (instance == null) {
            instance = new FullInventoryFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.FULL_INVENTORY;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnInventoryFull;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnFullInventory;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnFullInventory;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnInventoryFull;
    }
    @Override
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        if (FailsafeManager.getInstance().isHadEmergency())
            return;
        if (FeatureManager.getInstance().shouldPauseMacroExecution())
            return;
        if (LagDetector.getInstance().isLagging() || LagDetector.getInstance().wasJustLagging()) {
            LogUtils.sendDebug("[Failsafe] Lag detected! Cancelling full inventory failsafe...");
            return;
        }

        if (PlayerUtils.isInventoryFull(mc.thePlayer)) {
            if (clock.isScheduled() && clock.passed())
                FailsafeManager.getInstance().possibleDetection(this);
            if (!clock.isScheduled())
                clock.schedule(555);

        } else {
            clock.reset();
        }
    }

    // use item change failsafe movements
    @Override
    public void duringFailsafeTrigger() {
        switch (itemChangeState) {
            case NONE:
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                itemChangeState = ItemChangeState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                KeyBindUtils.stopMovement();
                itemChangeState = ItemChangeState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("ITEM_CHANGE_");
                itemChangeState = ItemChangeState.SWAP_BACK_ITEM;
                break;
            case SWAP_BACK_ITEM:
                if (MovRecPlayer.getInstance().isRunning()) return;
                itemChangeState = ItemChangeState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case END:
                PlayerUtils.getTool();
                endOfFailsafeTrigger();
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();

        Multithreading.schedule(() -> {
            if (GameStateHandler.getInstance().getCookieBuffState() != GameStateHandler.BuffState.ACTIVE) {
                LogUtils.sendDebug("[Failsafe] Looking at sb menu...");
                mc.thePlayer.inventory.currentItem = 8;
                KeyBindUtils.rightClick();
            } else {
                LogUtils.sendDebug("[Failsafe] Enabling auto sell...");
                AutoSell.getInstance().start();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void resetStates() {
        clock.reset();
        itemChangeState = ItemChangeState.NONE;
    }

    private ItemChangeState itemChangeState = ItemChangeState.NONE;
    enum ItemChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SWAP_BACK_ITEM,
        END
    }
}