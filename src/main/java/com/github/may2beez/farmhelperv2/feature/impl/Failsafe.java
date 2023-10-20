package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.github.may2beez.farmhelperv2.event.BlockChangeEvent;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.AudioManager;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private static final String[] FAILSAFE_MESSAGES = new String[]{
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
        failsafeDelay.reset();
        lookAroundTimes = 0;
        currentLookAroundTimes = 0;
        resetRotationCheck();
        resetDirtCheck();
        resetEvacuateCheck();
        resetItemChangeCheck();
        resetWorldChangeCheck();
        resetBedrockCageCheck();
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
        FeatureManager.getInstance().disableCurrentlyRunning(this);
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
        if (failsafeDelay.isScheduled() && !failsafeDelay.passed()) return;

        switch (emergency) {
            case NONE:
                break;
            case TEST:
                Failsafe.getInstance().stop();
                break;
            case ROTATION_CHECK:
            case TELEPORT_CHECK:
                onRotationTeleportCheck();
                break;
            case DIRT_CHECK:
                onDirtCheck();
                break;
            case ITEM_CHANGE_CHECK:
                onItemChange();
                break;
            case WORLD_CHANGE_CHECK:
                onWorldChange();
                break;
            case BEDROCK_CAGE_CHECK:
                onBedrockCage();
                break;
            case EVACUATE:
                onEvacuate();
                break;
            case BANWAVE:
                onBanwave();
                break;
            case JACOB:
                onJacobCheck();
                break;
        }
    }

    private final Clock failsafeDelay = new Clock();
    private int lookAroundTimes = 0;
    private int currentLookAroundTimes = 0;

    // region BANWAVE

    @SubscribeEvent
    public void onTickBanwave(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;
        if (!BanInfoWS.getInstance().isBanwave()) return;
        if (!FarmHelperConfig.enableLeavePauseOnBanwave) return;
        if (FarmHelperConfig.banwaveDontLeaveDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) return;

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

    // region ROTATION and TELEPORT

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

    public void onRotationTeleportCheck() {
        if (fakeMovementCheck()) return;

        switch (rotationCheckState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                rotationCheckState = RotationCheckState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                rotationCheckState = RotationCheckState.LOOK_AROUND;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                break;
            case LOOK_AROUND:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    if (FarmHelperConfig.sendFailsafeMessage) {
                        rotationCheckState = RotationCheckState.TYPE_SHIT;
                        failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                    } else {
                        rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                        currentLookAroundTimes = 0;
                        lookAroundTimes = (int) (2 + Math.random() * 2);
                    }
                    rotation.reset();
                } else {
                    randomMoveAndRotate();
                }
                break;
            case TYPE_SHIT:
                String randomMessage;
                String customMessages = "";
                switch (emergency) {
                    case ROTATION_CHECK:
                        customMessages = CustomFailsafeMessagesPage.customRotationMessages;
                        break;
                    case TELEPORT_CHECK:
                        customMessages = CustomFailsafeMessagesPage.customTeleportationMessages;
                        break;
                }
                if (customMessages.isEmpty()) {
                    randomMessage = getRandomMessage(FAILSAFE_MESSAGES);
                } else {
                    randomMessage = getRandomMessage(customMessages.split("\\|"));
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                rotationCheckState = RotationCheckState.LOOK_AROUND_2;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (2 + Math.random() * 2);
                break;
            case LOOK_AROUND_2:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    rotationCheckState = RotationCheckState.END;
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    randomMoveAndRotate();
                }
                break;
            case END:
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                rotation.easeTo((float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30), (float) (30 + Math.random() * 20 - 10), (long) randomTime);
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
                }, (int) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void randomMoveAndRotate() {
        long rotationTime = FarmHelperConfig.getRandomRotationTime();
        rotation.easeTo(mc.thePlayer.rotationYaw + randomValueBetweenExt(-180, 180, 45), randomValueBetweenExt(-20, 40, 5), rotationTime);
        failsafeDelay.schedule(rotationTime - 50);
        currentLookAroundTimes++;
        if (!mc.thePlayer.onGround) return;
        double randomKey = Math.random();
        if (randomKey <= 0.2) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
        } else if (randomKey <= 0.4) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
        } else if (randomKey <= 0.6) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
        } else if (randomKey <= 0.8) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
            Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
        }
    }

    private float randomValueBetweenExt(float min, float max, float minFromZero) {
        double random = Math.random();
        if (random < 0.5) {
            // should return value between (min, -minFromZero)
            return (float) (min + Math.random() * (minFromZero - min));
        } else {
            // should return value between (minFromZero, max)
            return (float) (minFromZero + Math.random() * (max - minFromZero));
        }
    }

    private void resetRotationCheck() {
        rotationCheckState = RotationCheckState.NONE;
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

    // endregion

    // region DIRT check

    private final ArrayList<BlockPos> dirtBlocks = new ArrayList<>();

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (firstCheckReturn()) return;
        if (isEmergency()) return;
        if (event.update.getBlock() == null) return;
        if (event.update.getBlock().equals(Blocks.air)) return;
        if (event.update.getBlock() instanceof BlockCrops) return;
        if (event.update.getBlock() instanceof BlockNetherWart) return;
        if (event.update.getBlock() instanceof BlockCactus) return;
        if (event.update.getBlock() instanceof BlockReed) return;
        if (event.update.getBlock().isPassable(mc.theWorld, event.pos)) return;

        LogUtils.sendWarning("[Failsafe] Someone put block on your garden! Block pos: " + event.pos);
        dirtBlocks.add(event.pos);
    }

    enum DirtCheckState {
        NONE,
        WAIT_BEFORE_START,
        ROTATE_INTO_DIRT,
        LOOK_AWAY,
        TYPE_SHIT,
        LOOK_AROUND,
        ROTATE_INTO_DIRT_2,
        END
    }

    private DirtCheckState dirtCheckState = DirtCheckState.NONE;
    private final Clock dirtCheckDelay = new Clock();

    private void onDirtCheck() {
        if (fakeMovementCheck()) return;

        switch (dirtCheckState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                dirtCheckState = DirtCheckState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                dirtCheckState = DirtCheckState.ROTATE_INTO_DIRT;
                break;
            case ROTATE_INTO_DIRT:
                BlockPos closestDirt = dirtBlocks.stream().sorted(Comparator.comparingDouble(block -> mc.thePlayer.getPosition().distanceSq(block))).collect(Collectors.toList()).get(0);
                Pair<Float, Float> rotation = AngleUtils.getRotation(new Vec3(closestDirt.getX() + 0.5f + (Math.random() * 1 - 0.5), closestDirt.getY() + 0.5f + (Math.random() * 1 - 0.5), closestDirt.getZ() + 0.5f + (Math.random() * 1 - 0.5)));
                float yaw = rotation.getLeft();
                float pitch = rotation.getRight();
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(yaw, pitch, (long) randomTime);
                failsafeDelay.schedule((long) (randomTime + 250 + Math.random() * 400));
                dirtCheckState = DirtCheckState.LOOK_AWAY;
                Multithreading.schedule(() -> {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                    Multithreading.schedule(KeyBindUtils::stopMovement, (long) (350 + Math.random() * 300), TimeUnit.MILLISECONDS);
                }, (long) Math.max(300, randomTime - 200), TimeUnit.MILLISECONDS);
                break;
            case LOOK_AWAY:
                if (this.rotation.rotating) return;
                KeyBindUtils.stopMovement();
                long randomTime2 = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(mc.thePlayer.rotationYaw + 180 + randomValueBetweenExt(-30, 30, 10), randomValueBetweenExt(-30, 5, 2), randomTime2);
                if (FarmHelperConfig.sendFailsafeMessage) {
                    failsafeDelay.schedule((long) (randomTime2 + 450 + Math.random() * 500));
                    dirtCheckState = DirtCheckState.TYPE_SHIT;
                } else {
                    failsafeDelay.schedule(randomTime2);
                    dirtCheckState = DirtCheckState.LOOK_AROUND;
                }
                break;
            case TYPE_SHIT:
                if (this.rotation.rotating) return;
                String randomMessage;
                if (CustomFailsafeMessagesPage.customDirtMessages.isEmpty()) {
                    randomMessage = getRandomMessage(FAILSAFE_MESSAGES);
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customDirtMessages.split("\\|");
                    randomMessage = getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                dirtCheckState = DirtCheckState.LOOK_AROUND;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (2 + Math.random() * 2);
                break;
            case LOOK_AROUND:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    this.rotation.reset();
                    KeyBindUtils.stopMovement();
                    dirtCheckState = DirtCheckState.ROTATE_INTO_DIRT_2;
                    failsafeDelay.schedule((long) (150 + Math.random() * 150));
                } else {
                    randomMoveAndRotate();
                }
                break;
            case ROTATE_INTO_DIRT_2:
                BlockPos closestDirt2 = dirtBlocks.stream().sorted(Comparator.comparingDouble(block -> mc.thePlayer.getPosition().distanceSq(block))).collect(Collectors.toList()).get(0);
                Pair<Float, Float> rotation2 = AngleUtils.getRotation(new Vec3(closestDirt2.getX() + 0.5f + (Math.random() * 1 - 0.5), closestDirt2.getY() + 0.5f + (Math.random() * 1 - 0.5), closestDirt2.getZ() + 0.5f + (Math.random() * 1 - 0.5)));
                float yaw2 = rotation2.getLeft();
                float pitch2 = rotation2.getRight();
                long randomTime3 = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(yaw2, pitch2, randomTime3);
                failsafeDelay.schedule((long) (randomTime3 + 450 + Math.random() * 500));
                dirtCheckState = DirtCheckState.END;
                break;
            case END:
                if (this.rotation.rotating) return;
                long randomTime4 = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo((float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30), (float) (30 + Math.random() * 20 - 10), randomTime4);
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished dirt check failsafe");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, randomTime4 + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    public boolean isTouchingDirtBlock() {
        for (BlockPos dirtBlock : dirtBlocks) {
            double distance = Math.sqrt(mc.thePlayer.getPositionEyes(1).distanceTo(new Vec3(dirtBlock.getX() + 0.5, dirtBlock.getY() + 0.5, dirtBlock.getZ() + 0.5)));
            LogUtils.sendDebug(distance + " " + dirtBlock);
            if (distance <= 1.5) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDirtBlocks() {
        return !dirtBlocks.isEmpty();
    }

    private void resetDirtCheck() {
        dirtBlocks.clear();
        dirtCheckState = DirtCheckState.NONE;
    }

    // endregion

    // region EVACUATE

    enum EvacuateState {
        NONE,
        EVACUATE_FROM_ISLAND,
        TP_BACK_TO_ISLAND,
        END
    }

    private EvacuateState evacuateState = EvacuateState.NONE;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (event.type != 0) return;
        if (!FarmHelperConfig.autoEvacuateOnWorldUpdate) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (getNumberOfCharactersInString(message) > 1) return;

        if (message.contains("to warp out! CLICK to warp now!")) {
            evacuateState = EvacuateState.EVACUATE_FROM_ISLAND;
        }
    }

    private int getNumberOfCharactersInString(String string) {
        int count = 0;
        for (char c : string.toCharArray()) {
            if (c == ':') count++;
        }
        return count;
    }

    private void onEvacuate() {
        switch (evacuateState) {
            case NONE:
                MacroHandler.getInstance().pauseMacro();
                evacuateState = EvacuateState.EVACUATE_FROM_ISLAND;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                break;
            case EVACUATE_FROM_ISLAND:
                if (GameStateHandler.getInstance().inGarden()) {
                    mc.thePlayer.sendChatMessage("/evacuate");
                    failsafeDelay.schedule((long) (2_500 + Math.random() * 2_000));
                } else {
                    evacuateState = EvacuateState.TP_BACK_TO_ISLAND;
                    failsafeDelay.schedule((long) (3_000 + Math.random() * 3_000));
                }
                break;
            case TP_BACK_TO_ISLAND:
                if (GameStateHandler.getInstance().inGarden()) {
                    evacuateState = EvacuateState.END;
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    mc.thePlayer.sendChatMessage("/warp garden");
                    failsafeDelay.schedule((long) (2_500 + Math.random() * 2_000));
                }
                break;
            case END:
                Failsafe.getInstance().stop();
                MacroHandler.getInstance().resumeMacro();
                LogUtils.sendFailsafeMessage("[Failsafe] Came back from evacuation!");
                break;
        }
    }

    private void resetEvacuateCheck() {
        evacuateState = EvacuateState.NONE;
    }

    // endregion

    // region ITEM CHANGE

    enum ItemChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SWAP_BACK_ITEM,
        END
    }

    private ItemChangeState itemChangeState = ItemChangeState.NONE;

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent event) {
        if (firstCheckReturn()) return;
        if (MacroHandler.getInstance().isTeleporting()) return;

        if (!(event.packet instanceof S2FPacketSetSlot)) return;

        S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
        int slot = packet.func_149173_d();
        int farmingToolSlot = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop());
        ItemStack farmingTool = mc.thePlayer.inventory.getStackInSlot(farmingToolSlot);
        if (slot == 36 + farmingToolSlot &&
                farmingTool != null &&
                !(farmingTool.getItem() instanceof ItemHoe) &&
                !(farmingTool.getItem() instanceof ItemAxe)) {
            LogUtils.sendDebug("[Failsafe] No farming tool in hand! Slot: " + slot);
            addEmergency(EmergencyType.ITEM_CHANGE_CHECK);
        }
    }

    private void onItemChange() {
        if (fakeMovementCheck()) return;

        switch (itemChangeState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                itemChangeState = ItemChangeState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                itemChangeState = ItemChangeState.LOOK_AROUND;
                break;
            case LOOK_AROUND:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    itemChangeState = ItemChangeState.SWAP_BACK_ITEM;
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    randomMoveAndRotate();
                }
                break;
            case SWAP_BACK_ITEM:
                if (this.rotation.rotating) return;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                itemChangeState = ItemChangeState.END;
                break;
            case END:
                PlayerUtils.getTool();
                Failsafe.getInstance().stop();
                Multithreading.schedule(() -> {
                    LogUtils.sendDebug("[Failsafe] Finished item change failsafe. Continuing macro...");
                    MacroHandler.getInstance().resumeMacro();
                }, 500, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void resetItemChangeCheck() {
        itemChangeState = ItemChangeState.NONE;
    }

    // endregion

    // region WORLD CHANGE

    enum WorldChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        END
    }

    private WorldChangeState worldChangeState = WorldChangeState.NONE;

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (firstCheckReturn()) return;

        addEmergency(EmergencyType.WORLD_CHANGE_CHECK);
    }

    @SubscribeEvent
    public void onReceiveChatWhileWorldChange(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (emergency != EmergencyType.WORLD_CHANGE_CHECK) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
            LogUtils.sendWarning("[Failsafe] Can't warp to garden! Will try again in a moment.");
            failsafeDelay.schedule(10_000);
        }
    }

    private void onWorldChange() {
        if (!FarmHelperConfig.autoTPOnWorldChange) {
            LogUtils.sendDebug("[Failsafe] Auto tp on world change is disabled! Disabling macro and disconnecting");
            MacroHandler.getInstance().disableMacro();
            Multithreading.schedule(() -> {
                try {
                    mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Your world has been changed and you've got \"Auto TP On world is disabled\""));
                    AudioManager.getInstance().resetSound();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1_500, TimeUnit.MILLISECONDS);
            return;
        }

        switch (worldChangeState) {
            case NONE:
                MacroHandler.getInstance().pauseMacro();
                failsafeDelay.schedule((long) (250f + Math.random() * 500f));
                worldChangeState = WorldChangeState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                if (FarmHelperConfig.fakeMovements) {
                    lookAroundTimes = (int) Math.round(2 + Math.random() * 2);
                    currentLookAroundTimes = 0;
                    failsafeDelay.schedule((long) (500 + Math.random() * 500));
                    KeyBindUtils.stopMovement();
                    worldChangeState = WorldChangeState.LOOK_AROUND;
                } else {
                    failsafeDelay.schedule((long) (500 + Math.random() * 500));
                    worldChangeState = WorldChangeState.END;
                }
                break;
            case LOOK_AROUND:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    worldChangeState = WorldChangeState.END;
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    randomMoveAndRotate();
                }
                break;
            case END:
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING || LagDetector.getInstance().isLagging()) {
                    failsafeDelay.schedule(1_000);
                    return;
                }
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY) {
                    LogUtils.sendDebug("[Failsafe] In lobby, sending /skyblock command...");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    failsafeDelay.schedule((long) (4_500 + Math.random() * 1_000));
                    return;
                }
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
                    LogUtils.sendDebug("[Failsafe] In Limbo, sending /l command...");
                    mc.thePlayer.sendChatMessage("/l");
                    failsafeDelay.schedule((long) (4_500 + Math.random() * 1_000));
                    return;
                }
                if (GameStateHandler.getInstance().inGarden()) {
                    LogUtils.sendDebug("[Failsafe] Went back to garden. Continuing macro...");
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                    return;
                } else {
                    LogUtils.sendDebug("[Failsafe] Sending /warp garden command...");
                    mc.thePlayer.sendChatMessage("/warp garden");
                    failsafeDelay.schedule((long) (4_500 + Math.random() * 1_000));
                }
                break;
        }
    }

    private void resetWorldChangeCheck() {
        worldChangeState = WorldChangeState.NONE;
    }

    // endregion

    // region BEDROCK CAGE

    enum BedrockCageState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        TYPE_SHIT,
        LOOK_AROUND_2,
        END,
    }
    private BedrockCageState bedrockCageState = BedrockCageState.NONE;


    @SubscribeEvent
    public void onTickBedrockCageCheck(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;

        if (mc.thePlayer.getPosition().getY() < 50) return;
        if (BlockUtils.bedrockCount() <= 2) return;

        addEmergency(EmergencyType.BEDROCK_CAGE_CHECK);
    }

    private void onBedrockCage() {
        if (fakeMovementCheck()) return;

        switch (bedrockCageState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                bedrockCageState = BedrockCageState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                bedrockCageState = BedrockCageState.LOOK_AROUND;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                break;
            case LOOK_AROUND:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    if (FarmHelperConfig.sendFailsafeMessage) {
                        bedrockCageState = BedrockCageState.TYPE_SHIT;
                        failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                    } else {
                        bedrockCageState = BedrockCageState.LOOK_AROUND_2;
                        currentLookAroundTimes = 0;
                        lookAroundTimes = (int) (2 + Math.random() * 2);
                    }
                    rotation.reset();
                } else {
                    if (Math.random() < 0.2) {
                        mc.thePlayer.jump();
                    }
                    if (Math.random() < 0.3) {
                        KeyBindUtils.leftClick();
                    }
                    randomMoveAndRotate();
                }
                break;
            case TYPE_SHIT:
                String randomMessage;
                String customMessages = CustomFailsafeMessagesPage.customBedrockMessages;
                if (customMessages.isEmpty()) {
                    randomMessage = getRandomMessage(FAILSAFE_MESSAGES);
                } else {
                    randomMessage = getRandomMessage(customMessages.split("\\|"));
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                bedrockCageState = BedrockCageState.LOOK_AROUND_2;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (2 + Math.random() * 2);
                break;
            case LOOK_AROUND_2:
                if (currentLookAroundTimes >= lookAroundTimes) {
                    rotation.reset();
                    KeyBindUtils.stopMovement();
                    bedrockCageState = BedrockCageState.END;
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                } else {
                    if (Math.random() < 0.2) {
                        mc.thePlayer.jump();
                    }
                    if (Math.random() < 0.3) {
                        KeyBindUtils.leftClick();
                    }
                    randomMoveAndRotate();
                }
                break;
            case END:
                if (BlockUtils.bedrockCount() > 2) {
                    failsafeDelay.schedule(2_000);
                    randomMoveAndRotate();
                    return;
                }
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                rotation.easeTo((float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30), (float) (30 + Math.random() * 20 - 10), (long) randomTime);
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished bedrock cage failsafe");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, (int) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void resetBedrockCageCheck() {
        bedrockCageState = BedrockCageState.NONE;
    }

    // endregion

    // region JACOB

    @SubscribeEvent
    public void onTickJacobFailsafe(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;
        if (!FarmHelperConfig.enableJacobFailsafes) return;
        if (!GameStateHandler.getInstance().getJacobsContestCrop().isPresent()) return;

        int cropThreshold = Integer.MAX_VALUE;
        switch (GameStateHandler.getInstance().getJacobsContestCrop().get()) {
            case CARROT:
                cropThreshold = FarmHelperConfig.jacobCarrotCap;
                break;
            case NETHER_WART:
                cropThreshold = FarmHelperConfig.jacobNetherWartCap;
                break;
            case POTATO:
                cropThreshold = FarmHelperConfig.jacobPotatoCap;
                break;
            case WHEAT:
                cropThreshold = FarmHelperConfig.jacobWheatCap;
                break;
            case SUGAR_CANE:
                cropThreshold = FarmHelperConfig.jacobSugarCaneCap;
                break;
            case MELON:
                cropThreshold = FarmHelperConfig.jacobMelonCap;
                break;
            case PUMPKIN:
                cropThreshold = FarmHelperConfig.jacobPumpkinCap;
                break;
            case CACTUS:
                cropThreshold = FarmHelperConfig.jacobCactusCap;
                break;
            case COCOA_BEANS:
                cropThreshold = FarmHelperConfig.jacobCocoaBeansCap;
                break;
            case MUSHROOM_ROTATE:
            case MUSHROOM:
                cropThreshold = FarmHelperConfig.jacobMushroomCap;
                break;
        }

        if (GameStateHandler.getInstance().getJacobsContestCropNumber() >= cropThreshold) {
            addEmergency(EmergencyType.JACOB);
        }
    }

    private void onJacobCheck() {
        if (FarmHelperConfig.jacobFailsafeAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused macro because of extending Jacob's Content!");
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!GameStateHandler.getInstance().inJacobContest()) {
                    LogUtils.sendFailsafeMessage("[Failsafe] Resuming macro because Jacob's Contest is over!");
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of extending Jacob's Contest!");
                MacroHandler.getInstance().pauseMacro();
                Multithreading.schedule(() -> {
                    try {
                        mc.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText("Will reconnect after end of Jacob's Contest!"));
                        AudioManager.getInstance().resetSound();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 500, TimeUnit.MILLISECONDS);
            }
        }
    }

    // endregion

    private static String getRandomMessage(String[] messages) {
        if (messages.length > 1) {
            return messages[(int) (Math.random() * (messages.length - 1))];
        } else {
            return messages[0];
        }
    }

    private boolean firstCheckReturn() {
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (!MacroHandler.getInstance().isMacroToggled()) return true;
        if (isEmergency()) return true;
        return FeatureManager.getInstance().isAnyOtherFeatureEnabled(this); // we don't want to leave while serving visitors or doing other stuff
    }

    public void addEmergency(EmergencyType emergencyType) {
        if (emergencyQueue.contains(emergencyType)) return;

        MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::saveState);
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
        } else if (failsafeDelay.isScheduled()) {
            String text = "Failsafe delay: " + LogUtils.formatTime(failsafeDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        }
    }
}
