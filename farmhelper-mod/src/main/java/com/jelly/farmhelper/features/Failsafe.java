package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.config.interfaces.JacobConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;

public class Failsafe {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Clock cooldown = new Clock();
    public static final Clock jacobWait = new Clock();
    private static final Clock evacuateCooldown = new Clock();
    private static final Clock afterEvacuateCooldown = new Clock();
    public static final Clock restartAfterFailsafeCooldown = new Clock();
    private static String formattedTime;

    private static boolean wasInGarden = false;
    private static final String[] FAILSAFE_MESSAGES = new String[]
            {"What", "what?", "what", "what??", "What???", "Wut?", "?", "what???", "yo huh", "yo huh?", "yo?", "bedrock??", "bedrock?",
                    "ehhhhh??", "eh", "yo", "ahmm", "ehh", "LOL what", "Lol", "lol", "lmao", "Lmfao", "lmfao"
                    ,"wtf is this", "wtf", "WTF", "wtf is this?", "wtf???", "tf", "tf?", "wth",
                    "lmao what?", "????", "??", "???????", "???", "UMMM???", "Umm", "ummm???", "damn wth", "Dang it", "Damn", "damn wtf", "damn",
                    "hmmm", "hm", "sus", "hmm", "Ok?", "ok?", "again lol", "again??", "ok damn"};


    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (MacroHandler.isMacroing) {
            if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
                LogUtils.debugLog("Failed teleport - waiting");
                cooldown.schedule(10000);
            }
            if (message.contains("to warp out! CLICK to warp now!")) {
                if (FailsafeConfig.setSpawnBeforeEvacuate)
                    PlayerUtils.setSpawn();
                LogUtils.debugLog("Update or restart is required - Evacuating in 5s");
                evacuateCooldown.schedule(5_000);
                MacroHandler.disableCurrentMacro(true);
                KeyBindUtils.stopMovement();
            }
            if (message.contains("Your spawn location has been set!")) {
                LogUtils.debugLog("Spawn set correctly");
            }
        }
        if (message.contains("You cannot set your spawn here!")) {
            LogUtils.debugLog("Spawn set incorrectly");
        }
    }

    @SubscribeEvent
    public final void onUnloadWorld(WorldEvent.Unload event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!MacroHandler.isMacroing) return;
        if (gameState.currentLocation != GameState.location.ISLAND && MacroHandler.currentMacro.enabled) {
            if(FailsafeConfig.notifications)
                Utils.createNotification("Not in island. It might be a server reboot or staff check." +
                        (FailsafeConfig.autoTpOnWorldChange ? "" : " Please go back to your island and restart the script."), SystemTray.getSystemTray(), TrayIcon.MessageType.WARNING);

            LogUtils.scriptLog("Failsafe - Not in island. It might be a server reboot or staff check." +
                    (FailsafeConfig.autoTpOnWorldChange ? "" : " Please go back to your island and restart the script." +
                            (FailsafeConfig.autoSetspawn ? "" : " Since auto setspawn was disabled, fly back to the place where you started")));
            
            MacroHandler.disableCurrentMacro();
            cooldown.schedule((long) (3000 + Math.random() * 3000));
        }

    }

    public static final Clock waitBetweenSendingInfo = new Clock();

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        // Mustario is a grape

        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        if (restartAfterFailsafeCooldown.isScheduled()) {
            if (waitBetweenSendingInfo.isScheduled() && waitBetweenSendingInfo.passed()) {
                LogUtils.debugLog("Waiting to restart macro: " + (String.format("%.1f", restartAfterFailsafeCooldown.getRemainingTime() / 1000f)) + "s");
            } else if (!waitBetweenSendingInfo.passed()) {
                waitBetweenSendingInfo.schedule(15_000);
            }

            if (restartAfterFailsafeCooldown.passed()) {
                LogUtils.debugLog("Restarting macro. 3 minutes after failsafe are passed");
                MacroHandler.enableMacro();
                restartAfterFailsafeCooldown.reset();
            }
        }

        if (!MacroHandler.isMacroing) return;

        if (dirtToCheck != null) {
            frustum.setPosition(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);
            if (frustum.isBoundingBoxInFrustum(new AxisAlignedBB(dirtToCheck.getX(), dirtToCheck.getY(), dirtToCheck.getZ(), dirtToCheck.getX() + 1, dirtToCheck.getY() + 1, dirtToCheck.getZ() + 1))) {
                emergencyFailsafe(FailsafeType.DIRT);
            }
            if (dirtCheck.isScheduled() && dirtCheck.passed()) {
                dirtToCheck = null;
                dirtCheck.reset();
            }
        }


        if(!emergency && BlockUtils.bedrockCount() > 2){
            emergencyFailsafe(FailsafeType.BEDROCK);
            return;
        }

        GameState.location location = gameState.currentLocation;



        switch (location) {
            case TELEPORTING:
                return;
            case LIMBO:
                if(!FailsafeConfig.autoTpOnWorldChange) return;
                if (cooldown.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule(5000);
                }
                return;
            case LOBBY:
                if(!FailsafeConfig.autoTpOnWorldChange) return;
                if (cooldown.passed() && jacobWait.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    cooldown.schedule(7500);
                    afterEvacuateCooldown.schedule(7500);
                }
                return;
            case HUB:
                if(!FailsafeConfig.autoTpOnWorldChange) return;
                LogUtils.debugLog("Detected Hub");
                if (cooldown.passed() && jacobWait.passed() && !AutoCookie.isEnabled() && !AutoPot.isEnabled()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage(wasInGarden ? "/warp garden" : "/is");
                    cooldown.schedule(5000);
                } else if (afterEvacuateCooldown.isScheduled() && !afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("Waiting for \"after evacuate\" cooldown: " + (String.format("%.1f", afterEvacuateCooldown.getRemainingTime() / 1000f)));
                }
                return;
            case ISLAND:
                checkInGarden();
                if (evacuateCooldown.isScheduled() && evacuateCooldown.passed()) {
                    LogUtils.debugLog("Evacuating");
                    mc.thePlayer.sendChatMessage("/evacuate");
                    evacuateCooldown.reset();
                    cooldown.schedule(5000);
                    afterEvacuateCooldown.schedule(15_000);
                }
                if (JacobConfig.jacobFailsafe && jacobExceeded() && jacobWait.passed() && MacroHandler.currentMacro.enabled) {
                    LogUtils.debugLog("Jacob remaining time: " + formattedTime);
                    LogUtils.webhookLog("Jacob score exceeded - - Resuming in " + formattedTime);
                    jacobWait.schedule(getJacobRemaining());
                    mc.theWorld.sendQuittingDisconnectingPacket();
                } else if (!MacroHandler.currentMacro.enabled
                        && jacobWait.passed()
                        && !Autosell.isEnabled()
                        && !MacroHandler.startingUp
                        && Scheduler.isFarming()
                        && !AutoCookie.isEnabled()
                        && !AutoPot.isEnabled()
                        && !(BanwaveChecker.banwaveOn && FailsafeConfig.banwaveDisconnect)
                        && !emergency
                        && !evacuateCooldown.isScheduled()
                        && !afterEvacuateCooldown.isScheduled()
                        && !restartAfterFailsafeCooldown.isScheduled()
                        && (!MiscConfig.visitorsMacro || !VisitorsMacro.isEnabled())) {

                    LogUtils.debugLog("Resuming macro");
                    MacroHandler.enableCurrentMacro();
                } else if (afterEvacuateCooldown.isScheduled() && !afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("Waiting for \"after entering island\" cooldown: " + (String.format("%.1f", afterEvacuateCooldown.getRemainingTime() / 1000f)));
                } else if (afterEvacuateCooldown.isScheduled() && afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("\"After entering island\" cooldown passed");
                    afterEvacuateCooldown.reset();
                }
        }
    }

    public static void checkInGarden(){
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("Island")) {
                wasInGarden = false;
            } else if(cleanedLine.contains("Garden") || cleanedLine.contains("Plot:"))
                wasInGarden = true;
        }
    }


    public static boolean jacobExceeded() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if (cleanedLine.contains("Wart") || cleanedLine.contains("Nether")) {
                return gameState.jacobCounter > JacobConfig.netherWartCap;
            } else if (cleanedLine.contains("Mushroom")) {
                return gameState.jacobCounter > JacobConfig.mushroomCap;
            } else if (cleanedLine.contains("Carrot")) {
                return gameState.jacobCounter > JacobConfig.carrotCap;
            } else if (cleanedLine.contains("Potato")) {
                return gameState.jacobCounter > JacobConfig.potatoCap;
            } else if (cleanedLine.contains("Wheat")) {
                return gameState.jacobCounter > JacobConfig.wheatCap;
            } else if (cleanedLine.contains("Sugar") || cleanedLine.contains("Cane") ) {
                return gameState.jacobCounter > JacobConfig.sugarcaneCap;
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
                formattedTime = matcher.group(1) + "m" + matcher.group(2) + "s";
                return TimeUnit.MINUTES.toMillis(Long.parseLong(matcher.group(1))) + TimeUnit.SECONDS.toMillis(Long.parseLong(matcher.group(2)));
            }
        }
        LogUtils.debugLog("Failed to get Jacob remaining time");
        return 0;
    }



    // region emergency
    private static final Rotation rotation = new Rotation();
    static Thread bzchillingthread;
    public static boolean emergency = false;
    private static final Frustum frustum = new Frustum();

    //private static Clock eDisable = new Clock();

    private static ExecutorService emergencyThreadExecutor = Executors.newScheduledThreadPool(5);
    private static BlockPos dirtToCheck = null;
    private static final Clock dirtCheck = new Clock();


    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event){

        if(mc.thePlayer == null || mc.theWorld == null || MacroHandler.currentMacro == null || !MacroHandler.isMacroing || !MacroHandler.currentMacro.enabled || emergency)
            return;

        if(gameState.currentLocation != GameState.location.ISLAND || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame))
            return;

        if(event.old != null && BlockUtils.isWalkable(event.old.getBlock()) && event.update.getBlock().equals(Blocks.dirt)) {
            if (mc.thePlayer.getDistanceSqToCenter(event.pos) < 10) {
                dirtToCheck = event.pos;
                dirtCheck.schedule(10_000);
            }
        }

    }
    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @SubscribeEvent
    public void onItemChange(ReceivePacketEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (evacuateCooldown.isScheduled() || afterEvacuateCooldown.isScheduled()) return;
        if (gameState.currentLocation != GameState.location.ISLAND) return;
        if (event.packet instanceof S09PacketHeldItemChange) {
            emergencyFailsafe(FailsafeType.ITEM_CHANGE);
            CropUtils.itemChangedByStaff = true;
        }
    }

    public static void stopAllFailsafeThreads() {
        emergencyThreadExecutor.shutdownNow();
        emergencyThreadExecutor = Executors.newScheduledThreadPool(2);
        rotation.reset();
        emergency = false;

    }

    public enum FailsafeType {
        DIRT ("You may have been dirt checked"),
        BEDROCK ("You have been bedrock checked"),
        ROTATION ("You may have been rotation checked or you may have moved your mouse"),
        DESYNC("You are desynced. " +
                "You might be lagging or there might be a staff spectating. If this is happening frequently, disable check desync"),
        ITEM_CHANGE("Your item has been probably changed."),
        TEST("Its just a test failsafe. (Its Safe to Ignore)");

        final String label;
        FailsafeType(String s) {
            this.label = s;
        }
    }

    public static void emergencyFailsafe(FailsafeType type) {

        emergency = true;

        LogUtils.webhookLog(type.label);
        LogUtils.scriptLog(type.label + "!");
        LogUtils.scriptLog("Act like a normal player and stop the script!");

        if(type != FailsafeType.DIRT && type != FailsafeType.DESYNC && type != FailsafeType.ITEM_CHANGE) // you may not be able to see the dirt, disable a few seconds later
            MacroHandler.disableCurrentMacro(true);

        if(FailsafeConfig.notifications){
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Utils.createNotification(type.label, tray, TrayIcon.MessageType.WARNING);
            } catch (UnsupportedOperationException e) {
                LogUtils.scriptLog("Notifications are not supported on this system");
            }
        }

        if(FailsafeConfig.autoFocusOnStaffCheck) {
            Utils.bringWindowToFront();
        }

        if (FailsafeConfig.pingSound) {
            Utils.sendPingAlert();
        }

        if (FailsafeConfig.fakeMovements) {
            switch (type){
                case DESYNC:
                    emergencyThreadExecutor.submit(stopScript);
                    break;
                case DIRT: case ROTATION:
                    emergencyThreadExecutor.submit(rotationMovement);
                    break;
                case BEDROCK:
                    emergencyThreadExecutor.submit(cagedActing);
                    break;
                case ITEM_CHANGE:
                    emergencyThreadExecutor.submit(itemChange);
                    break;
            }
        }

        if (FailsafeConfig.restartAfterFailsafe)
            restartAfterFailsafeCooldown.schedule(180_000);
    }

    static Runnable stopScript = () -> {
        try{
//            Thread.sleep((long) (2000 + Math.random() * 1000));
            MacroHandler.disableCurrentMacro(true);
        } catch(Exception e){
            e.printStackTrace();
        }
    };

    static Runnable bazaarChilling = () -> {
        try {
            UngrabUtils.ungrabMouse();
            LogUtils.scriptLog("Gonna chill for a bit (we don't want to get banned)");
            LogUtils.webhookLog("Gonna chill in hub for about 5 minutes");

            rotation.reset();
            rotation.easeTo(103f, -11f, 1000);
            Thread.sleep(1500);
            KeyBindUtils.updateKeys(true, false, false, false, false, true, false);
            long timeout = System.currentTimeMillis();
            boolean timedOut = false;
            while (BlockUtils.getRelativeBlock(0, 0, 1) != Blocks.spruce_stairs) {
                if ((System.currentTimeMillis() - timeout) > 10000) {
                    LogUtils.scriptLog("Couldn't find bz, gonna chill here");
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
            mc.thePlayer.sendChatMessage("/is");
            Thread.sleep(6000);
            MacroHandler.enableMacro();
        } catch (Exception ignored) {}

    };


    static Runnable rotationMovement = () -> {
        try {
            int numberOfRepeats = new Random().nextInt(2) + 2;
            boolean sneak = Math.random() < 0.3d;
            Thread.sleep(new Random().nextInt(300) + 300);
            if (Math.random() < 0.3f)
                mc.thePlayer.jump();
            else if (Math.random() < 0.1f){
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
            LogUtils.scriptLog("Stop the macro if you see this!");
            PlayerUtils.setSpawn();
            Thread.sleep(3000);
            if(Math.random() < 0.5d) {
                mc.thePlayer.sendChatMessage("/hub");
                Thread.sleep(1000);
                if (FarmHelper.gameState.currentLocation == GameState.location.HUB) {
                    emergencyThreadExecutor.submit(bazaarChilling);
                } else {
                    Thread.sleep(1000 * 60 * 5);
                    MacroHandler.enableMacro();
                }
            } else mc.theWorld.sendQuittingDisconnectingPacket();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    static Runnable itemChange = () -> {
        try {
            Thread.sleep(1000 + new Random().nextInt(1000));
            MacroHandler.disableCurrentMacro(true);
            int numberOfRepeats = new Random().nextInt(2) + 2;
            boolean sneak = Math.random() < 0.3d;
            Thread.sleep(new Random().nextInt(300) + 300);
            if (Math.random() < 0.3f)
                mc.thePlayer.jump();
            else if (Math.random() < 0.1f){
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
            MacroHandler.enableMacro();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };


    static Runnable cagedActing = () -> {
        LogUtils.webhookLog("You just got caged bozo! Buy a lottery ticket! @everyone");

        if (bzchillingthread != null) {
            bzchillingthread.interrupt();
        }
        try {
            Thread.sleep((long) (Math.random() * 1000));
            boolean said = false;
            for (int i = 0; i < 3; i++) {
                long rotationTime = (long) (800 * (Math.random() + 1));
                rotation.easeTo((float) (270 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                if(Math.random() < 0.5d && !said) {
                    said = true;
                    KeyBindUtils.stopMovement();
                    Thread.sleep(rotationTime + 2800);
                    mc.thePlayer.sendChatMessage(FAILSAFE_MESSAGES[(int) Math.floor(Math.random() * (FAILSAFE_MESSAGES.length - 1))]);//"/ac " +
                    Thread.sleep((long) (200 + Math.random() * 500));
                } else while (rotation.rotating) {
                    if (i == 0) {
                        KeyBindUtils.updateKeys(Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, Math.random() < 0.3, false, false);
                        Thread.sleep(500);
                        rotation.reset();
                        stopMovement();
                        Thread.sleep((long) (Math.random() * 1000));
                        break;
                    } else {
                        // around 0.3/0.6s long movements (the more random the best)
                        Thread.sleep((long) (100 * (Math.random() + 2)));
                        KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                    }
                }
            }
            stopMovement();
            Thread.sleep((long) (500 + Math.random() * 2000));
            if(Math.random() < 0.5d) {
                mc.thePlayer.sendChatMessage("/hub");
                Thread.sleep(5000);
                if (FarmHelper.gameState.currentLocation == GameState.location.HUB) {
                    emergencyThreadExecutor.submit(bazaarChilling);
                } else {
                    Thread.sleep(1000 * 60 * 5);
                    MacroHandler.enableMacro();
                }
            } else mc.theWorld.sendQuittingDisconnectingPacket();



        } catch (Exception ignored) {}
    };



}