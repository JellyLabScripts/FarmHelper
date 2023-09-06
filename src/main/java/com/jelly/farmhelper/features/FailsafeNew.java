package com.jelly.farmhelper.features;

import akka.japi.Pair;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.JacobsContestHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jelly.farmhelper.FarmHelper.config;
import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;

public class FailsafeNew {

    enum EvacuateState {
        NONE,
        EVACUATE_FROM_ISLAND,
        RETURN_TO_ISLAND
    }

    public enum FailsafeType {
        ITEM_CHANGE("Your item has been changed by a staff member"),
        ROTATION("You may have been rotation checked by a staff member"),
        TELEPORTATION("You may have been teleported by a staff member"),
        DESYNC("You are desynced. You might be lagging or there might be a staff spectating. If this is happening frequently, disable check desync"),
        BEDROCK("You have been bedrock checked"),
        DIRT("You may have been dirt checked");

        final String label;

        FailsafeType(String s) {
            this.label = s;
        }
    }

    private static final ArrayList<FailsafeType> detectedFailsafes = new ArrayList<>();

    public static FailsafeType findHighestPriorityElement() {
        FailsafeType highestPriority = null;
        int maxPriority = Integer.MIN_VALUE;

        for (FailsafeType element : detectedFailsafes) {
            int priority = element.ordinal();
            if (priority > maxPriority) {
                maxPriority = priority;
                highestPriority = element;
            }
        }

        return highestPriority;
    }

    public static boolean emergency = false;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final Clock cooldown = new Clock();
    public static Clock restartAfterFailsafeCooldown = new Clock();
    public static Clock getAllEmergencies = new Clock();
    private static EvacuateState evacuateState = EvacuateState.NONE;
    private static boolean isDuringBedrockCheck = false;
    private static ExecutorService emergencyThreadExecutor = Executors.newScheduledThreadPool(5);


    private static final String[] FAILSAFE_MESSAGES = new String[] {
            "WHAT", "what?", "what", "what??", "what???", "wut?", "?", "what???", "yo huh", "yo huh?", "yo?",
            "ehhhhh??", "eh", "yo", "ahmm", "ehh", "LOL what", "lol :skull:", "bro wtf was that?", "lmao",
            "lmfao :sob:", "lmfao", "wtf is this", "wtf", "WTF", "wtf is this?", "wtf???", "tf", "tf?", "wth",
            "lmao what?", "????", "??", "???????", "???", "UMMM???", "umm", "ummm???", "damn wth",
            "dang it", "Damn", "damn wtf", "damn", "hmmm", "hm", "sus", "hmm", "ok??", "ok?", "give me a rest", "im done",
            "again lol", "again??", "ok damn", "seriously?", "seriously????", "seriously", "really?", "really",
            "are you kidding me?", "are you serious?", "are you fr???", "oh come on", "oh come on :sob:",
            "not again", "not again :sob:", "give me a break", "youre kidding right?", "youre joking", "youre kidding me",
            "you must be joking", "seriously bro?", "cmon now", "cmon", "this is too much", "stop messing with me :sob:"};

    public static void resetFailsafes() {
        LogUtils.sendDebug("Resetting failsafes");
        emergency = false;
        rotation.rotating = false;
        bedrockCheckSent = false;
        isDuringBedrockCheck = false;
        cooldown.reset();
        rotation.reset();
        restartAfterFailsafeCooldown.reset();
        evacuateState = EvacuateState.NONE;
        emergencyThreadExecutor.shutdownNow();
        emergencyThreadExecutor = Executors.newScheduledThreadPool(5);
        getAllEmergencies.reset();
        detectedFailsafes.clear();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!config.debugMode) return;

        if (cooldown.isScheduled() && !cooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("cooldown: " + cooldown.getRemainingTime(), 300, 32, Color.GREEN.getRGB());
        }

