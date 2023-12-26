package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.util.BlockPos;

public class BedrockCageFailsafe extends Failsafe {
    private static BedrockCageFailsafe instance;
    public static BedrockCageFailsafe getInstance() {
        if (instance == null) {
            instance = new BedrockCageFailsafe();
        }
        return instance;
    }

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

        switch (bedrockCageCheckState) {
            case NONE:
                positionBeforeReacting = mc.thePlayer.getPosition();
                bedrockCageCheckState = BedrockCageCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                positionBeforeReacting = mc.thePlayer.getPosition();
                bedrockCageCheckState = BedrockCageCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Start");
                bedrockCageCheckState = BedrockCageCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                String randomMessage;
                if (CustomFailsafeMessagesPage.customRotationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customRotationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                bedrockCageCheckState = BedrockCageCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (Math.random() < 0.2) {
                    bedrockCageCheckState = BedrockCageCheckState.SEND_MESSAGE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                    break;
                } else if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_JumpBoost_");
                } else if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Fly_");
                } else {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_OnGround_");
                }
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
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
                bedrockCageCheckState = BedrockCageCheckState.WAIT_UNTIL_TP_BACK;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WAIT_UNTIL_TP_BACK:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    bedrockCageCheckState = BedrockCageCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                } else {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Wait_");
                    FailsafeManager.getInstance().scheduleRandomDelay(2500, 4000);
                }
                break;
//            case GO_BACK_START:
//                if (MovRecPlayer.getInstance().isRunning())
//                    break;
//                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
//                    bedrockCageCheckState = BedrockCageCheckState.ROTATE_TO_POS_BEFORE_2;
//                    break;
//                }
//                BaritoneHandler.walkToBlockPos(positionBeforeReacting);
//                bedrockCageCheckState = BedrockCageCheckState.GO_BACK_END;
//                break;
//            case GO_BACK_END:
//                if (BaritoneHandler.hasFailed() || BaritoneHandler.isWalkingToGoalBlock()) {
//                    bedrockCageCheckState = BedrockCageCheckState.ROTATE_TO_POS_BEFORE_2;
//                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
//                    break;
//                }
//                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
//                FailsafeManager.getInstance().scheduleDelay(200);
//                break;
            case ROTATE_TO_POS_BEFORE:
                if (rotation.isRotating()) break;
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        750, null));
                bedrockCageCheckState = BedrockCageCheckState.LOOK_AROUND_3;
                FailsafeManager.getInstance().scheduleRandomDelay(800, 200);
                break;
            case LOOK_AROUND_3:
                MovRecPlayer.getInstance().playRandomRecording("ITEM_CHANGE_");
                bedrockCageCheckState = BedrockCageCheckState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(800, 200);
                break;
            case END:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                this.endOfFailsafeTrigger();
                break;
        }
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

    private BedrockCageCheckState bedrockCageCheckState = BedrockCageCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    enum BedrockCageCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SEND_MESSAGE,
        LOOK_AROUND_2,
        SEND_MESSAGE_2,
        WAIT_UNTIL_TP_BACK,
        GO_BACK_START,
        GO_BACK_END,
        LOOK_AROUND_3,
        ROTATE_TO_POS_BEFORE,
        END
    }
}
