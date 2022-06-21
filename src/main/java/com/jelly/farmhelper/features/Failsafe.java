package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.interfaces.JacobConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.ScoreboardUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jelly.farmhelper.FarmHelper.gameState;

public class Failsafe {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Clock cooldown = new Clock();
    public static final Clock jacobWait = new Clock();
    private static String formattedTime;


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

        if(gameState.currentLocation != GameState.location.ISLAND && MacroHandler.currentMacro.enabled){
            MacroHandler.disableCurrentMacro();
        }

        switch (gameState.currentLocation) {
            case TELEPORTING:
                if (cooldown.passed()) {
                    LogUtils.debugLog("Teleport failed??");
                    LogUtils.webhookLog("Teleport failed??");
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule(15000);
                }
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
                if (cooldown.passed() && jacobWait.passed() && !AutoCookie.isEnabled()) {
                    LogUtils.webhookLog("Not at island - teleporting back");
                    mc.thePlayer.sendChatMessage("/is");
                    cooldown.schedule(5000);
                }
                return;
            case ISLAND:
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
                        && !(BanwaveChecker.banwaveOn && MiscConfig.banwaveDisconnect)) {
                    MacroHandler.enableCurrentMacro();
                }
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
}