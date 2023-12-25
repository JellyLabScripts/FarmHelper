package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class RotationFailsafe extends Failsafe {
    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.ROTATION_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnRotationFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnRotationFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnRotationFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnRotationFailsafe;
    }

    @Override
    public void duringFailsafeTrigger() {

        //blah blah do something

        this.endOfFailsafeTrigger();
    }

    @Override
    public void endOfFailsafeTrigger() {

        // blah blah get to the starting position

        FailsafeManager.getInstance().restartMacroAfterDelay();
        FailsafeManager.getInstance().stopFailsafes();
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (!(event.packet instanceof S08PacketPlayerPosLook)) {
            return;
        }
        S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
        double packetYaw = packet.getYaw();
        double packetPitch = packet.getPitch();
        double playerYaw = mc.thePlayer.rotationYaw;
        double playerPitch = mc.thePlayer.rotationPitch;
        double yawDiff = Math.abs(packetYaw - playerYaw);
        double pitchDiff = Math.abs(packetPitch - playerPitch);
        double threshold = FarmHelperConfig.rotationCheckSensitivity;
        if (yawDiff == 360 && pitchDiff == 0) // prevents false checks
            return;
        if (yawDiff >= threshold || pitchDiff >= threshold) {
            LogUtils.sendDebug("[Failsafe] Rotation detected! Yaw diff: " + yawDiff + ", Pitch diff: " + pitchDiff);
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }
}
