package com.jelly.farmhelperv2.feature.impl;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.page.CustomFailsafeMessagesPage;
import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.BlockChangeEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*
    Credits to Yuro for this superb inspiration to make this class
*/
@Getter
public class Failsafe implements IFeature {

    // region BOOLEANS, LISTS, ETC
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
    private static Failsafe instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final ArrayList<EmergencyType> emergencyQueue = new ArrayList<>();
    private final Clock chooseEmergencyDelay = new Clock();
    private final Clock restartMacroAfterFailsafeDelay = new Clock();
    private final Clock failsafeDelay = new Clock();
    private final ArrayList<BlockPos> dirtBlocks = new ArrayList<>();
    private final Clock dirtCheckDelay = new Clock();
    private EmergencyType emergency = EmergencyType.NONE;
    @Setter
    private boolean hadEmergency = false;
    private int lookAroundTimes = 0;
    private int currentLookAroundTimes = 0;
    private RotationCheckState rotationCheckState = RotationCheckState.NONE;
    private DirtCheckState dirtCheckState = DirtCheckState.NONE;
    private EvacuateState evacuateState = EvacuateState.NONE;
    private ItemChangeState itemChangeState = ItemChangeState.NONE;
    private WorldChangeState worldChangeState = WorldChangeState.NONE;
    private BedrockCageState bedrockCageState = BedrockCageState.NONE;
    private CustomMovementState recordingCheckState = CustomMovementState.NONE;
    private int playingState = 0;
    @Setter @Getter
    private int reactionType = 0; // 0 = not checked yet, 1 = custom recording, 2 = built-in recording, 3 = legacy
    private boolean recordingFile = false;
    private boolean recordingFile2 = false;
    private boolean sendingFailsafeInfo = false;
    private final CircularFifoQueue<Float> bpsQueue = new CircularFifoQueue<>(20);
    private long lastTimeCheckedBPS = System.currentTimeMillis();
    private LowerBPSState lowerBPSState = LowerBPSState.NONE;
    private final Clock tabListCheckDelay = new Clock();
    private boolean wasGuestOnGarden = false;
    private String lastGuestName = "";
    private BlockPos positionBeforeReacting = null;
    private Rotation rotationBeforeReacting = null;
    private boolean pathing = false;
    private int flyingRecordingNumber = 1;
    private static final int maxFlyingRecordingNumber = 3;
    private boolean dirtOnLeft = false;
    private int minimumReactions = 3;
    // endregion

    // region MAIN

    public static Failsafe getInstance() {
        if (instance == null) {
            instance = new Failsafe();
        }
        return instance;
    }

