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
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TeleportFailsafe extends Failsafe {
    private static TeleportFailsafe instance;
    public static TeleportFailsafe getInstance() {
        if (instance == null) {
            instance = new TeleportFailsafe();
        }
        return instance;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.TELEPORT_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnTeleportationFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnTeleportationFailsafe;
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {

        if (MacroHandler.getInstance().isTeleporting()) return;

        if (event.packet instanceof S08PacketPlayerPosLook) {
            // Rotation or teleport
            if (LagDetector.getInstance().isLagging() || LagDetector.getInstance().wasJustLagging()) {
                LogUtils.sendWarning("[Failsafe] Got rotation packet while lagging! Ignoring that one.");
                return;
            }

            S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
            Vec3 currentPlayerPos = mc.thePlayer.getPositionVector();
            Vec3 packetPlayerPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

            // Teleport Check

            double distance = currentPlayerPos.distanceTo(packetPlayerPos);
            if (packet.getY() >= 80 || BlockUtils.bedrockCount() > 2) {
                LogUtils.sendDebug("[Failsafe] Most likely a bedrock check! Will check in a moment to be sure.");
                return;
            }

            if (distance >= FarmHelperConfig.teleportCheckSensitivity || (MacroHandler.getInstance().getCurrentMacro().isPresent() && Math.abs(packet.getY()) - Math.abs(MacroHandler.getInstance().getCurrentMacro().get().getLayerY()) > 0.8)) {
                LogUtils.sendDebug("[Failsafe] Teleport detected! Distance: " + distance);
                final double lastRecievedPacketDistance = currentPlayerPos.distanceTo(LagDetector.getInstance().getLastPacketPosition());
                // blocks per tick
                final double playerMovementSpeed = mc.thePlayer.getAttributeMap().getAttributeInstanceByName("generic.movementSpeed").getAttributeValue();
                final int ticksSinceLastPacket = (int) Math.ceil(LagDetector.getInstance().getTimeSinceLastTick() / 50D);
                final double estimatedMovement = playerMovementSpeed * ticksSinceLastPacket;
                if (lastRecievedPacketDistance > 7.5D && Math.abs(lastRecievedPacketDistance - estimatedMovement) < FarmHelperConfig.teleportCheckLagSensitivity)
                    return;
                FailsafeManager.getInstance().possibleDetection(this);
            }
        }
    }

    @Override
    public void duringFailsafeTrigger() {
        switch (teleportCheckState) {
            case NONE:
                positionBeforeReacting = mc.thePlayer.getPosition();
                teleportCheckState = TeleportCheckState.WAIT_BEFORE_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                MovRecPlayer.setYawDifference(AngleUtils.getClosest(rotationBeforeReacting.getYaw()));
                positionBeforeReacting = mc.thePlayer.getPosition();
                teleportCheckState = TeleportCheckState.LOOK_AROUND;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 500);
                break;
            case LOOK_AROUND:
                MovRecPlayer.getInstance().playRandomRecording("TELEPORT_CHECK_Start");
                teleportCheckState = TeleportCheckState.SEND_MESSAGE;
                FailsafeManager.getInstance().scheduleRandomDelay(2000, 3000);
                break;
            case SEND_MESSAGE:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                String randomMessage;
                if (CustomFailsafeMessagesPage.customTeleportationMessages.isEmpty()) {
                    randomMessage = FailsafeManager.getRandomMessage();
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customTeleportationMessages.split("\\|");
                    randomMessage = FailsafeManager.getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                teleportCheckState = TeleportCheckState.LOOK_AROUND_2;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case LOOK_AROUND_2:
                if (Math.random() < 0.2) {
                    teleportCheckState = TeleportCheckState.SEND_MESSAGE_2;
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
                teleportCheckState = TeleportCheckState.GO_BACK_START;
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
            case GO_BACK_START:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) < 2) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE_2;
                    break;
                }
                BaritoneHandler.walkToBlockPos(positionBeforeReacting);
                teleportCheckState = TeleportCheckState.GO_BACK_END;
                break;
            case GO_BACK_END:
                if (BaritoneHandler.hasFailed() || BaritoneHandler.isWalkingToGoalBlock()) {
                    teleportCheckState = TeleportCheckState.ROTATE_TO_POS_BEFORE_2;
                    FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                    break;
                }
                LogUtils.sendDebug("Distance difference: " + mc.thePlayer.getPosition().distanceSq(positionBeforeReacting));
                FailsafeManager.getInstance().scheduleDelay(200);
                break;
            case ROTATE_TO_POS_BEFORE_2:
                if (rotation.isRotating()) break;
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        500, null));
                this.endOfFailsafeTrigger();
                FailsafeManager.getInstance().scheduleRandomDelay(500, 1000);
                break;
        }
    }

    @Override
    public void endOfFailsafeTrigger() {
        teleportCheckState = TeleportCheckState.NONE;
        positionBeforeReacting = null;
        rotationBeforeReacting = null;
        FailsafeManager.getInstance().stopFailsafes();
        if (mc.thePlayer.getPosition().getY() < 100 && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN)
            MacroHandler.getInstance().resumeMacro();
    }

    @SubscribeEvent
    public void onWorldUnloadDetection(WorldEvent.Unload event) {
        endOfFailsafeTrigger();
    }


    private TeleportCheckState teleportCheckState = TeleportCheckState.NONE;
    private final RotationHandler rotation = RotationHandler.getInstance();
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;

    enum TeleportCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SEND_MESSAGE,
        LOOK_AROUND_2,
        SEND_MESSAGE_2,
        ROTATE_TO_POS_BEFORE_2,
        GO_BACK_START,
        GO_BACK_END,
    }
}