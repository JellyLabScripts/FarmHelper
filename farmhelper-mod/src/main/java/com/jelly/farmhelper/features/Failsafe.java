package com.jelly.farmhelper.features;

import akka.japi.Pair;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
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

public class Failsafe {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Clock cooldown = new Clock();
    public static final Clock jacobWait = new Clock();
    private static final Clock evacuateCooldown = new Clock();
    private static final Clock afterEvacuateCooldown = new Clock();
    public static Clock restartAfterFailsafeCooldown = new Clock();
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
                if (FarmHelper.config.setSpawnBeforeEvacuate)
                    PlayerUtils.setSpawn();
                LogUtils.debugLog("Update or restart is required - Evacuating in 5s");
                evacuateCooldown.schedule(2_500);
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
            if(FarmHelper.config.popUpNotification)
                Utils.createNotification("Not in island. It might be a server reboot or staff check." +
                        (FarmHelper.config.autoTPOnWorldChange ? "" : " Please go back to your island and restart the script."), SystemTray.getSystemTray(), TrayIcon.MessageType.WARNING);

            LogUtils.scriptLog("Failsafe - Not in island. It might be a server reboot or staff check." +
                    (FarmHelper.config.autoTPOnWorldChange ? "" : " Please go back to your island and restart the script." +
                            (FarmHelper.config.enableAutoSetSpawn ? "" : " Since auto setspawn was disabled, fly back to the place where you started")));
            