    private static String getRandomMessage(String[] messages) {
        if (messages.length > 1) {
            return messages[(int) (Math.random() * (messages.length - 1))];
        } else {
            return messages[0];
        }
    }

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
        AudioManager.getInstance().resetSound();
        emergency = EmergencyType.NONE;
        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        failsafeDelay.reset();
        lookAroundTimes = 0;
        currentLookAroundTimes = 0;
        reactionType = 0;
        resetCustomMovement();
        resetRotationCheck();
        resetDirtCheck();
        resetEvacuateCheck();
        resetItemChangeCheck();
        resetWorldChangeCheck();
        resetBedrockCageCheck();
        resetLowerBPS();
        resetGuestVisit();
        sendingFailsafeInfo = false;
        positionBeforeReacting = null;
        rotationBeforeReacting = null;
        flyingRecordingNumber = 1;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        stop();
        restartMacroAfterFailsafeDelay.reset();
        if (UngrabMouse.getInstance().isRunning())
            UngrabMouse.getInstance().regrabMouse(true);
        else {
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
        }
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return !isEmergency();
    }

    public boolean isEmergency() {
        return emergency != EmergencyType.NONE;
    }

    @SubscribeEvent
    public void onTickChooseEmergency(TickEvent.ClientTickEvent event) {
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
        emergency = tempEmergency;

        emergencyQueue.clear();
        chooseEmergencyDelay.reset();
        hadEmergency = true;
        checkReactionType();
        LogUtils.sendDebug("[Failsafe] Reaction Type: " + reactionType);
        LogUtils.sendDebug("[Failsafe] Emergency chosen: " + StringUtils.stripControlCodes(emergency.name()));
        FeatureManager.getInstance().disableCurrentlyRunning(this);
        if (!Scheduler.getInstance().isFarming()) {
            Scheduler.getInstance().farmingTime();
        }
    }

    @SubscribeEvent
    public void onTickRestartMacro(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!restartMacroAfterFailsafeDelay.isScheduled()) return;

        if (restartMacroAfterFailsafeDelay.passed()) {
            if (mc.currentScreen != null) {
                PlayerUtils.closeScreen();
            }
            if (FarmHelperConfig.alwaysTeleportToGarden) {
                MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::triggerWarpGarden);
            }
            restartMacroAfterFailsafeDelay.reset();
            Multithreading.schedule(() -> {
                LogUtils.sendDebug("[Failsafe] Restarting the macro...");
                MacroHandler.getInstance().enableMacro();
                Failsafe.getInstance().setHadEmergency(false);
                Failsafe.getInstance().getRestartMacroAfterFailsafeDelay().reset();
            }, FarmHelperConfig.alwaysTeleportToGarden ? 1_500 : 0, TimeUnit.MILLISECONDS);
        }
    }

    public void addEmergency(EmergencyType emergencyType) {
        if (emergencyQueue.contains(emergencyType)) return;

        MacroHandler.getInstance().getCurrentMacro().ifPresent(AbstractMacro::saveState);
        emergencyQueue.add(emergencyType);
        if (!chooseEmergencyDelay.isScheduled())
            chooseEmergencyDelay.schedule(FarmHelperConfig.failsafeStopDelay + (long) (Math.random() * 500));
        LogUtils.sendDebug("[Failsafe] Emergency added: " + emergencyType.name());
        LogUtils.sendWarning("[Failsafe] Probability of emergency: " + LogUtils.capitalize(emergencyType.name()));
        if (!sendingFailsafeInfo) {
            sendingFailsafeInfo = true;
            Multithreading.schedule(() -> {
                EmergencyType tempEmergency = getHighestPriorityEmergency();
                if (tempEmergency == EmergencyType.NONE) {
                    // Should never happen, but yeh...
                    LogUtils.sendDebug("[Failsafe] No emergency chosen!");
                    stop();
                    return;
                }

                if (FarmHelperConfig.enableFailsafeSound && shouldPlaySoundAlert(tempEmergency)) {
                    AudioManager.getInstance().playSound();
                }

                if (FarmHelperConfig.autoAltTab && shouldAltTab(tempEmergency)) {
                    FailsafeUtils.bringWindowToFront();
                }
                Multithreading.schedule(() -> {
                    if (FarmHelperConfig.autoAltTab && shouldAltTab(tempEmergency) && !Display.isActive()) {
                        FailsafeUtils.bringWindowToFrontUsingRobot();
                        System.out.println("Bringing window to front using Robot because Winapi failed as usual.");
                    }
                }, 750, TimeUnit.MILLISECONDS);

                LogUtils.sendFailsafeMessage(tempEmergency.label, shouldTagEveryone(tempEmergency));
                if (shouldNotify(tempEmergency))
                    FailsafeUtils.getInstance().sendNotification(StringUtils.stripControlCodes(tempEmergency.label), TrayIcon.MessageType.WARNING);
            }, 800, TimeUnit.MILLISECONDS);
        }
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
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        if (chooseEmergencyDelay.isScheduled()) {
            String text = "Failsafe in: " + LogUtils.formatTime(chooseEmergencyDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        } else if (restartMacroAfterFailsafeDelay.isScheduled()) {
            String text = "Restarting the macro in: " + LogUtils.formatTime(restartMacroAfterFailsafeDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        } else if (failsafeDelay.isScheduled()) {
            String text = "Failsafe delay: " + LogUtils.formatTime(failsafeDelay.getRemainingTime());
            RenderUtils.drawCenterTopText(text, event, Color.MAGENTA);
        }
    }

    @SubscribeEvent
    public void onTickFailsafe(TickEvent.ClientTickEvent event) {
        if ((mc.thePlayer == null || mc.theWorld == null) && emergency != EmergencyType.DISCONNECT) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!isEmergency()) return;
        if (failsafeDelay.isScheduled() && !failsafeDelay.passed()) return;

        if (reactionType == 1) {
            onCustomFailsafeReaction(emergency.name());
            return;
        }

        switch (emergency) {
            case NONE:
                break;
            case ROTATION_CHECK:
            case TELEPORT_CHECK:
                onRotationTeleportCheck(emergency.name());
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
            case DISCONNECT:
                onDisconnect();
                break;
            case LOWER_AVERAGE_BPS:
                onLowerBPS();
                break;
            case GUEST_VISIT:
                onGuestVisit();
                break;
        }
    }

    // endregion

    // region TRIGGERS

    @SubscribeEvent
    public void onTickBanwave(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;
        if (!BanInfoWS.getInstance().isBanwave()) return;
        if (!FarmHelperConfig.banwaveCheckerEnabled) return;
        if (!FarmHelperConfig.enableLeavePauseOnBanwave) return;
        if (FarmHelperConfig.banwaveDontLeaveDuringJacobsContest && GameStateHandler.getInstance().inJacobContest())
            return;

        addEmergency(EmergencyType.BANWAVE);
    }

    @SubscribeEvent
    public void onPacketReceived(ReceivePacketEvent event) {
        if (firstCheckReturn()) return;
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
            if (yawDiff == 360 && pitchDiff == 0) // prevents false checks
                return;
            if (yawDiff >= threshold || pitchDiff >= threshold) {
                LogUtils.sendDebug("[Failsafe] Rotation detected! Yaw diff: " + yawDiff + ", Pitch diff: " + pitchDiff);
                addEmergency(EmergencyType.ROTATION_CHECK);
            }
        }
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (firstCheckReturn()) return;
        if (isEmergency()) return;
        if (event.update.getBlock() == null) return;
        if (!event.update.getBlock().equals(Blocks.dirt)) return;

        LogUtils.sendWarning("[Failsafe] Someone put a block on your garden! Block pos: " + event.pos);
        dirtBlocks.add(event.pos);
    }

    @SubscribeEvent
    public void onTickCheckScoreboard(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!FarmHelperConfig.autoEvacuateOnWorldUpdate) return;
        if (evacuateState != EvacuateState.NONE) return;

        GameStateHandler.getInstance().getServerClosingSeconds().ifPresent(seconds -> {
            if (seconds < 30) {
                addEmergency(EmergencyType.EVACUATE);
            }
        });
    }

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent event) {
        if (firstCheckReturn()) return;
        if (MacroHandler.getInstance().isTeleporting()) return;

        if (!(event.packet instanceof S2FPacketSetSlot)) return;

        S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
        int slot = packet.func_149173_d();
        int farmingToolSlot = -1;
        try {
            farmingToolSlot = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (farmingToolSlot == -1) return;
        ItemStack farmingTool = mc.thePlayer.inventory.getStackInSlot(farmingToolSlot);
        if (slot == 36 + farmingToolSlot &&
                farmingTool != null &&
                !(farmingTool.getItem() instanceof ItemHoe) &&
                !(farmingTool.getItem() instanceof ItemAxe)) {
            LogUtils.sendDebug("[Failsafe] No farming tool in hand! Slot: " + slot);
            addEmergency(EmergencyType.ITEM_CHANGE_CHECK);
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (firstCheckReturn()) return;

        addEmergency(EmergencyType.WORLD_CHANGE_CHECK);
    }

    @SubscribeEvent
    public void onTickCheckLimbo(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (emergency == EmergencyType.WORLD_CHANGE_CHECK) return;

        if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
            addEmergency(EmergencyType.WORLD_CHANGE_CHECK);
        }
    }

    @SubscribeEvent
    public void onChatMessageCheckLimbo(ClientChatReceivedEvent event) {
        if (firstCheckReturn()) return;
        if (emergency != EmergencyType.WORLD_CHANGE_CHECK) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("You were spawned in Limbo.") || message.contains("/limbo") || message.startsWith("A kick occurred in your connection")) {
            LogUtils.sendWarning("[Failsafe] Got kicked to Limbo!");
            addEmergency(EmergencyType.WORLD_CHANGE_CHECK);
        }
    }

    @SubscribeEvent
    public void onReceiveChatWhileWorldChange(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (emergency != EmergencyType.WORLD_CHANGE_CHECK) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
            LogUtils.sendWarning("[Failsafe] Can't warp to the garden! Will try again in a moment.");
            failsafeDelay.schedule(10_000);
        }
    }

    @SubscribeEvent
    public void onTickBedrockCageCheck(TickEvent.ClientTickEvent event) {
        if (firstCheckReturn()) return;

        if (mc.thePlayer.getPosition().getY() < 50) return;
        if (BlockUtils.bedrockCount() <= 2) return;

        addEmergency(EmergencyType.BEDROCK_CAGE_CHECK);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (firstCheckReturn()) return;
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (emergency != EmergencyType.NONE) return;
        if (BanInfoWS.getInstance().isBanwave() && FarmHelperConfig.enableLeavePauseOnBanwave && !FarmHelperConfig.banwaveAction)
            return;

        addEmergency(EmergencyType.DISCONNECT);
    }

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

    @SubscribeEvent
    public void onTickCheckAverageBPS(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().isCurrentMacroPaused()) {
            bpsQueue.clear();
            lastTimeCheckedBPS = System.currentTimeMillis();
            return;
        }
        if (firstCheckReturn()) return;
        if (!FarmHelperConfig.averageBPSDropCheck) return;
        if (isEmergency()) return;

        if (System.currentTimeMillis() - lastTimeCheckedBPS < 1_000) return;
        lastTimeCheckedBPS = System.currentTimeMillis();
        float bps = ProfitCalculator.getInstance().getBPSFloat();
        bpsQueue.add(bps);
        if (!bpsQueue.isAtFullCapacity()) return;
        float averageBPS = getAverageBPS();
        if (averageBPS > bps) {
            float percentage = (averageBPS - bps) / averageBPS * 100;
            if (percentage > FarmHelperConfig.averageBPSDrop) {
                addEmergency(EmergencyType.LOWER_AVERAGE_BPS);
            }
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (firstCheckReturn()) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains(":")) return;
        if (message.contains("is visiting Your Garden") && (!GameStateHandler.getInstance().isGuestOnGarden()) && !wasGuestOnGarden) {
            lastGuestName = message.replace("[SkyBlock] ", "").replace(" is visiting Your Garden!", "");
            wasGuestOnGarden = true;
            tabListCheckDelay.schedule(5000L);
            addEmergency(EmergencyType.GUEST_VISIT);
            if (!FarmHelperConfig.pauseWhenGuestArrives)
                Multithreading.schedule(() -> {
                    if (emergency == EmergencyType.GUEST_VISIT) {
                        Failsafe.getInstance().stop();
                        MacroHandler.getInstance().resumeMacro();
                    }
                }, chooseEmergencyDelay.getRemainingTime() + 100L, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onTickCheckGuests(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isEmergency()) return;
        if (tabListCheckDelay.isScheduled() && !tabListCheckDelay.passed()) return;
        if (!GameStateHandler.getInstance().isGuestOnGarden()
                && GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.GARDEN
                && wasGuestOnGarden) {
            LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because guest visit is over!", false);
            Failsafe.getInstance().stop();
            MacroHandler.getInstance().resumeMacro();
        }
    }

    // endregion

    // region REACTIONS

    private void onRotationTeleportCheck(String emergencyName) {
        if (fakeMovementCheck()) return;

        switch (recordingCheckState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                recordingCheckState = CustomMovementState.WAIT_BEFORE_START;
                playingState = 0;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                recordingCheckState = CustomMovementState.LOOK_AROUND;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                break;
            case LOOK_AROUND:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                switch (playingState) {
                    case 0: // play recording
                        MovRecPlayer.getInstance().playRandomRecording(emergencyName + "_PreChat_", true);
                        failsafeDelay.schedule((long) (2000 + Math.random() * 1_000));
                        playingState = 1;
                        break;
                    case 1: // this will be executed once it finishes playing
                        if (FarmHelperConfig.sendFailsafeMessage) {
                            recordingCheckState = CustomMovementState.TYPE_SHIT;
                            failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                        } else {
                            recordingCheckState = CustomMovementState.LOOK_AROUND_2;
                        }
                        playingState = 2;
                        break;
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
                recordingCheckState = CustomMovementState.LOOK_AROUND_2;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (2 + Math.random() * 2);
                break;
            case LOOK_AROUND_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                switch (playingState) {
                    case 2: // play another recording
                        MovRecPlayer.getInstance().playRandomRecording(emergencyName + "_PostChat_", true);
                        failsafeDelay.schedule((long) (2000 + Math.random() * 1_000));
                        playingState = 3;
                        break;
                    case 3: // this will be executed once it finishes playing
                        recordingCheckState = CustomMovementState.END;
                        failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                        playingState = 0;
                        break;
                }
                break;
            case END:
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(
                                        (float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30),
                                        (float) (30 + Math.random() * 20 - 10))
                                , (long) randomTime, null));
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished playing custom movement recording");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, (long) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void onBanwave() {
        if (FarmHelperConfig.banwaveAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of banwave!", false);
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!BanInfoWS.getInstance().isBanwave()) {
                    LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because banwave is over!", false);
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of banwave!", false);
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

    public void onRotationTeleportCheckLegacy() {
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
                this.rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(
                                        (float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30),
                                        (float) (30 + Math.random() * 20 - 10))
                                , (long) randomTime, null));
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                if (emergency == EmergencyType.TELEPORT_CHECK) {
                    Multithreading.schedule(() -> {
                        InventoryUtils.openInventory();
                        Multithreading.schedule(() -> {
                            if (mc.currentScreen != null)
                                PlayerUtils.closeScreen();
                            MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                            Multithreading.schedule(() -> {
                                InventoryUtils.openInventory();
                                LogUtils.sendDebug("[Failsafe] Finished rotation failsafe");
                                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                                    LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                                    restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                                }
                            }, (long) (1_500 + Math.random() * 1_000), TimeUnit.MILLISECONDS);
                        }, (long) (3_500 + Math.random() * 2_000), TimeUnit.MILLISECONDS);
                    }, (int) randomTime + 250, TimeUnit.MILLISECONDS);
                } else {
                    Multithreading.schedule(() -> {
                        InventoryUtils.openInventory();
                        LogUtils.sendDebug("[Failsafe] Finished rotation failsafe");
                        if (FarmHelperConfig.enableRestartAfterFailSafe) {
                            LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                            restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                        }
                    }, (long) (randomTime + 250), TimeUnit.MILLISECONDS);
                }
                break;
        }
    }

    private void onDirtCheck() {
        if (fakeMovementCheck()) return;

        switch (dirtCheckState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                dirtCheckState = DirtCheckState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                minimumReactions = (int) Math.round(3 + Math.random() * 3);
                LogUtils.sendWarning("[Failsafe] Minimum reactions: " + minimumReactions);
                KeyBindUtils.stopMovement();
                if (BlockUtils.getRelativeBlock(1, 1, 0).isCollidable())
                    dirtOnLeft = true;
                else if (BlockUtils.getRelativeBlock(-1, 1, 0).isCollidable())
                    dirtOnLeft = false;
                positionBeforeReacting = mc.thePlayer.getPosition();
                rotationBeforeReacting = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                dirtCheckState = DirtCheckState.PLAY_RECORDING;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                break;
            case PLAY_RECORDING:
                // CanFly && isAboveClear,
                // BlockedLeft,
                // BlockedRight,
                // Universal,
                // OnGround,
                // JumpBoost
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                if (FlyPathfinder.getInstance().isRunning())
                    break;
                String tempRecordingName = "DIRT_CHECK_";
                if (dirtOnLeft)
                    tempRecordingName += "BlockedLeft_";
                else
                    tempRecordingName += "BlockedRight_";
                if (Math.random() < 0.2) {
                    dirtCheckState = DirtCheckState.SEND_MESSAGE;
                    failsafeDelay.schedule((long) (800 + Math.random() * 2_000));
                    break;
                }
                if (mc.thePlayer.onGround && Math.random() < 0.3) {
                    tempRecordingName += "OnGround_";
                }
                if (mc.thePlayer.getActivePotionEffects() != null
                        && mc.thePlayer.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getPotionID() == 8)
                        && Math.random() < 0.2) {
                    tempRecordingName += "JumpBoost_";
                }
                if (mc.thePlayer.capabilities.allowFlying && BlockUtils.isAboveHeadClear() && Math.random() < 0.3) {
                    Random random = new Random();
                    flyingRecordingNumber = random.nextInt(maxFlyingRecordingNumber) + 1;
                    if (mc.thePlayer.onGround)
                        tempRecordingName += "FlyAway_" + flyingRecordingNumber;
                    else
                        tempRecordingName += "FlyBack_" + flyingRecordingNumber;
                }
                if (minimumReactions > 0) {
                    LogUtils.sendWarning("[Failsafe] Playing recording: " + tempRecordingName);
                    MovRecPlayer.getInstance().playRandomRecording(tempRecordingName, true);
                    if (!tempRecordingName.contains("FlyAway"))
                        minimumReactions--;
                } else {
                    FlyPathfinder.getInstance().getPathTo(new GoalBlock(positionBeforeReacting));
                    dirtCheckState = DirtCheckState.GO_BACK_END;
                }
                failsafeDelay.schedule((long) (500 + Math.random() * 4_000));
                break;
            case GO_BACK_END:
                if (FlyPathfinder.getInstance().isRunning())
                    break;
                if (checkForPathingFinish()) {
                    dirtCheckState = DirtCheckState.END_DIRT_CHECK;
                } else if (mc.thePlayer.getPosition() == positionBeforeReacting) {
                    mc.thePlayer.capabilities.isFlying = false;
                } else if (mc.thePlayer.onGround && mc.thePlayer.getPosition().distanceSq(positionBeforeReacting) > 2) {
                    KeyBindUtils.stopMovement();
                    FlyPathfinder.getInstance().getPathTo(new GoalBlock(positionBeforeReacting));
                    PathingCommand pathingCommand = new PathingCommand(new GoalBlock(positionBeforeReacting), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().secretInternalSetGoalAndPath(pathingCommand);
                }
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                break;
            case SEND_MESSAGE:
                if (this.rotation.isRotating()) return;
                String randomMessage;
                if (CustomFailsafeMessagesPage.customDirtMessages.isEmpty()) {
                    randomMessage = getRandomMessage(FAILSAFE_MESSAGES);
                } else {
                    String[] customMessages = CustomFailsafeMessagesPage.customDirtMessages.split("\\|");
                    randomMessage = getRandomMessage(customMessages);
                }
                LogUtils.sendDebug("[Failsafe] Chosen message: " + randomMessage);
                mc.thePlayer.sendChatMessage("/ac " + randomMessage);
                dirtCheckState = DirtCheckState.PLAY_RECORDING;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                break;
            case END_DIRT_CHECK:
                if (this.rotation.isRotating()) return;
                long randomTime4 = FarmHelperConfig.getRandomRotationTime();
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                rotation.easeTo(new RotationConfiguration(new Rotation(rotationBeforeReacting.getYaw(), rotationBeforeReacting.getPitch()),
                        randomTime4, null));
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished dirt check failsafe");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, randomTime4 + 250, TimeUnit.MILLISECONDS);
                break;
        }
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
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        mc.thePlayer.sendChatMessage("/warp garden");
                        failsafeDelay.schedule((long) (2_500 + Math.random() * 2_000));
                    } else {
                        mc.thePlayer.sendChatMessage("/skyblock");
                        failsafeDelay.schedule((long) (5_500 + Math.random() * 4_000));
                    }
                }
                break;
            case END:
                Failsafe.getInstance().stop();
                MacroHandler.getInstance().resumeMacro();
                LogUtils.sendFailsafeMessage("[Failsafe] Came back from evacuation!");
                break;
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
                if (this.rotation.isRotating()) return;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                itemChangeState = ItemChangeState.END;
                break;
            case END:
                PlayerUtils.getTool();
                Failsafe.getInstance().stop();
                Multithreading.schedule(() -> {
                    LogUtils.sendDebug("[Failsafe] Finished item change failsafe. Farming...");
                    MacroHandler.getInstance().resumeMacro();
                }, 500, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void onWorldChange() {
        if (!FarmHelperConfig.autoTPOnWorldChange) {
            LogUtils.sendDebug("[Failsafe] Auto TP on world change is disabled! Disabling macro and disconnecting...");
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
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.TELEPORTING) {
                    failsafeDelay.schedule(1_000);
                    return;
                }
                if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LOBBY && !LagDetector.getInstance().isLagging()) {
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
                    LogUtils.sendDebug("[Failsafe] Came back to the garden. Farming...");
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                    return;
                } else if (!LagDetector.getInstance().isLagging()) {
                    LogUtils.sendDebug("[Failsafe] Sending /warp garden command...");
                    mc.thePlayer.sendChatMessage("/warp garden");
                    failsafeDelay.schedule((long) (4_500 + Math.random() * 1_000));
                }
                break;
        }
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
                rotation.easeTo(new RotationConfiguration(new Rotation((float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30), (float) (30 + Math.random() * 20 - 10)), (long) randomTime, null));
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
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, (int) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    private void onDisconnect() {
        if (!AutoReconnect.getInstance().isRunning() && AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Trying to reconnect...");
            Notifications.INSTANCE.send("Farm Helper", "Disconnected from server! Trying to reconnect...");
            AutoReconnect.getInstance().getReconnectDelay().schedule(5_000);
            AutoReconnect.getInstance().start();
        } else if (!AutoReconnect.getInstance().isRunning() && !AutoReconnect.getInstance().isToggled()) {
            System.out.println("[Reconnect] Disconnected from server! Stopping macro...");
            Notifications.INSTANCE.send("Farm Helper", "Disconnected from server! Stopping macro...");
            MacroHandler.getInstance().disableMacro();
            stop();
        } else if (AutoReconnect.getInstance().isRunning()) {
            System.out.println("[Reconnect] Disconnected from server! Reconnect is already running!");
            Notifications.INSTANCE.send("Farm Helper", "Disconnected from server! Reconnect is already running!");
            stop();
        }
    }

    private void onJacobCheck() {
        if (FarmHelperConfig.jacobFailsafeAction) {
            // pause
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of extended Jacob's Content!", false);
                MacroHandler.getInstance().pauseMacro();
            } else {
                if (!GameStateHandler.getInstance().inJacobContest()) {
                    LogUtils.sendFailsafeMessage("[Failsafe] Resuming the macro because Jacob's Contest is over!", false);
                    Failsafe.getInstance().stop();
                    MacroHandler.getInstance().resumeMacro();
                }
            }
        } else {
            // leave
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Leaving because of extending Jacob's Contest!", false);
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

    private void onLowerBPS() {
        switch (lowerBPSState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                lowerBPSState = LowerBPSState.WAIT_BEFORE_START;
                break;
            case WAIT_BEFORE_START:
                KeyBindUtils.stopMovement();
                MacroHandler.getInstance().pauseMacro();
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                lowerBPSState = LowerBPSState.WARP_BACK;
                break;
            case WARP_BACK:
                if (GameStateHandler.getInstance().inGarden()) {
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                    failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                    lowerBPSState = LowerBPSState.END;
                } else {
                    if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.HUB) {
                        mc.thePlayer.sendChatMessage("/warp garden");
                        failsafeDelay.schedule((long) (2_500 + Math.random() * 2_000));
                    } else if (GameStateHandler.getInstance().getLocation() == GameStateHandler.Location.LIMBO) {
                        mc.thePlayer.sendChatMessage("/l");
                        failsafeDelay.schedule((long) (2_500 + Math.random() * 2_000));
                    } else {
                        mc.thePlayer.sendChatMessage("/skyblock");
                        failsafeDelay.schedule((long) (5_500 + Math.random() * 4_000));
                    }
                }
                break;
            case END:
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(
                                        (float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30),
                                        (float) (30 + Math.random() * 20 - 10))
                                , (long) randomTime, null));
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, (long) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    public void onGuestVisit() {
        tabListCheckDelay.schedule(5000L);
        if (FarmHelperConfig.pauseWhenGuestArrives) {
            if (!MacroHandler.getInstance().isCurrentMacroPaused()) {
                LogUtils.sendFailsafeMessage("[Failsafe] Paused the macro because of guest visit!", false);
                MacroHandler.getInstance().pauseMacro();
            }
        }
    }

    public void onCustomFailsafeReaction(String emergencyName) {
        if (fakeMovementCheck()) return;

        switch (recordingCheckState) {
            case NONE:
                failsafeDelay.schedule((long) (500f + Math.random() * 1_000f));
                recordingCheckState = CustomMovementState.WAIT_BEFORE_START;
                playingState = 0;
                break;
            case WAIT_BEFORE_START:
                MacroHandler.getInstance().pauseMacro();
                lookAroundTimes = (int) Math.round(3 + Math.random() * 3);
                currentLookAroundTimes = 0;
                recordingCheckState = CustomMovementState.LOOK_AROUND;
                failsafeDelay.schedule((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
                break;
            case LOOK_AROUND:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                switch (playingState) {
                    case 0: // play recording
                        MovRecPlayer.getInstance().playRandomRecording(emergencyName + "_PreChat_", false);
                        failsafeDelay.schedule((long) (2000 + Math.random() * 1_000));
                        playingState = 1;
                        break;
                    case 1: // this will be executed once it finishes playing
                        if (FarmHelperConfig.sendFailsafeMessage) {
                            recordingCheckState = CustomMovementState.TYPE_SHIT;
                            failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                        } else {
                            recordingCheckState = CustomMovementState.LOOK_AROUND_2;
                        }
                        playingState = 2;
                        break;
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
                recordingCheckState = CustomMovementState.LOOK_AROUND_2;
                failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                currentLookAroundTimes = 0;
                lookAroundTimes = (int) (2 + Math.random() * 2);
                break;
            case LOOK_AROUND_2:
                if (MovRecPlayer.getInstance().isRunning())
                    break;
                switch (playingState) {
                    case 2: // play another recording
                        MovRecPlayer.getInstance().playRandomRecording(emergencyName + "_PostChat_", false);
                        failsafeDelay.schedule((long) (2000 + Math.random() * 1_000));
                        playingState = 3;
                        break;
                    case 3: // this will be executed once it finishes playing
                        recordingCheckState = CustomMovementState.END;
                        failsafeDelay.schedule((long) (500 + Math.random() * 1_000));
                        playingState = 0;
                        break;
                }
                break;
            case END:
                float randomTime = FarmHelperConfig.getRandomRotationTime();
                this.rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(
                                        (float) (mc.thePlayer.rotationYaw + Math.random() * 60 - 30),
                                        (float) (30 + Math.random() * 20 - 10))
                                , (long) randomTime, null));
                Failsafe.getInstance().stop();
                if (FarmHelperConfig.enableRestartAfterFailSafe) {
                    MacroHandler.getInstance().pauseMacro();
                } else {
                    MacroHandler.getInstance().disableMacro();
                }
                Multithreading.schedule(() -> {
                    InventoryUtils.openInventory();
                    LogUtils.sendDebug("[Failsafe] Finished playing custom movement recording");
                    if (FarmHelperConfig.enableRestartAfterFailSafe) {
                        LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                        restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
                    }
                }, (long) randomTime + 250, TimeUnit.MILLISECONDS);
                break;
        }
    }

    // endregion

    // region RESETS

    private void resetRotationCheck() {
        rotationCheckState = RotationCheckState.NONE;
    }

    private void resetDirtCheck() {
        dirtBlocks.clear();
        dirtCheckState = DirtCheckState.NONE;
    }

    private void resetEvacuateCheck() {
        evacuateState = EvacuateState.NONE;
    }

    private void resetItemChangeCheck() {
        itemChangeState = ItemChangeState.NONE;
    }

    private void resetWorldChangeCheck() {
        worldChangeState = WorldChangeState.NONE;
    }

    private void resetBedrockCageCheck() {
        bedrockCageState = BedrockCageState.NONE;
    }

    public void resetCustomMovement() {
        recordingCheckState = CustomMovementState.NONE;
        playingState = 0;
        recordingFile = false;
        recordingFile2 = false;
    }

    public void resetLowerBPS() {
        lowerBPSState = LowerBPSState.NONE;
    }

    public void resetGuestVisit() {
        tabListCheckDelay.reset();
        wasGuestOnGarden = false;
        lastGuestName = "";
    }

    // endregion

    // region HELPER_METHODS

    private boolean firstCheckReturn() {
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (!MacroHandler.getInstance().isMacroToggled()) return true;
        if (isEmergency()) return true;
        return FeatureManager.getInstance().shouldIgnoreFalseCheck();
    }

    private boolean fakeMovementCheck() {
        if (!FarmHelperConfig.fakeMovements) {
            LogUtils.sendDebug("[Failsafe] Fake movement is disabled! Disabling macro.");
            if (FarmHelperConfig.enableRestartAfterFailSafe) {
                MacroHandler.getInstance().pauseMacro();
                LogUtils.sendDebug("[Failsafe] Restarting the macro in " + FarmHelperConfig.restartAfterFailSafeDelay + " minutes.");
                restartMacroAfterFailsafeDelay.schedule(FarmHelperConfig.restartAfterFailSafeDelay * 1_000L * 60L);
            } else {
                MacroHandler.getInstance().disableMacro();
            }
            Failsafe.getInstance().stop();
            return true;
        }
        UngrabMouse.getInstance().ungrabMouse();
        return false;
    }

    private void randomMoveAndRotate() {
        long rotationTime = FarmHelperConfig.getRandomRotationTime();
        this.rotation.easeTo(
                new RotationConfiguration(
                        new Rotation(
                                mc.thePlayer.rotationYaw + randomValueBetweenExt(-180, 180, 45),
                                randomValueBetweenExt(-20, 40, 5)),
                        rotationTime, null));
        failsafeDelay.schedule(rotationTime - 50);
        currentLookAroundTimes++;
        double randomKey = Math.random();
        if (!mc.thePlayer.onGround && FarmHelperConfig.tryToUseJumpingAndFlying) {
            if (randomKey <= 0.3) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.6) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else {
                KeyBindUtils.stopMovement();
            }
        } else {
            if (randomKey <= 0.175 && GameStateHandler.getInstance().isFrontWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.35 && GameStateHandler.getInstance().isLeftWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.525 && GameStateHandler.getInstance().isRightWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            } else if (randomKey <= 0.70 && GameStateHandler.getInstance().isBackWalkable()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                Multithreading.schedule(() -> KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false), (long) (rotationTime + Math.random() * 150), TimeUnit.MILLISECONDS);
            }
            if (randomKey > 0.7) {
                KeyBindUtils.stopMovement();
                if (FarmHelperConfig.tryToUseJumpingAndFlying) {
                    mc.thePlayer.jump();
                    Multithreading.schedule(() -> {
                        if (!mc.thePlayer.onGround && mc.thePlayer.capabilities.allowEdit && mc.thePlayer.capabilities.allowFlying && !mc.thePlayer.capabilities.isFlying) {
                            mc.thePlayer.capabilities.isFlying = true;
                            mc.thePlayer.sendPlayerAbilities();
                        }
                    }, (long) (250 + Math.random() * 250), TimeUnit.MILLISECONDS);
                }
            }
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

    public void checkReactionType() {
        if (reactionType != 0) return;
        if (shouldPlayCustomRecording(emergency))
            reactionType = 1;
        else
            reactionType = 2;
    }

    public boolean shouldPlayCustomRecording(EmergencyType emergency) {
        switch (emergency) {
            case DIRT_CHECK:
            case ROTATION_CHECK:
            case TELEPORT_CHECK:
            case ITEM_CHANGE_CHECK:
            case BEDROCK_CAGE_CHECK:
                if (movementFilesExist(emergency.name())) {
                    return true;
                }
        }
        return false;
    }

    private boolean movementFilesExist(String emergencyName) {
        File recordingDir = new File(mc.mcDataDir, "movementrecorder");
        if (recordingDir.exists()) {
            File[] recordingFiles = recordingDir.listFiles();
            if (recordingFiles != null) {
                for (File file : recordingFiles) {
                    if (file.getName().contains(emergencyName + "_PreChat_")) {
                        recordingFile = true;
                    } else if (file.getName().contains(emergencyName + "_PostChat_")) {
                        recordingFile2 = true;
                    }
                    if (recordingFile && recordingFile2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void handleRecordingError(int reactionType) {
        if (Failsafe.getInstance().isRunning()) {
            LogUtils.sendWarning("[Movement Recorder] Something wrong happened. Switching to built-in legacy failsafe mechanism instead.");
            Failsafe.getInstance().resetCustomMovement();
            Failsafe.getInstance().setReactionType(reactionType);
        }
        MovRecPlayer.getInstance().stop();
        MovRecPlayer.getInstance().resetStatesAfterMacroDisabled();
    }

    public float getAverageBPS() {
        float averageBPS = 0;
        for (float bpsValue : bpsQueue) {
            averageBPS += bpsValue;
        }
        averageBPS /= bpsQueue.size();
        return averageBPS;
    }

    private boolean checkForPathingFinish() {
        if (pathing) {
            GoalNear goal = (GoalNear) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
            double distance = mc.thePlayer.getDistance(goal.getGoalPos().getX() + 0.5f, mc.thePlayer.posY, goal.getGoalPos().getZ() + 0.5);
            System.out.println(BaritoneEventListener.pathEvent);
            if (distance <= 1 || BaritoneEventListener.pathEvent == PathEvent.AT_GOAL) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                pathing = false;
                return false;
            }
            return true;
        }
        return false;
    }

    // endregion

    // region CHECKS

    private boolean shouldNotify(EmergencyType emergency) {
        if (!FarmHelperConfig.popUpNotification)
            return false;
        switch (emergency) {
            case ROTATION_CHECK:
                return FailsafeNotificationsPage.notifyOnRotationFailsafe;
            case TELEPORT_CHECK:
                return FailsafeNotificationsPage.notifyOnTeleportationFailsafe;
            case DIRT_CHECK:
                return FailsafeNotificationsPage.notifyOnDirtFailsafe;
            case ITEM_CHANGE_CHECK:
                return FailsafeNotificationsPage.notifyOnItemChangeFailsafe;
            case WORLD_CHANGE_CHECK:
                return FailsafeNotificationsPage.notifyOnWorldChangeFailsafe;
            case BEDROCK_CAGE_CHECK:
                return FailsafeNotificationsPage.notifyOnBedrockCageFailsafe;
            case EVACUATE:
                return FailsafeNotificationsPage.notifyOnEvacuateFailsafe;
            case BANWAVE:
                return FailsafeNotificationsPage.notifyOnBanwaveFailsafe;
            case DISCONNECT:
                return FailsafeNotificationsPage.notifyOnDisconnectFailsafe;
            case JACOB:
                return FailsafeNotificationsPage.notifyOnJacobFailsafe;
            case LOWER_AVERAGE_BPS:
                return FailsafeNotificationsPage.notifyOnLowerAverageBPS;
            case GUEST_VISIT:
                return FailsafeNotificationsPage.notifyOnGuestVisit;
        }
        return false;
    }

    private boolean shouldPlaySoundAlert(EmergencyType emergency) {
        switch (emergency) {
            case ROTATION_CHECK:
                return FailsafeNotificationsPage.alertOnRotationFailsafe;
            case TELEPORT_CHECK:
                return FailsafeNotificationsPage.alertOnTeleportationFailsafe;
            case DIRT_CHECK:
                return FailsafeNotificationsPage.alertOnDirtFailsafe;
            case ITEM_CHANGE_CHECK:
                return FailsafeNotificationsPage.alertOnItemChangeFailsafe;
            case WORLD_CHANGE_CHECK:
                return FailsafeNotificationsPage.alertOnWorldChangeFailsafe;
            case BEDROCK_CAGE_CHECK:
                return FailsafeNotificationsPage.alertOnBedrockCageFailsafe;
            case EVACUATE:
                return FailsafeNotificationsPage.alertOnEvacuateFailsafe;
            case BANWAVE:
                return FailsafeNotificationsPage.alertOnBanwaveFailsafe;
            case DISCONNECT:
                return FailsafeNotificationsPage.alertOnDisconnectFailsafe;
            case JACOB:
                return FailsafeNotificationsPage.alertOnJacobFailsafe;
            case LOWER_AVERAGE_BPS:
                return FailsafeNotificationsPage.alertOnLowerAverageBPS;
            case GUEST_VISIT:
                return FailsafeNotificationsPage.alertOnGuestVisit;
        }
        return false;
    }

    private boolean shouldAltTab(EmergencyType emergency) {
        switch (emergency) {
            case ROTATION_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnRotationFailsafe;
            case TELEPORT_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnTeleportationFailsafe;
            case DIRT_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnDirtFailsafe;
            case ITEM_CHANGE_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnItemChangeFailsafe;
            case WORLD_CHANGE_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnWorldChangeFailsafe;
            case BEDROCK_CAGE_CHECK:
                return FailsafeNotificationsPage.autoAltTabOnBedrockCageFailsafe;
            case EVACUATE:
                return FailsafeNotificationsPage.autoAltTabOnEvacuateFailsafe;
            case BANWAVE:
                return FailsafeNotificationsPage.autoAltTabOnBanwaveFailsafe;
            case DISCONNECT:
                return FailsafeNotificationsPage.autoAltTabOnDisconnectFailsafe;
            case JACOB:
                return FailsafeNotificationsPage.autoAltTabOnJacobFailsafe;
            case LOWER_AVERAGE_BPS:
                return FailsafeNotificationsPage.autoAltTabOnLowerAverageBPS;
            case GUEST_VISIT:
                return FailsafeNotificationsPage.autoAltTabOnGuestVisit;
        }
        return false;
    }

    private boolean shouldTagEveryone(EmergencyType emergency) {
        switch (emergency) {
            case ROTATION_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnRotationFailsafe;
            case TELEPORT_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnTeleportationFailsafe;
            case DIRT_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnDirtFailsafe;
            case ITEM_CHANGE_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnItemChangeFailsafe;
            case WORLD_CHANGE_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnWorldChangeFailsafe;
            case BEDROCK_CAGE_CHECK:
                return FailsafeNotificationsPage.tagEveryoneOnBedrockCageFailsafe;
            case EVACUATE:
                return FailsafeNotificationsPage.tagEveryoneOnEvacuateFailsafe;
            case BANWAVE:
                return FailsafeNotificationsPage.tagEveryoneOnBanwaveFailsafe;
            case DISCONNECT:
                return FailsafeNotificationsPage.tagEveryoneOnDisconnectFailsafe;
            case JACOB:
                return FailsafeNotificationsPage.tagEveryoneOnJacobFailsafe;
            case LOWER_AVERAGE_BPS:
                return FailsafeNotificationsPage.tagEveryoneOnLowerAverageBPS;
            case GUEST_VISIT:
                return FailsafeNotificationsPage.tagEveryoneOnGuestVisit;
        }
        return false;
    }

    // endregion

    // region ENUMS

    public enum EmergencyType {
        NONE,
        ROTATION_CHECK("You've got§l ROTATED§r§d by staff member!", 4),
        TELEPORT_CHECK("You've got§l TELEPORTED§r§d by staff member!", 5),
        DIRT_CHECK("You've got§l DIRT CHECKED§r§d by staff member!", 3),
        ITEM_CHANGE_CHECK("Your §lITEM HAS CHANGED§r§d!", 3),
        WORLD_CHANGE_CHECK("Your §lWORLD HAS CHANGED§r§d!", 2),
        BEDROCK_CAGE_CHECK("You've got§l BEDROCK CAGED§r§d by staff member!", 1),
        EVACUATE("Server is restarting! Evacuate!", 1),
        BANWAVE("Banwave has been detected!", 6),
        DISCONNECT("You've been§l DISCONNECTED§r§d from the server!", 1),
        LOWER_AVERAGE_BPS("Your BPS is lower than average!", 9),
        JACOB("You've extended the §lJACOB COUNTER§r§d!", 7),
        GUEST_VISIT("You've got§l VISITED§r§d by "
                + (!getInstance().lastGuestName.isEmpty() ? getInstance().lastGuestName : "a guest") + "!", 1);

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

    enum RotationCheckState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        TYPE_SHIT,
        LOOK_AROUND_2,
        END,
    }

    enum DirtCheckState {
        NONE,
        WAIT_BEFORE_START,
        ROTATE_INTO_DIRT,
        PLAY_RECORDING,
        GO_BACK_END,
        SEND_MESSAGE,
        ROTATE_INTO_DIRT_2,
        END_DIRT_CHECK
    }

    enum EvacuateState {
        NONE,
        EVACUATE_FROM_ISLAND,
        TP_BACK_TO_ISLAND,
        END
    }

    enum ItemChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SWAP_BACK_ITEM,
        END
    }

    enum WorldChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        END
    }

    enum BedrockCageState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        TYPE_SHIT,
        LOOK_AROUND_2,
        END,
    }

    enum CustomMovementState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        TYPE_SHIT,
        LOOK_AROUND_2,
        END
    }

    enum LowerBPSState {
        NONE,
        WAIT_BEFORE_START,
        WARP_BACK,
        END
    }

    // endregion
}
