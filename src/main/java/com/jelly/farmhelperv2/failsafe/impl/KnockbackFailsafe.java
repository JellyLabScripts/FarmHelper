package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.MovRecPlayer;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class KnockbackFailsafe extends Failsafe {
    private static KnockbackFailsafe instance;

    public static KnockbackFailsafe getInstance() {
        if (instance == null) {
            instance = new KnockbackFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.KNOCKBACK_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnKnockbackFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnKnockbackFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnKnockbackFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnKnockbackFailsafe;
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting())
            return;
        if (!(event.packet instanceof S12PacketEntityVelocity)) {
            return;
        }
        if (((S12PacketEntityVelocity) event.packet).getEntityID() != mc.thePlayer.getEntityId())
            return;
        if (((S12PacketEntityVelocity) event.packet).getMotionY() < FarmHelperConfig.verticalKnockbackThreshold)
            return;

        if (FlyPathFinderExecutor.getInstance().isRunning()) {
            AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox().expand(2, 2.5, 2);
            for (BlockPos blockPos : BlockUtils.getBlocksInBB(boundingBox)) {
                Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                if (block.equals(Blocks.cactus)) {
                    LogUtils.sendDebug("[Failsafe] Knockback detected, but cactus is inside the bounding box. Ignoring.");
                    return;
                }
            }
        }

        rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        positionBeforeReacting = mc.thePlayer.getPosition();
        LogUtils.sendWarning("[Failsafe] Knockback detected! MotionY: " + ((S12PacketEntityVelocity) event.packet).getMotionY());

        FailsafeManager.getInstance().possibleDetection(this);
    }

    @Override
    public void duringFailsafeTrigger() {
        if (mc.currentScreen != null) {
            PlayerUtils.closeScreen();
            // just in case something in the hand keeps opening the screen
            if (FailsafeManager.getInstance().swapItemDuringRecording && mc.thePlayer.inventory.currentItem > 1)
                FailsafeManager.getInstance().selectNextItemSlot();
            return;
        }
        switch (knockbackCheckState) {
            case NONE:
                knockbackCheckState = KnockbackCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                if (rotationBeforeReacting == null)
                    rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                if (positionBeforeReacting == null)
                    positionBeforeReacting = mc.thePlayer.getPosition();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                knockbackCheckState = KnockbackCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_Start_");
                knockbackCheckState = KnockbackCheckState.WAIT_BEFORE_SENDING_MESSAGE_1;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_1:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!FarmHelperConfig.sendFailsafeMessage) {
                    knockbackCheckState = KnockbackCheckState.ROTATE_TO_POS_BEFORE;
                    FailsafeManager.getInstance().scheduleRandomDelay(300, 600);
                    break;
                }
                if (!CustomFailsafeMessagesPage.customJacobMessages.isEmpty()
                        && GameStateHandler.getInstance().inJacobContest()
                        && Math.random() > CustomFailsafeMessagesPage.customJacobChance / 100.0) {
                    String[] customJacobMessages = CustomFailsafeMessagesPage.customJacobMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customJacobMessages);
                } else if (CustomFailsafeMessagesPage.customKnockbackMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customKnockbackMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                knockbackCheckState = KnockbackCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                knockbackCheckState = KnockbackCheckState.ROTATE_TO_POS_BEFORE;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case ROTATE_TO_POS_BEFORE:
                if (FeatureManager.getInstance().isAnyOtherFeatureEnabled()) {
                    LogUtils.sendDebug("Ending failsafe earlier because another feature is enabled");
                    knockbackCheckState = KnockbackCheckState.END;
                    break;
                }
                FailsafeManager.getInstance().rotation.easeTo(new RotationConfiguration(new Rotation((float) (rotationBeforeReacting.getYaw() + (Math.random() * 30 - 15)), (float) (Math.random() * 30 + 30)),
                        500, null));
                knockbackCheckState = KnockbackCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().swapItemDuringRecording = Math.random() < 0.2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (rotation.isRotating())
                    break;
                if (Math.random() < 0.2) {
                    if (Math.random() > 0.4)
                        knockbackCheckState = KnockbackCheckState.WAIT_BEFORE_SENDING_MESSAGE_2;
                    else
                        knockbackCheckState = KnockbackCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                    break;
                } else if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_JumpBoost_");
                } else if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_Fly_");
                } else {
                    MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_OnGround_");
                }
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WAIT_BEFORE_SENDING_MESSAGE_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (!FarmHelperConfig.sendFailsafeMessage) {
                    knockbackCheckState = KnockbackCheckState.GO_BACK_START;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                if (CustomFailsafeMessagesPage.customContinueMessages.isEmpty()) {
                    randomContinueMessage = FailsafeManager.getRandomContinueMessage();
                } else {
                    String[] customContinueMessages = CustomFailsafeMessagesPage.customContinueMessages.split("\\|");
                    randomContinueMessage = FailsafeManager.getRandomMessage(customContinueMessages);
                }
                knockbackCheckState = KnockbackCheckState.SEND_MESSAGE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(3500, 2500);
                break;
            case SEND_MESSAGE_2:
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomContinueMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomContinueMessage);
                knockbackCheckState = KnockbackCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (FailsafeManager.getInstance().swapItemDuringRecording)
                    FailsafeManager.getInstance().swapItemDuringRecording = false;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    knockbackCheckState = KnockbackCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 500) {
                    BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                    knockbackCheckState = KnockbackCheckState.GO_BACK_END;
                } else {
                    knockbackCheckState = KnockbackCheckState.WARP_GARDEN;
                }
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || !BaritoneHandler.isWalkingToGoalBlock()) {
                    knockbackCheckState = KnockbackCheckState.ROTATE_TO_POS_BEFORE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                FailsafeManager.getInstance().scheduleDelay(200);
                break;
            case ROTATE_TO_POS_BEFORE_2:
                if (rotation.isRotating()) break;
                if (MacroHandler.getInstance().isTeleporting()) break;
                FailsafeManager.getInstance().rotation.easeTo(new RotationConfiguration(new Rotation((float) (rotationBeforeReacting.getYaw() + (Math.random() * 30 - 15)), (float) (Math.random() * 30 + 30)),
                        500, null));
                knockbackCheckState = KnockbackCheckState.END;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case WARP_GARDEN:
                MacroHandler.getInstance().triggerWarpGarden(true, false);
                knockbackCheckState = KnockbackCheckState.ROTATE_TO_POS_BEFORE_2;
                FailsafeManager.getInstance().scheduleRandomDelay(3000, 1000);
                break;
            case END:
                this.endOfFailsafeTrigger();
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        FailsafeManager.getInstance().stopFailsafes();
        if (mc.thePlayer.getPosition().getY() < 100 && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN)
            FailsafeManager.getInstance().restartMacroAfterDelay();
    }

    @Override
    public void resetStates() {
        knockbackCheckState = KnockbackCheckState.NONE;
        rotationBeforeReacting = null;
        positionBeforeReacting = null;
        randomMessage = null;
        randomContinueMessage = null;
        rotation.reset();
    }

    private KnockbackCheckState knockbackCheckState = KnockbackCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    String randomMessage;
    String randomContinueMessage;

    enum KnockbackCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        WAIT_BEFORE_SENDING_MESSAGE_1,
        SEND_MESSAGE,
        ROTATE_TO_POS_BEFORE,
        LOOK_AROUND_2,
        WAIT_BEFORE_SENDING_MESSAGE_2,
        SEND_MESSAGE_2,
        ROTATE_TO_POS_BEFORE_2,
        GO_BACK_START,
        GO_BACK_END,
        WARP_GARDEN,
        END
    }
}