            MacroHandler.disableCurrentMacro();
            cooldown.schedule((long) (6000 + Math.random() * 5000));
        }

    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!FarmHelper.config.debugMode) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (restartAfterFailsafeCooldown.isScheduled() && !restartAfterFailsafeCooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("restartAfterFailsafeCooldown: " + restartAfterFailsafeCooldown.getRemainingTime(), 300, 2, Color.WHITE.getRGB());
        }

        if (evacuateCooldown.isScheduled() && !evacuateCooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("evacuateCooldown: " + evacuateCooldown.getRemainingTime(), 300, 12, Color.WHITE.getRGB());
        }

        if (afterEvacuateCooldown.isScheduled() && !afterEvacuateCooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("afterEvacuateCooldown: " + afterEvacuateCooldown.getRemainingTime(), 300, 22, Color.WHITE.getRGB());
        }

        if (cooldown.isScheduled() && !cooldown.passed()) {
            mc.fontRendererObj.drawStringWithShadow("cooldown: " + cooldown.getRemainingTime(), 300, 32, Color.WHITE.getRGB());
        }
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        // Mustario is a grape

        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        if (restartAfterFailsafeCooldown.isScheduled()) {
            if (restartAfterFailsafeCooldown.passed()) {
                LogUtils.debugLog("Restarting macro. " + FarmHelper.config.restartAfterFailSafeDelay + " seconds after failsafe is passed");
                emergency = false;
                restartAfterFailsafeCooldown = new Clock();
                MacroHandler.enableMacro();
            }
        }

        if (!MacroHandler.isMacroing) return;

        if (!dirtToCheck.isEmpty()) {
            dirtToCheck.removeIf(pair -> pair.second() + 20_000 < System.currentTimeMillis());

            for (Pair<BlockPos, Long> pair : dirtToCheck) {
                System.out.println("Distance: " + Math.sqrt(mc.thePlayer.getDistanceSq(pair.first())));
                if (Math.sqrt(mc.thePlayer.getDistanceSq(pair.first())) < 1.5) {
                    dirtPos.add(pair.first());
                    emergencyFailsafe(FailsafeType.DIRT);
                    dirtToCheck.clear();
                    break;
                }
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
                if(!FarmHelper.config.autoTPOnWorldChange) return;
                if (cooldown.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule(5000);
                    if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                        restartAfterFailsafeCooldown.schedule(FarmHelper.config.restartAfterFailSafeDelay * 1000L);
                }
                return;
            case LOBBY:
                if(!FarmHelper.config.autoTPOnWorldChange) return;
                if (cooldown.passed() && jacobWait.passed()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/skyblock");
                    cooldown.schedule(15_000);
                    afterEvacuateCooldown.schedule(15_000);
                    if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                        restartAfterFailsafeCooldown.schedule(FarmHelper.config.restartAfterFailSafeDelay * 1000L);
                }
                return;
            case HUB:
                if(!FarmHelper.config.autoTPOnWorldChange) return;
                LogUtils.debugLog("Detected Hub");
                if (afterEvacuateCooldown.isScheduled() && !afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("Waiting for \"after evacuate\" cooldown: " + (String.format("%.1f", afterEvacuateCooldown.getRemainingTime() / 1000f)));
                    return;
                } else if (afterEvacuateCooldown.isScheduled() && afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("After evacuate cooldown passed");
                    LogUtils.webhookLog("Teleporting back to island after evacuate");
                    mc.thePlayer.sendChatMessage(wasInGarden ? "/warp garden" : "/is");
                    afterEvacuateCooldown.schedule(15_000);
                }
                if (cooldown.passed() && jacobWait.passed() && !AutoCookie.isEnabled() && !AutoPot.isEnabled()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage(wasInGarden ? "/warp garden" : "/is");
                    cooldown.schedule(15_000);
                }
                if (emergency && FarmHelper.config.enableRestartAfterFailSafe)
                    restartAfterFailsafeCooldown.schedule(FarmHelper.config.restartAfterFailSafeDelay * 1000L);
                return;
            case ISLAND:
                checkInGarden();
                if (evacuateCooldown.isScheduled() && evacuateCooldown.getRemainingTime() < 2_500 && MacroHandler.currentMacro.enabled) {
                    MacroHandler.disableCurrentMacro(true);
                }
                if (evacuateCooldown.isScheduled() && evacuateCooldown.passed()) {
                    LogUtils.debugLog("Evacuating");
                    mc.thePlayer.sendChatMessage("/evacuate");
                    evacuateCooldown.reset();
                    afterEvacuateCooldown.schedule(5_000);
                }
                if (FarmHelper.config.enableJacobFailsafes && jacobExceeded() && jacobWait.passed() && MacroHandler.currentMacro.enabled) {
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
                        && !(BanwaveChecker.banwaveOn && FarmHelper.config.enableLeaveOnBanwave)
                        && !emergency
                        && !evacuateCooldown.isScheduled()
                        && !afterEvacuateCooldown.isScheduled()
                        && !restartAfterFailsafeCooldown.isScheduled()
                        && !VisitorsMacro.isEnabled()) {

                    LogUtils.debugLog("Resuming macro");
                    MacroHandler.enableCurrentMacro();
                } else if (afterEvacuateCooldown.isScheduled() && !afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("Waiting for \"after entering island\" cooldown: " + (String.format("%.1f", afterEvacuateCooldown.getRemainingTime() / 1000f)));
                } else if (afterEvacuateCooldown.isScheduled() && afterEvacuateCooldown.passed()) {
                    LogUtils.debugLog("\"After entering island\" cooldown passed");
                    afterEvacuateCooldown.reset();
                    MacroHandler.currentMacro.triggerTpCooldown();
                }
                // DEBUG MESSAGES IN CASE SOMETHING IS BROKEN AGAIN
//                else {
//                    if (!MacroHandler.currentMacro.enabled) {
//                        LogUtils.debugLog("Condition not met: MacroHandler.currentMacro.enabled");
//                    }
//
//                    if (!jacobWait.passed()) {
//                        LogUtils.debugLog("Condition not met: jacobWait.passed()");
//                    }
//
//                    if (AutoSellConfig.autoSell && !Autosell.isEnabled()) {
//                        LogUtils.debugLog("Condition not met: Autosell.isEnabled()");
//                    }
//
//                    if ((!MacroHandler.currentMacro.enabled || !Scheduler.isFarming()) && !MacroHandler.startingUp) {
//                        LogUtils.debugLog("Condition not met: MacroHandler.startingUp");
//                    }
//
//                    if (SchedulerConfig.scheduler && !Scheduler.isFarming()) {
//                        LogUtils.debugLog("Condition not met: Scheduler.isFarming()");
//                    }
//
//                    if (MiscConfig.autoCookie && !AutoCookie.isEnabled()) {
//                        LogUtils.debugLog("Condition not met: AutoCookie.isEnabled()");
//                    }
//
//                    if (MiscConfig.autoGodPot && !AutoPot.isEnabled()) {
//                        LogUtils.debugLog("Condition not met: AutoPot.isEnabled()");
//                    }
//
//                    if (BanwaveChecker.banwaveOn && FailsafeConfig.banwaveDisconnect) {
//                        LogUtils.debugLog("Condition not met: !(BanwaveChecker.banwaveOn && FailsafeConfig.banwaveDisconnect)");
//                    }
//
//                    if (emergency) {
//                        LogUtils.debugLog("Condition not met: !emergency");
//                    }
//
//                    if (evacuateCooldown.isScheduled()) {
//                        LogUtils.debugLog("Condition not met: !evacuateCooldown.isScheduled()");
//                    }
//
//                    if (afterEvacuateCooldown.isScheduled()) {
//                        LogUtils.debugLog("Condition not met: !afterEvacuateCooldown.isScheduled()");
//                    }
//
//                    if (restartAfterFailsafeCooldown.isScheduled()) {
//                        LogUtils.debugLog("Condition not met: !restartAfterFailsafeCooldown.isScheduled()");
//                    }
//                    if (MiscConfig.visitorsMacro && VisitorsMacro.isEnabled()) {
//                        LogUtils.debugLog("Condition not met: (!MiscConfig.visitorsMacro || !VisitorsMacro.isEnabled())");
//                    }
//                }
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
                return gameState.jacobCounter > FarmHelper.config.jacobNetherWartCap;
            } else if (cleanedLine.contains("Mushroom")) {
                return gameState.jacobCounter > FarmHelper.config.jacobMushroomCap;
            } else if (cleanedLine.contains("Carrot")) {
                return gameState.jacobCounter > FarmHelper.config.jacobCarrotCap;
            } else if (cleanedLine.contains("Potato")) {
                return gameState.jacobCounter > FarmHelper.config.jacobPotatoCap;
            } else if (cleanedLine.contains("Wheat")) {
                return gameState.jacobCounter > FarmHelper.config.jacobWheatCap;
            } else if (cleanedLine.contains("Sugar") || cleanedLine.contains("Cane") ) {
                return gameState.jacobCounter > FarmHelper.config.jacobSugarCaneCap;
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

    private static ExecutorService emergencyThreadExecutor = Executors.newScheduledThreadPool(5);
    private static final ArrayList<Pair<BlockPos, Long>> dirtToCheck = new ArrayList<>();
    private static final ArrayList<BlockPos> dirtPos = new ArrayList<>();

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event){
        if (mc.thePlayer == null || mc.theWorld == null || MacroHandler.currentMacro == null || !MacroHandler.isMacroing || !MacroHandler.currentMacro.enabled || emergency)
            return;

        if (!emergency && !dirtPos.isEmpty()) {
            dirtPos.clear();
        }

        if(event.update.getBlock().equals(Blocks.dirt)) {
            if (dirtToCheck.stream().noneMatch(pair -> pair.first().equals(event.pos))) {
                dirtToCheck.add(new Pair<>(event.pos, System.currentTimeMillis()));
                LogUtils.debugLog("Dirt has been put in the world near you");
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
        DESYNC("You are desynced. You might be lagging or there might be a staff spectating. If this is happening frequently, disable check desync"),
        ITEM_CHANGE("Your item has been probably changed."),
        TEST("Its just a test failsafe. (Its Safe to Ignore)");

        final String label;
        FailsafeType(String s) {
            this.label = s;
        }
    }

    static Runnable delayedStopScript = () -> {
        try{
            LogUtils.debugLog("delayedStopScript: waiting");
            Thread.sleep((long) ((FarmHelper.config.delayedStopScriptTime * 1_000) + Math.random() * (FarmHelper.config.delayedStopScriptTimeRandomness * 1_000)));
            MacroHandler.disableCurrentMacro(true);
            LogUtils.debugLog("delayedStopScript: done, stopping macro");
        } catch(Exception e){
            e.printStackTrace();
        }
    };

    public static void emergencyFailsafe(FailsafeType type) {

        emergency = true;

        LogUtils.webhookLog(type.label);
        LogUtils.scriptLog(type.label + "!");
        LogUtils.scriptLog("Act like a normal player and stop the script!");

        if(type != FailsafeType.DESYNC)
        {
            emergencyThreadExecutor.submit(delayedStopScript);
        }

        if(FarmHelper.config.popUpNotification){
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Utils.createNotification(type.label, tray, TrayIcon.MessageType.WARNING);
            } catch (UnsupportedOperationException e) {
                LogUtils.scriptLog("Notifications are not supported on this system");
            }
        }

        if(FarmHelper.config.autoAltTab) {
            Utils.bringWindowToFront();
        }

        if (FarmHelper.config.pingSound) {
            Utils.sendPingAlert();
        }

        if (FarmHelper.config.fakeMovements) {
            switch (type){
                case DESYNC:
                    emergencyThreadExecutor.submit(stopScript);
                    break;
                case BEDROCK:
                    emergencyThreadExecutor.submit(cagedActing);
                    break;
                case DIRT:
                    emergencyThreadExecutor.submit(rotationMovement);
                    break;
                case ROTATION:
                    emergencyThreadExecutor.submit(rotationMovement);
                    break;
                case ITEM_CHANGE:
                    emergencyThreadExecutor.submit(itemChange);
                    break;
            }
        }

        if (FarmHelper.config.enableRestartAfterFailSafe)
            restartAfterFailsafeCooldown.schedule(FarmHelper.config.restartAfterFailSafeDelay * 1000L);
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
            LogUtils.debugLog("rotationMovement: waiting");
            Thread.sleep((long) (1000 + Math.random() * 1000));

            long rotationTime;
            String messageChosen;
            boolean said;

            // stage 1: look around and move to back (bonus random crouch)
            LogUtils.debugLog("rotationMovement: stage 1");
            if (dirtPos.size() < 0) {
                rotationTime = (long)((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
                rotation.easeTo((float) (300 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);

                Thread.sleep(rotationTime + 200);
                KeyBindUtils.updateKeys(false, true, false, false, false, false, false);
                Thread.sleep((long) (500 + Math.random() * 500));
                KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
            }

            // exeception stage: if have a dirt check then get pos of dirt and look at it
            if (dirtPos.size() > 0) {
                LogUtils.debugLog("rotationMovement: stage 1.5");
                for (BlockPos dirt : dirtPos) {
                    LogUtils.debugLog("rotationMovement: stage 1.5: dirt found");

                    // get yaw and pitch to dirt
                    double x = mc.thePlayer.posX - dirt.getX();
                    double y = mc.thePlayer.posY - dirt.getY();
                    double z = mc.thePlayer.posZ - dirt.getZ();

                    float yaw = (float) Math.atan2(z, -x);
                    float pitch = (float)Math.atan2(-y, Math.sqrt(x*x+z*z));;

                    rotation.easeTo(yaw, pitch,450);

                    // move back
                    KeyBindUtils.updateKeys(false, true, false, false, true, false, false);
                    Thread.sleep((long) (500 + Math.random() * 500));
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                }
                dirtPos.clear();

                // random try break dirt block
                if (Math.random() < 0.5f) {
                    Thread.sleep((long) (200 + Math.random() * 500));
                    KeyBindUtils.updateKeys(false, false, false, false, true, false, false);
                    Thread.sleep(300);
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                }

                Thread.sleep((long) (1000 + Math.random() * 1000));
            }

            for (int i = 0; i < config.rotationActingTimes; i++) {
                rotationTime = (long)((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
                rotation.easeTo((float) (360 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                if (Math.random() < 0.1f) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
                    Thread.sleep(300);
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                }
                if (Math.random() < 0.5f) {
                    KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, false, false, false, false, false);
                    Thread.sleep(250);
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                }
                Thread.sleep(rotationTime + 200);
            }

            // stage 2: send a message
            LogUtils.debugLog("rotationMovement: stage 2");
            if (!FarmHelper.config.customRotationMessages.isEmpty()) {
                String[] messages = FarmHelper.config.customRotationMessages.split("\\|");
                if (messages.length > 1) {
                    messageChosen = messages[(int) Math.floor(Math.random() * (messages.length - 1))];
                    Thread.sleep((long) ((messageChosen.length() * 250) + Math.random() * 500));

                } else {
                    messageChosen = FarmHelper.config.customRotationMessages;
                    Thread.sleep((long) ((FarmHelper.config.customRotationMessages.length() * 250) + Math.random() * 500));
                }
            }
            else {
                messageChosen = FAILSAFE_MESSAGES[(int) Math.floor(Math.random() * (FAILSAFE_MESSAGES.length - 1))];
                Thread.sleep((long) ((messageChosen.length() * 250) + Math.random() * 500));
            }
            mc.thePlayer.sendChatMessage(messageChosen);
            said = true;

            // stage 3: look around again and jump
            LogUtils.debugLog("rotationMovement: stage 3");
            for (int i = 0; i < config.rotationActingTimes; i++) {
                rotationTime = (long)((Math.random() * FarmHelper.config.rotationTimeRandomness * 1_000) + FarmHelper.config.rotationTime * 1_000);
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
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                }
            }

            // final stage: come hub or quit game
            LogUtils.debugLog("rotationMovement: final stage");
            LogUtils.scriptLog("Stop the macro if you see this!");
            if (FarmHelper.config.ladderDesign) PlayerUtils.setSpawn();
            Thread.sleep(3000);

            if((Math.random() < 0.5d || !config.leaveAfterFailSafe) && said) {
                mc.thePlayer.sendChatMessage("/hub");
                Thread.sleep(3000);
                KeyBindUtils.updateKeys(false, false, false, false, false, false, false);
                if (FarmHelper.gameState.currentLocation == GameState.location.HUB) {
                    emergencyThreadExecutor.submit(bazaarChilling);
                } else {
                    Thread.sleep(1000 * 60 * 5);
                    MacroHandler.enableMacro();
                }
            } else {
                    mc.theWorld.sendQuittingDisconnectingPacket();
            }
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
        LogUtils.debugLog("You just got caged bozo! Buy a lottery ticket!");
        LogUtils.webhookLog("You just got caged bozo! Buy a lottery ticket! @everyone");

        if (bzchillingthread != null) {
            bzchillingthread.interrupt();
        }
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

            // stage 2: look around and sending messages
            for (int i = 0; i < config.bedrockActingTimes; i++) {
                rotationTime = (long) ((Math.random() * (FarmHelper.config.rotationTimeRandomness * 1_000)) + (FarmHelper.config.rotationTime * 1_000));
                rotation.easeTo((float) (270 * (Math.random())), (float) (20 * (Math.random() - 1)), rotationTime);
                if (i == 0) {
                    KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                }

                if(Math.random() < 0.5d && !said) {
                    said = true;
                    KeyBindUtils.stopMovement();
                    Thread.sleep(rotationTime + 1_500);

                    if (!FarmHelper.config.customBedrockMessages.isEmpty()) {
                        String[] messages = FarmHelper.config.customBedrockMessages.split("\\|");
                        if (messages.length > 1) {
                            messageChosen = messages[(int) Math.floor(Math.random() * (messages.length - 1))];
                            Thread.sleep((long) ((messageChosen.length() * 250) + Math.random() * 500));

                        } else {
                            messageChosen = FarmHelper.config.customBedrockMessages;
                            Thread.sleep((long) ((FarmHelper.config.customBedrockMessages.length() * 250) + Math.random() * 500));
                        }
                    }
                    else {
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
                        // around 0.3/0.6s long movements (the more random the best)
                        Thread.sleep((long) (100 * (Math.random() + 2)));
                        KeyBindUtils.updateKeys(Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.5, Math.random() < 0.8, Math.random() < 0.3);
                    }
                }
            }

            // stage 3: come hub or quit game
            stopMovement();
            Thread.sleep((long) (500 + Math.random() * 3_000));
            if((Math.random() < 0.5d || !config.leaveAfterFailSafe) && said) {
                mc.thePlayer.sendChatMessage("/hub");
                Thread.sleep(5000);
                if (FarmHelper.gameState.currentLocation == GameState.location.HUB) {
                    emergencyThreadExecutor.submit(bazaarChilling);
                } else {
                    Thread.sleep(1000 * 60 * 5);
                    MacroHandler.enableMacro();
                }
            } else {
                mc.theWorld.sendQuittingDisconnectingPacket();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };
}