        if (restartAfterFailsafeCooldown.isScheduled() && !restartAfterFailsafeCooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("restartAfterFailsafeCooldown: " + restartAfterFailsafeCooldown.getRemainingTime(), 300, 2, Color.GREEN.getRGB());
        }
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!MacroHandler.isMacroing) return;
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
            LogUtils.sendDebug("Failed teleport - waiting");
            cooldown.schedule(10000);
        }
        if (message.contains("to warp out! CLICK to warp now!")) {
            Autosell.disableOnly();
            VisitorsMacro.stopMacro();
            if (config.setSpawnBeforeEvacuate)
                PlayerUtils.setSpawn();
            LogUtils.sendDebug("Update or restart is required - Evacuating in 3s");
            cooldown.schedule(3_000);
            evacuateState = EvacuateState.EVACUATE_FROM_ISLAND;
            if (config.enableScheduler)
                Scheduler.pause();
            MacroHandler.disableCurrentMacro(config.setSpawnBeforeEvacuate);
            KeyBindUtils.stopMovement();
        }
    }


    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        if (restartAfterFailsafeCooldown.isScheduled()) {
            if (restartAfterFailsafeCooldown.passed()) {
                LogUtils.sendDebug("Restarting macro after failsafe is passed");
                restartAfterFailsafeCooldown.reset();
                if (config.enableScheduler)
                    Scheduler.start();
                MacroHandler.enableCurrentMacro();
            }
            return;
        }

        if (!MacroHandler.isMacroing) return;

        if (MacroHandler.currentMacro != null && MacroHandler.currentMacro.enabled) {
            if (dirtCheck()) return;
            if (bedrockCheck()) return;
        }
        if (jacobCheck()) return;
        locationCheck();
        emergencyCheck();
    }

    private void emergencyCheck() {
        if (!emergency) return;
        if (cooldown.isScheduled() && !cooldown.passed()) return;
        if (restartAfterFailsafeCooldown.isScheduled() && !restartAfterFailsafeCooldown.passed()) return;
        if (!getAllEmergencies.isScheduled() || !getAllEmergencies.passed()) return;
        if (findHighestPriorityElement() == null) return;
        if (AutoReconnect.currentState != AutoReconnect.reconnectingState.NONE) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;

        getAllEmergencies.reset();
        takeAction(findHighestPriorityElement());
        LogUtils.sendDebug("Chosen emergency: " + findHighestPriorityElement());
        detectedFailsafes.clear();
    }

    private static boolean bedrockCheckSent = false;

    private boolean bedrockCheck() {

        if (!bedrockCheckSent && BlockUtils.bedrockCount() > 2 && mc.thePlayer.getPosition().getY() > 50) {
            emergencyFailsafe(FailsafeType.BEDROCK);
            bedrockCheckSent = true;
            return true;
        }
        return false;
    }

    private void locationCheck() {
        LocationUtils.Island location = LocationUtils.currentIsland;

        if (location == null) return;

        switch (location) {
            case LIMBO: {
                if (!FarmHelper.config.autoTPOnWorldChange) return;
                if (!cooldown.isScheduled()) {
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (MacroHandler.currentMacro.enabled) {
                        MacroHandler.disableCurrentMacro(true);
                    }
                    return;
                }
                if (cooldown.isScheduled() && cooldown.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back from Limbo");
                    LogUtils.sendDebug("Not at island - teleporting back from Limbo");
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                        restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1000L);
                }
                break;
            }
            case LOBBY: {
                if (!FarmHelper.config.autoTPOnWorldChange) return;
                if (!cooldown.isScheduled()) {
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (MacroHandler.currentMacro.enabled) {
                        MacroHandler.disableCurrentMacro(true);
                    }
                    return;
                }
                if (cooldown.isScheduled() && cooldown.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back from Lobby");
                    LogUtils.sendDebug("Not at island - teleporting back from Lobby");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                        restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1000L);
                }
                break;
            }
            case THE_HUB: {
                if (!FarmHelper.config.autoTPOnWorldChange) return;
                if (!cooldown.isScheduled()) {
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (MacroHandler.currentMacro.enabled) {
                        MacroHandler.disableCurrentMacro(true);
                    }
                    return;
                }
                if (evacuateState == EvacuateState.RETURN_TO_ISLAND) {
                    if (!cooldown.passed()) {
                        LogUtils.sendDebug("Waiting after evacuate cooldown in hub: " + (String.format("%.1f", cooldown.getRemainingTime() / 1000f)));
                        return;
                    }
                    if (cooldown.passed()) {
                        LogUtils.sendDebug("Not at island - teleporting back from Evacuating");
                        LogUtils.webhookLog("Not at island - teleporting back from Evacuating");
                        mc.thePlayer.sendChatMessage("/warp garden");
                        cooldown.schedule((long) (5000 + Math.random() * 5000));
                    }
                    return;
                }
                if (cooldown.isScheduled() && cooldown.passed()) {
                    LogUtils.sendDebug("Not at island - teleporting back from The Hub");
                    LogUtils.webhookLog("Not at island - teleporting back from The Hub");
                    mc.thePlayer.sendChatMessage("/warp garden");
                    cooldown.schedule((long) (5000 + Math.random() * 5000));
                    if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                        restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1000L);
                }
                break;
            }
            case PRIVATE_ISLAND:
            case GARDEN: {
                if (evacuateState == EvacuateState.EVACUATE_FROM_ISLAND) {
                    if (!cooldown.isScheduled()) {
                        if (MacroHandler.currentMacro.enabled) {
                            MacroHandler.disableCurrentMacro(config.setSpawnBeforeEvacuate);
                        }
                        cooldown.schedule((long) (5000 + Math.random() * 5000));
                        return;
                    }
                    if (!cooldown.passed()) {
                        LogUtils.sendDebug("Waiting before evacuate cooldown: " + (String.format("%.1f", cooldown.getRemainingTime() / 1000f)));
                        return;
                    }
                    if (cooldown.passed()) {
                        LogUtils.sendDebug("Evacuating");
                        LogUtils.webhookLog("Evacuating");
                        mc.thePlayer.sendChatMessage("/evacuate");
                        cooldown.schedule((long) (5000 + Math.random() * 5000));
                        evacuateState = EvacuateState.RETURN_TO_ISLAND;
                    }
                    return;
                } else if (evacuateState == EvacuateState.RETURN_TO_ISLAND) {
                    if (!cooldown.isScheduled()) {
                        if (MacroHandler.currentMacro.enabled) {
                            MacroHandler.disableCurrentMacro(config.setSpawnBeforeEvacuate);
                        }
                        cooldown.schedule((long) (5000 + Math.random() * 5000));
                        return;
                    }
                    if (!cooldown.passed()) {
                        LogUtils.sendDebug("Waiting after evacuate cooldown on island: " + (String.format("%.1f", cooldown.getRemainingTime() / 1000f)));
                        return;
                    }
                    if (cooldown.passed()) {
                        LogUtils.sendDebug("Came back from evacuate");
                        resetFailsafes();
                    }
                    return;
                } else {
                    if (cooldown.isScheduled() && !cooldown.passed()) {
                        LogUtils.sendDebug("Waiting after teleportation: " + (String.format("%.1f", cooldown.getRemainingTime() / 1000f)));
                        return;
                    }
                    if (cooldown.isScheduled() && cooldown.passed()) {
                        LogUtils.sendDebug("Came back to island");
                        if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                            restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1000L);
                        cooldown.reset();
                    }
                }

                if (config.enableJacobFailsafes && jacobExceeded()) {
                    if (!cooldown.isScheduled()) {
                        long remainingTime = getJacobRemaining() + 3_000;
                        LogUtils.sendDebug("Exceeded Jacob limit - waiting " + (String.format("%.1f", remainingTime / 1000f)));
                        LogUtils.webhookLog("Exceeded Jacob limit - waiting " + (String.format("%.1f", remainingTime / 1000f)));
                        cooldown.schedule(remainingTime);
                        MacroHandler.disableCurrentMacro(true);
                        isJacobFailsafeExceeded = true;
                    } else {
                        LogUtils.sendDebug("Unknown state, report to #yuro1337 :troll:");
                        LogUtils.webhookLog("Unknown state, report to #yuro1337 :troll:");
                        cooldown.reset();
                    }
                    return;
                }

                if (MacroHandler.currentMacro.enabled) return;
                if (!cooldown.passed() || cooldown.isScheduled()) return;
                if (Autosell.isEnabled()) return;
                if (MacroHandler.startingUp) return;
                if (!Scheduler.isFarming() && !JacobsContestHandler.jacobsContestTriggered) return;
                if (!FarmHelper.config.pauseSchedulerDuringJacobsContest && JacobsContestHandler.jacobsContestTriggered) return;
                if (AutoCookie.isEnabled()) return;
                if (AutoPot.isEnabled()) return;
                if (BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave) return;
                if (emergency) return;
                if (VisitorsMacro.isEnabled()) return;
                if (PetSwapper.isEnabled()) return;
                if (evacuateState != EvacuateState.NONE) return;
                if (!MacroHandler.currentMacro.isRewarpLocationSet()) return;

                LogUtils.sendDebug("Resuming the macro...");
                MacroHandler.enableCurrentMacro();
                break;
            }
            default: {
                LogUtils.sendDebug("You are outside the garden! Disabling the macro...");
                MacroHandler.disableMacro();
            }
        }
    }

    /* ==================
        Item Swap Check
    =================== */

