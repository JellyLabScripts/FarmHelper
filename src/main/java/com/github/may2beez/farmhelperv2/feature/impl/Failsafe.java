package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.AudioManager;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.TickTask;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Getter
public class Failsafe implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RotationUtils rotation = new RotationUtils();

    private static Failsafe instance;

    public static Failsafe getInstance() {
        if (instance == null) {
            instance = new Failsafe();
        }
        return instance;
    }

    public enum EmergencyType {
        NONE,
        TEST("This is a test emergency!", 100),
        ROTATION_CHECK("You've got§l ROTATED§r§d by staff member!", 4),
        TELEPORT_CHECK("You've got§l TELEPORTED§r§d by staff member!", 5),
        DIRT_CHECK("You've got§l DIRT CHECKED§r§d by staff member!", 3),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!", 3),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!", 2),
        BEDROCK_CAGE_CHECK("You've got§l BEDROCK CAGED§r§d by staff member!", 1),
        EVACUATE("Server is restarting! Evacuate!", 1),
        BANWAVE("Banwave has been detected!", 6),
        JACOB("You've extended the §lJACOB COUNTER§r§d!", 7);

        final String label;
        // 1 is highest priority
        final int priority;

        EmergencyType(String s, int priority) {
            label = s;
            this.priority = priority;
        }

        EmergencyType() {
            label = name();
            priority = 999;
        }
    }

    private EmergencyType emergency = EmergencyType.NONE;
    private final ArrayList<EmergencyType> emergencyQueue = new ArrayList<>();

    private static final String[] FAILSAFE_MESSAGES = new String[] {
            "WHAT", "what?", "what", "what??", "what???", "wut?", "?", "what???", "yo huh", "yo huh?", "yo?",
            "ehhhhh??", "eh", "yo", "ahmm", "ehh", "LOL what", "lol :skull:", "bro wtf was that?", "lmao",
            "lmfao", "wtf is this", "wtf", "WTF", "wtf is this?", "wtf???", "tf", "tf?", "wth",
            "lmao what?", "????", "??", "???????", "???", "UMMM???", "umm", "ummm???", "damn wth",
            "Damn", "damn wtf", "damn", "hmmm", "hm", "sus", "hmm", "ok??", "ok?", "give me a rest",
            "again lol", "again??", "ok damn", "seriously?", "seriously????", "seriously", "really?", "really",
            "are you kidding me?", "are you serious?", "are you fr???", "not again",
            "give me a break", "youre kidding right?", "youre joking", "youre kidding me",
            "you must be joking", "seriously bro?", "cmon now", "cmon", "this is too much", "stop messing with me"};

    @Override
    public String getName() {
        return "Failsafe";
    }

    @Override
    public boolean isRunning() {
        return isEmergency();
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        emergency = EmergencyType.NONE;
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        AudioManager.getInstance().resetSound();
        resetRotationStates();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        stop();
        restartMacroAfterFailsafeDelay.reset();
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    public boolean isEmergency() {
        return emergency != EmergencyType.NONE;
    }

    private final Clock chooseEmergencyDelay = new Clock();
    private final Clock restartMacroAfterFailsafeDelay = new Clock();

    @Setter
    private boolean hadEmergency = false;

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (isEmergency()) return;
        if (emergencyQueue.isEmpty()) return;
        if (chooseEmergencyDelay.isScheduled() && !chooseEmergencyDelay.passed()) return;
        EmergencyType tempEmergency = getHighestPriorityEmergency();
        if (tempEmergency == EmergencyType.NONE) {
            // Should never happen, but yeh...
            LogUtils.sendDebug("[Failsafe] No emergency chosen!");
            stop();
            return;
        }

        AudioManager.getInstance().playSound();
        emergency = tempEmergency;
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        hadEmergency = true;
        LogUtils.sendDebug("[Failsafe] Emergency chosen: " + StringUtils.stripControlCodes(emergency.label));
        LogUtils.sendFailsafeMessage(emergency.label);
        FailsafeUtils.getInstance().sendNotification(StringUtils.stripControlCodes(emergency.label), TrayIcon.MessageType.WARNING);
    }

    @SubscribeEvent
    public void onTickRestartMacro(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!restartMacroAfterFailsafeDelay.isScheduled()) return;

        if (restartMacroAfterFailsafeDelay.passed()) {
            LogUtils.sendDebug("[Failsafe] Restarting macro...");
            MacroHandler.getInstance().enableMacro();
            Failsafe.getInstance().setHadEmergency(false);
            Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().reset();
        }
    }

    @SubscribeEvent
    public void onTickFailsafe(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!isEmergency()) return;

        switch (emergency) {
            case NONE:
                break;
            case TEST:
                Failsafe.getInstance().stop();
                break;
            case ROTATION_CHECK:
                onRotationCheck();
                break;
            case TELEPORT_CHECK:
                onTeleportCheck();
                break;
            case DIRT_CHECK:
                break;
            case ITEM_CHANGE_CHECK:
                break;
            case WORLD_CHANGE_CHECK:
                break;
            case BEDROCK_CAGE_CHECK:
                break;
            case EVACUATE:
                break;
            case BANWAVE:
                onBanwave();
                break;
            case JACOB:
                break;
        }
    }

    // region BANWAVE

    @SubscribeEvent
    public void onTickBanwave(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;
        if (!BanInfoWS.getInstance().isBanwave()) return;
        if (!FarmHelperConfig.enableLavePauseOnBanwave) return;

        addEmergency(EmergencyType.BANWAVE);
    }

    private void onBanwave() {
        if (FarmHelperConfig.banwaveAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused macro because of banwave!");
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!BanInfoWS.getInstance().isBanwave()) {
                    LogUtils.sendFailsafeMessage("[Failsafe] Resuming macro because banwave is over!");
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of banwave!");
                MacroHandler.getInstance().pauseMacro();
                Multithreading.schedule(() -> {
                    try {
                        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Will reconnect after end of banwave!"));
                        AudioManager.getInstance().resetSound();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 500, TimeUnit.MILLISECONDS);
            }
        }
    }

    // endregion

    // region ROTATION and TELEPORT check

    @SubscribeEvent
    public void onPacketReceived(ReceivePacketEvent event) {
        if (firstCheckReturn()) return;
        if (MacroHandler.getInstance().isTeleporting()) return;

        if (event.packet instanceof S08PacketPlayerPosLook) {
            // Rotation or teleport
            if (LagDetector.getInstance().isLagging()) {
                LogUtils.sendWarning("[Failsafe] Got rotation packet while lagging! Ignoring that one.");
                return;
            }

            S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
            Vec3 currentPlayerPos = mc.thePlayer.getPositionVector();
            Vec3 packetPlayerPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());

            // Teleport Check

            double distance = currentPlayerPos.distanceTo(packetPlayerPos);
            if (packet.getY() >= 80) {
                LogUtils.sendDebug("[Failsafe] Most likely a bedrock check! Will check in a moment to be sure.");
                return;
            }

            if (distance >= FarmHelperConfig.teleportCheckSensitivity) {
                LogUtils.sendDebug("[Failsafe] Teleport detected! Distance: " + distance);
                addEmergency(EmergencyType.TELEPORT_CHECK);
                return;
            }

            // Rotation check

            double packetYaw = packet.getYaw();
            double packetPitch = packet.getPitch();
            double playerYaw = mc.thePlayer.rotationYaw;
            double playerPitch = mc.thePlayer.rotationPitch;
            double yawDiff = Math.abs(packetYaw - playerYaw);
            double pitchDiff = Math.abs(packetPitch - playerPitch);
            double threshold = FarmHelperConfig.rotationCheckSensitivity;
            if (yawDiff >= threshold || pitchDiff >= threshold) {
                LogUtils.sendDebug("[Failsafe] Rotation detected! Yaw diff: " + yawDiff + ", Pitch diff: " + pitchDiff);
                addEmergency(EmergencyType.ROTATION_CHECK);
            }
        }
    }

    enum RotationCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        TYPE_SHIT,
        LOOK_AROUND_2,
        END,
    }

    private RotationCheckState rotationCheckState = RotationCheckState.NONE;
    private final Clock rotationCheckDelay = new Clock();
    private int lookAroundTimes = 0;
    private int currentLookAroundTimes = 0;

    public void onRotationCheck() {
        if (fakeMovementCheck()) return;
        if (rotationCheckDelay.isScheduled() && !rotationCheckDelay.passed()) return;

        switch (rotationCheckState) {
            case NONE:
                rotationCheckDelay.schedule((long) (500f + Math.random() * 1_000f));
                rotationCheckState = RotationCheckState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(2 + Math.random() * 2);
                currentLookAroundTimes = 0;
                rotationCheckState = RotationCheckState.LOOK_AROUND;
                rotationCheckDelay.schedule((long) (500 + Math.random() * 1_000));
                break;
            case LOOK_AROUND:
                if (rotation.rotating) return;
                if (currentLookAroundTimes >= lookAroundTimes) {
                    KeyBindUtils.stopMovement();
                    if (FarmHelperConfig.sendFailsafeMessage) {
                        rotationCheckState = RotationCheckState.TYPE_SHIT;
                    } else {
                        rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                    }
                    rotationCheckDelay.schedule((long) (500 + Math.random() * 1_000));
                    rotation.reset();
                } else {
                    randomMoveAndRotate();
                }
                break;
            case TYPE_SHIT:
                String randomMessage;
                if (CustomFailsafeMessagesPage.customRotationMessages.isEmpty()) {
                    randomMessage = getRandomMessage(FAILSAFE_MESSAGES);
                } else {
                    String[] messages = CustomFailsafeMessagesPage.customRotationMessages.split("\\|");
                    randomMessage = getRandomMessage(messages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                rotationCheckDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (1 + Math.random() * 1);
                break;
            case LOOK_AROUND_2:
                if (rotation.rotating) return;
                if (currentLookAroundTimes >= lookAroundTimes) {
                    KeyBindUtils.stopMovement();
                    rotationCheckState = RotationCheckState.END;
                    rotationCheckDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    randomMoveAndRotate();
                }
                break;
            case END:
                int randomTime = (int) (350 + Math.random() * 200);
                rotation.easeTo((float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30), (float) (30 + Math.random() * 20 - 10), randomTime);
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished rotation failsafe");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void randomMoveAndRotate() {
        KeyBindUtils.stopMovement();
        long rotationTime = getRandomRotationDelay();
        rotation.easeTo((float) (mc.thePlayer.rotationYaw + (Math.random() * 160 - 80)), (float) Math.random() * 60 - 30, rotationTime);
        rotationCheckDelay.schedule((long) (rotationTime + 150 + Math.random() * 300));
        currentLookAroundTimes++;
        double randomKey = Math.random();
        if (randomKey <= 0.25) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false), (long) (150 + Math.random() * 300), TimeUnit.MILLISECONDS);
        } else if (randomKey <= 0.5) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false), (long) (150 + Math.random() * 300), TimeUnit.MILLISECONDS);
        } else if (randomKey <= 0.75) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false), (long) (150 + Math.random() * 300), TimeUnit.MILLISECONDS);
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false), (long) (150 + Math.random() * 100), TimeUnit.MILLISECONDS);
        }
    }

    private void resetRotationStates() {
        rotationCheckState = RotationCheckState.NONE;
        rotationCheckDelay.reset();
        lookAroundTimes = 0;
        currentLookAroundTimes = 0;
    }

    public void onTeleportCheck() {
        if (fakeMovementCheck()) return;
    }

    private boolean fakeMovementCheck() {
        if (!FarmHelperConfig.fakeMovements) {
            LogUtils.sendDebug("[Failsafe] Fake movement is disabled! Disabling macro.");
            if (FarmHelperConfig.enableRestartAfterFailSafe) {
                MacroHandler.getInstance().pauseMacro();
                LogUtils.sendDebug("[Failsafe] Restarting macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
            } else {
                MacroHandler.getInstance().disableMacro();
            }
            Failsafe.getInstance().stop();
            return true;
        }
        return false;
    }

    private static String getRandomMessage(String[] messages) {
        if (messages.length > 1) {
            return messages[(int) (Math.random() * (messages.length - 1))];
        } else {
            return messages[0];
        }
    }

    private static long getRandomRotationDelay() {
        return (long) ((Math.random() * FarmHelperConfig.rotationTimeRandomness + FarmHelperConfig.rotationTime));
    }

    // endregion

    private boolean firstCheckReturn() {
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (!MacroHandler.getInstance().isMacroToggled()) return true;
        if (isEmergency()) return true;
        if (chooseEmergencyDelay.isScheduled()) return true;
        return FeatureManager.getInstance().isAnyOtherFeatureEnabled(this); // we don't want to leave while serving visitors or doing other stuff
    }

    public void addEmergency(EmergencyType emergencyType) {
        emergencyQueue.add(emergencyType);
        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(1_000);
        LogUtils.sendDebug("[Failsafe] Emergency added: " + emergencyType.name());
        LogUtils.sendWarning("[Failsafe] Probability of emergency: " + LogUtils.capitalize(emergencyType.name()));
    }

    private EmergencyType getHighestPriorityEmergency() {
        EmergencyType highestPriority = EmergencyType.NONE;
        for (EmergencyType emergencyType : emergencyQueue) {
            if (emergencyType.priority < highestPriority.priority) {
                highestPriority = emergencyType;
            }
        }
        return highestPriority;
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        if (chooseEmergencyDelay.isScheduled()) {
            String text = "Failsafe in: " + LogUtils.formatTime(chooseEmergencyDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        } else if (restartMacroAfterFailsafeDelay.isScheduled()) {
            String text = "Restarting macro in: " + LogUtils.formatTime(restartMacroAfterFailsafeDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        }
    }
}
