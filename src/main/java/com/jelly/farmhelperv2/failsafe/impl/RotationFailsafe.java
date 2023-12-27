package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.LagDetector;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.WorldEvent;

public class RotationFailsafe extends Failsafe {
    private static RotationFailsafe instance;
    public static RotationFailsafe getInstance() {
        if (instance == null) {
            instance = new RotationFailsafe();
        }
        return instance;
    }

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

        switch (rotationCheckState) {
            case NONE:
                positionBeforeReacting = mc.thePlayer.getPosition();
                rotationCheckState = RotationCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                positionBeforeReacting = mc.thePlayer.getPosition();
                rotationCheckState = RotationCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Start");
                rotationCheckState = RotationCheckState.WAIT;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                rotationCheckState = RotationCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE:
                String randomMessage;
                if (CustomFailsafeMessagesPage.customRotationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customRotationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (rotation.isRotating())
                    break;
                MovRecPlayer.getInstance().playRandomRecording("ROTATION_CHECK_Continue");
                rotationCheckState = RotationCheckState.WAIT_2;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                rotationCheckState = RotationCheckState.SEND_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                String randomContinueMessage;
                if (CustomFailsafeMessagesPage.customContinueMessages.isEmpty()) {
                    randomContinueMessage = FailsafeManager.getRandomContinueMessage();
                } else {
                    String[] customContinueMessages = CustomFailsafeMessagesPage.customContinueMessages.split("\\|");
                    randomContinueMessage = FailsafeManager.getRandomMessage(customContinueMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomContinueMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomContinueMessage);
                rotationCheckState = RotationCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                rotationCheckState = RotationCheckState.GO_BACK_END;
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || BaritoneHandler.isWalkingToGoalBlock()) {
                    rotationCheckState = RotationCheckState.ROTATE_TO_POS_BEFORE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                FailsafeManager.getInstance().scheduleDelay(200);
                break;
            case ROTATE_TO_POS_BEFORE_2:
                if (rotation.isRotating()) break;
                LogUtils.sendSuccess("[Failsafe] Rotation check finished successfully!");
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                this.endOfFailsafeTrigger();
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        rotationBeforeReacting = null;
        positionBeforeReacting = null;
        rotationCheckState = RotationCheckState.NONE;
        FailsafeManager.getInstance().stopFailsafes();
        if (mc.thePlayer.getPosition().getY() < 100 && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN)
            MacroHandler.getInstance().resumeMacro();
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (!(event.packet instanceof S08PacketPlayerPosLook)) {
            return;
        }
        if (LagDetector.getInstance().isLagging() || LagDetector.getInstance().wasJustLagging()) {
            LogUtils.sendWarning("[Failsafe] Got rotation packet while lagging! Ignoring that one.");
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
            rotationBeforeReacting = new Rotation((float) playerYaw, (float) playerPitch);
            LogUtils.sendDebug("[Failsafe] Rotation detected! Yaw diff: " + yawDiff + ", Pitch diff: " + pitchDiff);
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    @Override
    public void onWorldUnloadDetection(WorldEvent.Unload event) {
        endOfFailsafeTrigger();
    }

    private RotationCheckState rotationCheckState = RotationCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;

    enum RotationCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        WAIT,
        SEND_MESSAGE,
        ROTATE_TO_POS_BEFORE,
        LOOK_AROUND_2,
        WAIT_2,
        SEND_MESSAGE_2,
        ROTATE_TO_POS_BEFORE_2,
        GO_BACK_START,
        GO_BACK_END,
    }
}