package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.util.BlockUtils;

public class BedrockCageFailsafe extends Failsafe {
    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.BEDROCK_CAGE_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnBedrockCageFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnBedrockCageFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnBedrockCageFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnBedrockCageFailsafe;
    }

    @Override
    public void duringFailsafeTrigger() {

    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().restartMacroAfterDelay();
        FailsafeManager.getInstance().stopFailsafes();
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (mc.thePlayer.getPosition().getY() < 50) return;
        if (BlockUtils.bedrockCount() <= 2) return;
        FailsafeManager.getInstance().possibleDetection(this);
    }
}
