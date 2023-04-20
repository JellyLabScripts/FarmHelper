package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.config.interfaces.JacobConfig;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.awt.image.BufferedImage;
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
    private static String formattedTime;

    private static boolean wasInGarden = false;
    private static final String[] messages = new String[]
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
        }
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {

        if (!MacroHandler.isMacroing || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;


        if(!emergency && BlockUtils.bedrockCount() > 2){
            emergencyFailsafe(FailsafeType.BEDROCK);
            return;
        }

        GameState.location location = gameState.currentLocation;

        if(location != GameState.location.ISLAND && MacroHandler.currentMacro.enabled){
            if(FailsafeConfig.notifications)
                createNotification("Not in island", SystemTray.getSystemTray(), TrayIcon.MessageType.WARNING);
            LogUtils.scriptLog("Failsafe - Not in island, re-warping" + (FailsafeConfig.autoSetspawn ? "" : ". Since auto setspawn was disabled, fly back to the place where you started"));
            MacroHandler.disableCurrentMacro();
        }

        switch (location) {
            case TELEPORTING:
                return;
            case LIMBO:
                if (cooldown.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule(5000);
                }
                return;
            case LOBBY:
                if (cooldown.passed() && jacobWait.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    cooldown.schedule(5000);
                }
                return;
            case HUB:
                LogUtils.debugLog("Detected Hub");
                if (cooldown.passed() && jacobWait.passed() && !AutoCookie.isEnabled() && !AutoPot.isEnabled()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage(wasInGarden ? "/warp garden" : "/is");
                    cooldown.schedule(5000);
                }
                return;
            case ISLAND:
                checkInGarden();
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
                        && !emergency) {


                    MacroHandler.enableCurrentMacro();
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

    //private static Clock eDisable = new Clock();

    private static ExecutorService emergencyThreadExecutor = Executors.newScheduledThreadPool(5);


    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event){

        if(mc.thePlayer == null || mc.theWorld == null || MacroHandler.currentMacro == null || !MacroHandler.isMacroing || !MacroHandler.currentMacro.enabled || emergency)
            return;

        if(gameState.currentLocation != GameState.location.ISLAND || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame))
            return;

        if(event.old != null && BlockUtils.isWalkable(event.old.getBlock()) && event.update.getBlock().equals(Blocks.dirt))
            emergencyFailsafe(FailsafeType.DIRT);
    }
    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
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
        DESYNC("You are desynced. You might be lagging or there might be a staff spectating");

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

        if(type != FailsafeType.DIRT && type != FailsafeType.DESYNC) // you may not be able to see the dirt, disable a few seconds later
            MacroHandler.disableCurrentMacro();

        SystemTray tray = SystemTray.getSystemTray();

        if(FailsafeConfig.notifications){
            createNotification(type.label, tray, TrayIcon.MessageType.WARNING);
        }

        if(FailsafeConfig.pingSound) {
            Utils.sendPingAlert();
        }

        if(FailsafeConfig.fakeMovements) {
            switch (type){
                case DIRT: case DESYNC:
                    emergencyThreadExecutor.submit(stopScript);
                    break;
                case ROTATION:
                    emergencyThreadExecutor.submit(rotationMovement);
                    break;
                case BEDROCK:
                    emergencyThreadExecutor.submit(cagedActing);
                    break;
            }
        }
    }

    public static void createNotification(String text, SystemTray tray, TrayIcon.MessageType messageType) {
        new Thread(() -> {
            TrayIcon trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), "Farm Helper Failsafe Notification");
            trayIcon.setToolTip("Farm Helper Failsafe Notification");
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }

            trayIcon.displayMessage("Farm Helper - Failsafes", text, messageType);
        }).start();

    }

    static Runnable stopScript = () -> {
        try{
            Thread.sleep((long) (2000 + Math.random() * 1000));
            MacroHandler.disableCurrentMacro();
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
                    mc.thePlayer.sendChatMessage(messages[(int) Math.floor(Math.random() * (messages.length - 1))]);//"/ac " +
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