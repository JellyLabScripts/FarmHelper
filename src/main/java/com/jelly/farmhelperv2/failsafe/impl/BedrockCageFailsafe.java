package com.jelly.farmhelperv2.failsafe.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.TimeUnit;

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
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (!(event.packet instanceof S08PacketPlayerPosLook)) {
            return;
        }
        if (mc.thePlayer.getPosition().getY() < 66) return;
        S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
        Vec3 currentPlayerPos = mc.thePlayer.getPositionVector();
        Vec3 packetPlayerPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

        if (packet.getY() > 80) {
            rotationBeforeTeleporting = new Rotation(mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch);
            Multithreading.schedule(() -> {
                for (int i = 0; i < 5; i++) {
                    if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) {
                        return;
                    }
                    if (BlockUtils.bedrockCount() > 3) {
                        FailsafeManager.getInstance().possibleDetection(this);
                        return;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, 100, TimeUnit.MILLISECONDS);
            return;
        }
//        final double lastReceivedPacketDistance = currentPlayerPos.distanceTo(LagDetector.getInstance().getLastPacketPosition());
//        final double playerMovementSpeed = mc.thePlayer.getAttributeMap().getAttributeInstanceByName("generic.movementSpeed").getAttributeValue();
//        final int ticksSinceLastPacket = (int) Math.ceil(LagDetector.getInstance().getTimeSinceLastTick() / 50D);
//        final double estimatedMovement = playerMovementSpeed * ticksSinceLastPacket;
//        if (lastReceivedPacketDistance > 7.5D && Math.abs(lastReceivedPacketDistance - estimatedMovement) < FarmHelperConfig.teleportCheckLagSensitivity)
//            return;
        if (bedrockCageCheckState != BedrockCageCheckState.NONE
                && bedrockCageCheckState != BedrockCageCheckState.WAIT_BEFORE_START
                && bedrockCageCheckState != BedrockCageCheckState.ROTATE_TO_POS_BEFORE
                && bedrockCageCheckState != BedrockCageCheckState.END) {
            if (MovRecPlayer.getInstance().isRunning())
                MovRecPlayer.getInstance().stop();
            LogUtils.sendFailsafeMessage("[Failsafe] You've just passed the failsafe check for bedrock cage!", FailsafeNotificationsPage.tagEveryoneOnBedrockCageFailsafe);
            bedrockCageCheckState = BedrockCageCheckState.ROTATE_TO_POS_BEFORE;
            FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        switch (bedrockCageCheckState) {
            case NONE:
                bedrockCageCheckState = BedrockCageCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest());
                bedrockCageCheckState = BedrockCageCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                if (BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.bedrock))
                    bedrockOnLeft = true;
                else if (BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.bedrock))
                    bedrockOnLeft = false;
                if (bedrockOnLeft)
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Left_Start_");
                else
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Right_Start_");
                bedrockCageCheckState = BedrockCageCheckState.SEND_MESSAGE;
                break;
            case SEND_MESSAGE:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                String randomMessage;
                if (CustomFailsafeMessagesPage.customBedrockMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customBedrockMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                bedrockCageCheckState = BedrockCageCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                break;
            case LOOK_AROUND_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_JumpBoost_");
                } else if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Fly_");
                } else {
                    MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_OnGround_");
                }
                bedrockCageCheckState = BedrockCageCheckState.SEND_MESSAGE_2;
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
                break;
            case WAIT_UNTIL_TP_BACK:
                if (BlockUtils.bedrockCount() < 1 && !mc.thePlayer.isAirBorne) {
                    MovRecPlayer.getInstance().stop();
                    bedrockCageCheckState = BedrockCageCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    return;
                }
                if (MovRecPlayer.getInstance().isRunning())
                    break;

                MovRecPlayer.getInstance().playRandomRecording("BEDROCK_CHECK_Wait_");
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
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeTeleporting.getYaw(), rotationBeforeTeleporting.getPitch()),
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
        bedrockCageCheckState = BedrockCageCheckState.NONE;
        FailsafeManager.getInstance().restartMacroAfterDelay();
        FailsafeManager.getInstance().stopFailsafes();
    }

    @SubscribeEvent
    public void onWorldUnloadDetection(WorldEvent.Unload event) {
        endOfFailsafeTrigger();
    }

    private BedrockCageCheckState bedrockCageCheckState = BedrockCageCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private Rotation rotationBeforeTeleporting = null;
    private boolean bedrockOnLeft = false;

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
