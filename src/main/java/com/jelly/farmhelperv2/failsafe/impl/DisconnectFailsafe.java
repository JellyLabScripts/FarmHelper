package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.AutoReconnect;
import com.jelly.farmhelperv2.feature.impl.BanInfoWS;
import com.jelly.farmhelperv2.feature.impl.Scheduler;
import com.jelly.farmhelperv2.feature.impl.Scheduler.SchedulerState;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class DisconnectFailsafe extends Failsafe {
    private static DisconnectFailsafe instance;

    public static DisconnectFailsafe getInstance() {
        if (instance == null) {
            instance = new DisconnectFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.DISCONNECT;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnDisconnectFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnDisconnectFailsafe;
    }

    @Override
    public void onDisconnectDetection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
//        if (MacroHandler.getInstance().isTeleporting()) return;
        if (BanInfoWS.getInstance().isBanwave() && FarmHelperConfig.enableLeavePauseOnBanwave && !FarmHelperConfig.banwaveAction) {
            return;
        }

        if (Scheduler.getInstance().isRunning() && Scheduler.getInstance().getSchedulerState() == SchedulerState.BREAK && FarmHelperConfig.schedulerDisconnectDuringBreak){
            return;
        }

        FailsafeManager.getInstance().possibleDetection(this);
    }

    @Override
    public void duringFailsafeTrigger() {
        if (!AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Trying to reconnect...");
            LogUtils.sendNotification("Farm Helper", "Disconnected from server! Trying to reconnect...");
            AutoReconnect.getInstance().getReconnectDelay().schedule(5_000);
            AutoReconnect.getInstance().start();
        } else if (!AutoReconnect.getInstance().isRunning() && !AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Stopping macro...");
            LogUtils.sendNotification("Farm Helper", "Disconnected from server! Stopping macro...");
            MacroHandler.getInstance().disableMacro();
            FailsafeManager.getInstance().stopFailsafes();
        } else if (AutoReconnect.getInstance().isRunning()) {
            System.out.println("[Reconnect] Disconnected from server! Reconnect is already running!");
            LogUtils.sendNotification("Farm Helper", "Disconnected from server! Reconnect is already running!");
            FailsafeManager.getInstance().stopFailsafes();
        }
    }

    @Override
    public void endOfFailsafeTrigger() {

    }
}
