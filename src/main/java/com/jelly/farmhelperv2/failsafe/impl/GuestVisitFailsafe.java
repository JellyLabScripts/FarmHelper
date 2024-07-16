package com.jelly.farmhelperv2.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;

public class GuestVisitFailsafe extends Failsafe {
    private static GuestVisitFailsafe instance;
    public static GuestVisitFailsafe getInstance() {
        if (instance == null) {
            instance = new GuestVisitFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.GUEST_VISIT;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnGuestVisit;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnGuestVisit;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnGuestVisit;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnGuestVisit;
    }

    @Override
    public void onTickDetection(TickEvent.ClientTickEvent event) {
        tabListCheckDelay.schedule(5000L);
        if (FarmHelperConfig.pauseOnGuestArrival && wasGuestInGarden && GameStateHandler.getInstance().isGuestInGarden()) {
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of guest visit!", false);
                MacroHandler.getInstance().pauseMacro();
            }
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        if (tabListCheckDelay.isScheduled() && !tabListCheckDelay.passed()) return;
        if (!GameStateHandler.getInstance().isGuestInGarden()
                && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN
                && wasGuestInGarden
        && MacroHandler.getInstance().isMacroToggled()
        && MacroHandler.getInstance().isCurrentMacroPaused()) {
            LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because guest visit is over!", false);
            endOfFailsafeTrigger();
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();
        FailsafeManager.getInstance().setHadEmergency(false);
        MacroHandler.getInstance().resumeMacro();
    }

    @Override
    public void resetStates() {
        tabListCheckDelay.reset();
        wasGuestInGarden = false;
        lastGuestName = "";
    }

    @Override
    public void onChatDetection(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("is visiting Your Garden") && (!GameStateHandler.getInstance().isGuestInGarden()) && !wasGuestInGarden) {
            lastGuestName = message.replace("[SkyBlock] ", "").replace(" is visiting Your Garden!", "");
            wasGuestInGarden = true;
            tabListCheckDelay.schedule(5000L);
            FailsafeManager.getInstance().possibleDetection(this);
            if (!FarmHelperConfig.pauseOnGuestArrival)
                Multithreading.schedule(() -> {
                    if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()
                            && FailsafeManager.getInstance().triggeredFailsafe.get().getType() == FailsafeManager.EmergencyType.GUEST_VISIT) {
                        endOfFailsafeTrigger();
                    }
                }, 100L, TimeUnit.MILLISECONDS);
        }
    }

    private final Clock tabListCheckDelay = new Clock();
    public boolean wasGuestInGarden = false;
    public String lastGuestName = "";
}