//    private static ItemStack previousTickItemStack = null;
//
//    public boolean changeItemCheck() {
//        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN)
//            return false;
//
//        if (previousTickItemStack == null) {
//            int idOfHoe = PlayerUtils.getHoeSlot(MacroHandler.crop);
//            if (idOfHoe == -1) {
//                LogUtils.sendDebug("You don't have a hoe in your inventory");
//                return false;
//            }
//            previousTickItemStack = mc.thePlayer.inventory.getStackInSlot(idOfHoe);
//            return false;
//        }
//
//        if ((mc.thePlayer.getHeldItem() != null && !previousTickItemStack.getItem().equals(mc.thePlayer.getHeldItem().getItem()))
//                || (mc.thePlayer.getHeldItem() == null && previousTickItemStack != null)) {
//            emergencyFailsafe(FailsafeType.ITEM_CHANGE);
//            CropUtils.itemChangedByStaff = true;
//            return true;
//        }
//        previousTickItemStack = mc.thePlayer.getHeldItem();
//        return false;
//    }

    /* ==================
          Jacob Check
    =================== */

    public static boolean isJacobFailsafeExceeded = false;

    private static boolean jacobCheck() {
        if (!isJacobFailsafeExceeded) return false;

        if (cooldown.isScheduled() && !cooldown.passed()) {
            LogUtils.sendDebug("Waiting after Jacob failsafe cooldown: " + (String.format("%.1f", cooldown.getRemainingTime() / 1000f)));
            return true;
        }

        if (cooldown.isScheduled() && cooldown.passed()) {
            LogUtils.sendDebug("Jacob failsafe cooldown passed");
            isJacobFailsafeExceeded = false;
            cooldown.reset();
            MacroHandler.enableMacro();
            return false;
        }
        return false;
    }

    public static boolean jacobExceeded() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("Wart") || cleanedLine.contains("Nether")) {
                return gameState.jacobCounter > FarmHelper.config.jacobNetherWartCap;
            } else if (cleanedLine.contains("Mushroom")) {
                return gameState.jacobCounter > FarmHelper.config.jacobMushroomCap;
            } else if (cleanedLine.contains("Carrot")) {
                return gameState.jacobCounter > FarmHelper.config.jacobCarrotCap;
            } else if (cleanedLine.contains("Potato")) {
                return gameState.jacobCounter > FarmHelper.config.jacobPotatoCap;
            } else if (cleanedLine.contains("Wheat")) {
                return gameState.jacobCounter > FarmHelper.config.jacobWheatCap;
            } else if (cleanedLine.contains("Sugar") || cleanedLine.contains("Cane")) {
                return gameState.jacobCounter > FarmHelper.config.jacobSugarCaneCap;
            } else if (cleanedLine.contains("Melon")) {
                return gameState.jacobCounter > FarmHelper.config.jacobMelonCap;
            } else if (cleanedLine.contains("Pumpkin")) {
                return gameState.jacobCounter > FarmHelper.config.jacobPumpkinCap;
            } else if (cleanedLine.contains("Cocoa") || cleanedLine.contains("Bean")) {
                return gameState.jacobCounter > FarmHelper.config.jacobCocoaBeansCap;
            } else if (cleanedLine.contains("Cactus")) {
                return gameState.jacobCounter > FarmHelper.config.jacobCactusCap;
            }
        }
        return false;
    }

    public static long getJacobRemaining() {
        Pattern pattern = Pattern.compile("([0-9]|[1-2][0-9])m([0-9]|[1-5][0-9])s");
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            Matcher matcher = pattern.matcher(cleanedLine);
            if (matcher.find()) {
                return TimeUnit.MINUTES.toMillis(Long.parseLong(matcher.group(1))) + TimeUnit.SECONDS.toMillis(Long.parseLong(matcher.group(2)));
            }
        }
        LogUtils.sendDebug("Failed to get Jacob remaining time");
        return 0;
    }


    /* ==================
          Dirt Check
    =================== */

    private static final ArrayList<Pair<BlockPos, Long>> dirtToCheck = new ArrayList<>();
    private static final ArrayList<BlockPos> dirtPos = new ArrayList<>();

    private boolean dirtCheck() {
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN)
            return false;

        if (!dirtToCheck.isEmpty()) {
            dirtToCheck.removeIf(pair -> pair.second() + 20_000 < System.currentTimeMillis());

            for (Pair<BlockPos, Long> pair : dirtToCheck) {
                if (Math.sqrt(mc.thePlayer.getDistanceSq(pair.first())) < 1.5) {
                    dirtPos.add(pair.first());
                    emergencyFailsafe(FailsafeType.DIRT);
                    dirtToCheck.clear();
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || MacroHandler.currentMacro == null || !MacroHandler.isMacroing || !MacroHandler.currentMacro.enabled || emergency)
            return;

        if (!dirtPos.isEmpty()) {
            dirtPos.clear();
        }

        if (event.update.getBlock().equals(Blocks.dirt)) {
            if (dirtToCheck.stream().noneMatch(pair -> pair.first().equals(event.pos))) {
                dirtToCheck.add(new Pair<>(event.pos, System.currentTimeMillis()));
                LogUtils.sendDebug("Dirt has been put in the world near you");
            }
        }
    }

    /* ==================
          Rotations
    =================== */

    private static final Rotation rotation = new Rotation();

    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    /* ==================
        Rotation Check
    =================== */

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public void onReceivePacket(ReceivePacketEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (evacuateState != EvacuateState.NONE) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
        if (MacroHandler.currentMacro.isTping) return;
        if (AutoReconnect.currentState != AutoReconnect.reconnectingState.NONE) return;
        if (emergency) {
            if (restartAfterFailsafeCooldown.isScheduled() && !restartAfterFailsafeCooldown.passed()) return; // to avoid double messages
            if (event.packet instanceof S08PacketPlayerPosLook && isDuringBedrockCheck) {
                if (config.pingServer && (Pinger.dontRotationCheck.isScheduled() && !Pinger.dontRotationCheck.passed() || Pinger.isOffline)) {
                    LogUtils.sendDebug("Got rotation packet while having bad connection to the server, ignoring");
                    return;
                }

                S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
                if (packet.getY() >= 80) return;
                resetFailsafes();
                if (MacroHandler.currentMacro.enabled) {
                    MacroHandler.disableCurrentMacro();
                    KeyBindUtils.stopMovement();
                    resetFailsafes();
                    LogUtils.sendError("You have just left the bedrock check! You will probably get banned!");
                    LogUtils.webhookLog("You have just left the bedrock check! You will probably get banned!");
                    return;
                }
                rotation.easeTo(MacroHandler.currentMacro.yaw, MacroHandler.currentMacro.pitch, 750);
                restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1_000L);
                LogUtils.sendSuccess("You have just passed the bedrock check! Congratulations!");
                LogUtils.webhookLog("You have just passed the bedrock check! Congratulations!");
            }
            return;
        }

        // Item check
        if (event.packet instanceof S09PacketHeldItemChange) {
            emergencyFailsafe(FailsafeType.ITEM_CHANGE);
            CropUtils.itemChangedByStaff = true;
            return;
        }

        if (VisitorsMacro.isEnabled()) return;
        if (event.packet instanceof S08PacketPlayerPosLook) {
            if (config.pingServer && (Pinger.dontRotationCheck.isScheduled() && !Pinger.dontRotationCheck.passed() || Pinger.isOffline)) {
                LogUtils.sendDebug("Got rotation packet while having bad connection to the server, ignoring");
                return;
            }

            S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;

            // Teleportation check
            Vec3 playerPos = mc.thePlayer.getPositionVector();
            Vec3 teleportPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            if (packet.getY() >= 80) return;
            if ((float) playerPos.distanceTo(teleportPos) >= config.teleportCheckSensitivity) {
                LogUtils.sendDebug("Teleportation check distance: " + playerPos.distanceTo(teleportPos));
                emergencyFailsafe(FailsafeType.TELEPORTATION);
                return;
            }

            // Rotation check
            double yaw = packet.getYaw();
            double pitch = packet.getPitch();
            double threshold = config.rotationCheckSensitivity;
            double previousYaw = mc.thePlayer.rotationYaw;
            double previousPitch = mc.thePlayer.rotationPitch;
            if (Math.abs(yaw - previousYaw) > threshold || Math.abs(pitch - previousPitch) > threshold) {
                emergencyFailsafe(FailsafeType.ROTATION);
            }
        }
    }

    /* ==================
            Utils
    =================== */

    public static void emergencyFailsafe(FailsafeType type) {
        if (AutoReconnect.currentState != AutoReconnect.reconnectingState.NONE) return;
        getAllEmergencies.schedule(1000);
        if (detectedFailsafes.contains(type)) return;
        detectedFailsafes.add(type);
        LogUtils.sendDebug("Emergency added: " + type.toString());
        if (!emergency && type != FailsafeType.DESYNC) { // we want to execute it only once
            if (config.enableFailsafeSound) {
                if (!FarmHelper.config.failsafeSoundType)
                    Utils.playMcFailsafeSound();
                else
                    Utils.playFailsafeSound();
            }
            if (config.autoAltTab) {
                if (config.autoAltTabMode == 1)
                    Utils.bringWindowToFrontWinApi();
                else
                    Utils.bringWindowToFront();
            }
        }
        if (config.enableScheduler)
            Scheduler.pause();
        if (type == FailsafeType.BEDROCK)
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false); // temp fix
        emergency = true;
    }

    // take action based on the failsafe with the highest priority
    public static void takeAction(FailsafeType type) {
        LogUtils.sendFailsafeMessage(type.label);
        LogUtils.webhookLog(type.label);

        if (config.popUpNotification) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Utils.createNotification(type.label, tray, TrayIcon.MessageType.WARNING);
            } catch (UnsupportedOperationException e) {
                LogUtils.sendError("Notifications are not supported on this system!");
            }
        }

        switch (type) {
            case DESYNC:
                cooldown.schedule(3_000);
                evacuateState = EvacuateState.EVACUATE_FROM_ISLAND;
                Autosell.disableOnly();
                VisitorsMacro.stopMacro();
                if (config.enableScheduler)
                    Scheduler.pause();
                MacroHandler.disableCurrentMacro(config.setSpawnBeforeEvacuate);
                KeyBindUtils.stopMovement();
                return;
            default:
                emergencyThreadExecutor.submit(delayedStopScript);
                LogUtils.sendFailsafeMessage("Act like a normal player and stop the script!");

        }

        if (type == FailsafeType.BEDROCK)
            isDuringBedrockCheck = true;

        if (config.fakeMovements) {
            switch (type) {
                case BEDROCK:
                    emergencyThreadExecutor.submit(cagedActing);
                    return;
                case DIRT:
                case TELEPORTATION:
                case ROTATION:
                    emergencyThreadExecutor.submit(rotationMovement);
                    return;
                case ITEM_CHANGE:
                    emergencyThreadExecutor.submit(itemChange);
            }
        } else {
            emergencyThreadExecutor.submit(pauseAndRestartLong);
        }
    }

    /* ==================
          Runnables
    =================== */

    private static final Runnable delayedStopScript = () -> {
        try {
            LogUtils.sendDebug("delayedStopScript: waiting");
            Thread.sleep((long) ((config.delayedStopScriptTime * 1_000) + Math.random() * (config.delayedStopScriptTimeRandomness * 1_000)));
            MacroHandler.disableCurrentMacro(true);
            LogUtils.sendDebug("delayedStopScript: done, stopping macro");
        } catch (Exception ignored) {
//            e.printStackTrace();
        }
    };

    private static final Runnable bazaarChilling = () -> {
        try {
            UngrabUtils.ungrabMouse();
            LogUtils.sendWarning("Gonna chill for a bit (we don't want to get banned)");
            LogUtils.webhookLog("Gonna chill in hub for about 5 minutes");

            rotation.reset();
            rotation.easeTo(103f, -11f, 1000);
            Thread.sleep(1500);
            KeyBindUtils.updateKeys(true, false, false, false, false, true, false);
            long timeout = System.currentTimeMillis();
            boolean timedOut = false;
            while (BlockUtils.getRelativeBlock(0, 0, 1) != Blocks.spruce_stairs) {
                if ((System.currentTimeMillis() - timeout) > 10000) {
                    LogUtils.sendWarning("Couldn't find bz, gonna chill here");
                    timedOut = true;
                    break;
                }
            }
            stopMovement();

            if (!timedOut) {
                // about 5 minutes
                for (int i = 0; i < 15; i++) {
                    Thread.sleep(6000);
                    KeyBindUtils.rightClick();
                    Thread.sleep(3000);
                    PlayerUtils.clickOpenContainerSlot(11);
                    Thread.sleep(3000);
                    PlayerUtils.clickOpenContainerSlot(11);
                    Thread.sleep(3000);
                    PlayerUtils.clickOpenContainerSlot(10);
                    Thread.sleep(3000);
                    PlayerUtils.clickOpenContainerSlot(10);
                    Thread.sleep(3000);
                    mc.thePlayer.closeScreen();
                }
            } else {
                Thread.sleep(1000 * 60 * 5);
            }
            finishEmergency(false);

        } catch (Exception ignored) {
        }
    };

    private static final Runnable gameMenuChilling = () -> {
        try {
            LogUtils.sendWarning("Gonna chill for a bit in game menu");
            LogUtils.webhookLog("Gonna chill for a bit in game menu");
            PlayerUtils.getItemInHotbar("Game Menu");
            if (mc.thePlayer.inventory.currentItem != 8)
                mc.thePlayer.inventory.currentItem = 8;
            Thread.sleep((long) (300 + Math.random() * 300));
            KeyBindUtils.rightClick();
        } catch (Exception ignored) {
        }
    };

    private static final Runnable rotationMovement = () -> {
        try {
            LogUtils.sendDebug("rotationMovement: waiting");
            Thread.sleep((long) (1000 + Math.random() * 1000));

            long rotationTime;
            String messageChosen;

            // stage 1: look around and move to back (bonus random crouch)
            LogUtils.sendDebug("rotationMovement: stage 1");
            if (dirtPos.size() == 0) {
                rotationTime = (long) ((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
                rotation.easeTo((float) (300 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);

                Thread.sleep(rotationTime + 200);
                KeyBindUtils.updateKeys(false, true, false, false, false, false, false);
                Thread.sleep((long) (500 + Math.random() * 500));
                KeyBindUtils.stopMovement();
            }

            // exeception stage: if have a dirt check then get pos of dirt and look at it
            if (dirtPos.size() > 0) {
                LogUtils.sendDebug("rotationMovement: stage 1.5");
                for (BlockPos dirt : dirtPos) {
                    LogUtils.sendDebug("rotationMovement: stage 1.5: dirt found");

                    // get yaw and pitch to dirt
                    double x = mc.thePlayer.posX - dirt.getX();
                    double y = mc.thePlayer.posY - dirt.getY();
                    double z = mc.thePlayer.posZ - dirt.getZ();

                    float yaw = (float) Math.atan2(z, -x);
                    float pitch = (float) Math.atan2(-y, Math.sqrt(x * x + z * z));

                    rotation.easeTo(yaw, pitch, 450);

                    // move back
                    KeyBindUtils.updateKeys(false, true, false, false, true, false, false);
                    Thread.sleep((long) (500 + Math.random() * 500));
                    KeyBindUtils.stopMovement();
                }
                dirtPos.clear();

                // random try break dirt block
                if (Math.random() < 0.5f) {
                    Thread.sleep((long) (200 + Math.random() * 500));
                    KeyBindUtils.updateKeys(false, false, false, false, true, false, false);
                    Thread.sleep(300);
                    KeyBindUtils.stopMovement();
                }

                Thread.sleep((long) (1000 + Math.random() * 1000));
            }

            for (int i = 0; i < config.rotationActingTimes; i++) {
                rotationTime = (long) ((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
                rotation.easeTo((float) (360 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                if (Math.random() < 0.1f) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
                    Thread.sleep(300);
                    KeyBindUtils.stopMovement();
                }
                if (Math.random() < 0.5f) {
                    KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, false, false, false, false, false);
                    Thread.sleep(250);
                    KeyBindUtils.stopMovement();
                }
                Thread.sleep(rotationTime + 200);
            }

            // stage 2: send a message
            LogUtils.sendDebug("rotationMovement: stage 2");
            if (!Config.customRotationMessages.isEmpty()) {
                String[] messages = Config.customRotationMessages.split("\\|");
                if (messages.length > 1) {
                    messageChosen = messages[(int) Math.floor(Math.random() * (messages.length - 1))];
                    Thread.sleep((long) ((messageChosen.length() * 250L) + Math.random() * 500));

                } else {
                    messageChosen = Config.customRotationMessages;
                    Thread.sleep((long) ((Config.customRotationMessages.length() * 250L) + Math.random() * 500));
                }
            } else {
                messageChosen = FAILSAFE_MESSAGES[(int) Math.floor(Math.random() * (FAILSAFE_MESSAGES.length - 1))];
                Thread.sleep((long) ((messageChosen.length() * 250) + Math.random() * 500));
            }
            mc.thePlayer.sendChatMessage(messageChosen);

            // stage 3: look around again and jump
            LogUtils.sendDebug("rotationMovement: stage 3");
            for (int i = 0; i < config.rotationActingTimes; i++) {
                rotationTime = (long) ((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
                rotation.easeTo((float) (340 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                Thread.sleep(rotationTime + 200);
                // random crouch
                if (Math.random() < 0.1f) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
                    // random jump
                } else if (Math.random() < 0.1f) {
                    Thread.sleep(200);
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
                } else {
                    KeyBindUtils.stopMovement();
                }
            }

            // final stage: come hub or quit game
            LogUtils.sendDebug("rotationMovement: final stage");
            LogUtils.sendFailsafeMessage("Stop the macro if you see this!");
            finishEmergency(true);
        } catch (InterruptedException ignored) {
//            e.printStackTrace();
        }
    };

    private static final Runnable pauseAndRestartLong = () -> {
        try {
            LogUtils.sendDebug("pauseAndRestartLong: waiting");
            Thread.sleep((long) (25_000 + Math.random() * (config.delayedStopScriptTimeRandomness * 1_000)));
            LogUtils.sendDebug("pauseAndRestartLong: done, restarting the macro");
            finishEmergency(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    private static final Runnable itemChange = () -> {
        try {
            Thread.sleep(1000 + new Random().nextInt(1000));
            MacroHandler.disableCurrentMacro(true);
            int numberOfRepeats = new Random().nextInt(2) + 2;
            boolean sneak = Math.random() < 0.3d;
            Thread.sleep(new Random().nextInt(300) + 300);
            if (Math.random() < 0.3f)
                mc.thePlayer.jump();
            else if (Math.random() < 0.1f) {
                for (int i = 0; i < new Random().nextInt(2) + 1; i++) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                    Thread.sleep(150 + new Random().nextInt(300));
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                    Thread.sleep(150 + new Random().nextInt(300));
                }
            }
            Thread.sleep(new Random().nextInt(200) + 150);

            for (int i = 0; i < numberOfRepeats; i++) {
                Thread.sleep(new Random().nextInt(150) + 200);
                Tuple<Float, Float> targetRotation = new Tuple<>(mc.thePlayer.rotationYaw + new Random().nextInt(100) - 50, Math.min(Math.max(mc.thePlayer.rotationPitch + new Random().nextInt(100) - 50, -45), 45));
                int timeToRotate = new Random().nextInt(250) + 250;
                rotation.easeTo(targetRotation.getFirst(), targetRotation.getSecond(), timeToRotate);
                Thread.sleep(timeToRotate + new Random().nextInt(250));
                if (sneak && new Random().nextInt(3) == 0) {
                    for (int j = 0; j < new Random().nextInt(3); j++) {
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                        Thread.sleep(120 + new Random().nextInt(200));
                        KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                        Thread.sleep(150);
                    }
                }
            }
            finishEmergency(true);
        } catch (InterruptedException ignored) {
//            e.printStackTrace();
        }
    };

    private static final Runnable cagedActing = () -> {
        LogUtils.sendDebug("You just got caged bozo! Buy a lottery ticket!");
        LogUtils.webhookLog("You just got caged bozo! Buy a lottery ticket! @everyone");

        try {
            Thread.sleep((long) (3000 + Math.random() * 1000));
            String messageChosen = "";
            boolean said = false;
            long rotationTime;

            // stage 1: look around (random chance)
            if (Math.random() < 0.5f) {
                for (int i = 0; i < 3; i++) {
                    rotationTime = (long) (350 * (Math.random() + 1));
                    rotation.easeTo((float) (270 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                }
            }

            // stage 2: look around and send messages
            for (int i = 0; i < config.bedrockActingTimes; i++) {
                rotationTime = (long) ((Math.random() * (config.rotationTimeRandomness * 1_000)) + (config.rotationTime * 1_000));
                rotation.easeTo((float) (270 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                if (i == 0) {
                    KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                }

                if (Math.random() < 0.5d && !said) {
                    said = true;
                    KeyBindUtils.stopMovement();
                    Thread.sleep(rotationTime + 1_500);

                    if (!Config.customBedrockMessages.isEmpty()) {
                        String[] messages = Config.customBedrockMessages.split("\\|");
                        if (messages.length > 1) {
                            messageChosen = messages[(int) Math.floor(Math.random() * (messages.length - 1))];
                            Thread.sleep((long) ((messageChosen.length() * 250L) + Math.random() * 500));

                        } else {
                            messageChosen = Config.customBedrockMessages;
                            Thread.sleep((long) ((Config.customBedrockMessages.length() * 250L) + Math.random() * 500));
                        }
                    } else {
                        messageChosen = FAILSAFE_MESSAGES[(int) Math.floor(Math.random() * (FAILSAFE_MESSAGES.length - 1))];
                        Thread.sleep((long) ((messageChosen.length() * 250) + Math.random() * 500));
                    }
                    mc.thePlayer.sendChatMessage(messageChosen);

                    Thread.sleep((long) (600 + Math.random() * 500));
                } else while (rotation.rotating) {
                    if (i == 0) {
                        KeyBindUtils.updateKeys(Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, false, false);
                        rotation.reset();
                        stopMovement();
                        Thread.sleep((long) (Math.random() * 450));
                        break;
                    } else {
                        // around 0.3/0.6s long movements (the more random the better)
                        Thread.sleep((long) (100 * (Math.random() + 2)));
                        KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                    }
                }
            }

            // stage 3: rewarp and start the macro again if it takes too long
            stopMovement();
            Thread.sleep((long) (30_000 + Math.random() * 5_000));
            finishEmergency(false);

        } catch (InterruptedException ignored) {
//            e.printStackTrace();
        }
    };

    private static void finishEmergency(boolean shouldRestoreRotation) {
        KeyBindUtils.stopMovement();
        resetFailsafes();
        if (!config.leaveAfterFailSafe) {
            if (shouldRestoreRotation) {
                LogUtils.sendDebug("Restoring rotation before failsafe");
                rotation.easeTo(MacroHandler.currentMacro.yaw, MacroHandler.currentMacro.pitch, 750);
            } else {
                MacroHandler.currentMacro.isTping = true;
                mc.thePlayer.sendChatMessage("/warp garden");
            }
            restartAfterFailsafeCooldown.schedule(config.restartAfterFailSafeDelay * 1000L);
        } else {
            mc.theWorld.sendQuittingDisconnectingPacket();
        }
    }
}
