package com.jelly.farmhelper.features;

import com.jelly.farmhelper.config.interfaces.JacobConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.ScoreboardUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
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
    private static final Clock jacobWait = new Clock();
    private static boolean teleporting;

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = net.minecraft.util.StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (MacroHandler.macroEnabled) {
            if (message.contains("DYNAMIC") || message.contains("Something went wrong trying to send ") || message.contains("don't spam") || message.contains("A disconnect occurred ") || message.contains("An exception occurred ") || message.contains("Couldn't warp ") || message.contains("You are sending commands ") || message.contains("Cannot join ") || message.contains("There was a problem ") || message.contains("You cannot join ") || message.contains("You were kicked while ") || message.contains("You are already playing") || message.contains("You cannot join SkyBlock from here!")) {
                LogUtils.debugLog("Failed teleport - waiting");
                teleporting = false;
                cooldown.schedule(10000);
            }
        }
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.macroEnabled || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        switch (gameState.currentLocation) {
            case TELEPORTING:
                teleporting = false;
                return;
            case LIMBO:
                if (cooldown.passed()) {
                    mc.thePlayer.sendChatMessage("/lobby");
                    cooldown.schedule(5000);
                    teleporting = true;
                }
                return;
            case LOBBY:
                if (cooldown.passed() && jacobWait.passed()) {
                    mc.thePlayer.sendChatMessage("/skyblock");
                    cooldown.schedule(5000);
                    teleporting = true;
                }
                return;
            case HUB:
                LogUtils.debugLog("Detected Hub");
                if (cooldown.passed() && jacobWait.passed()) {
                    mc.thePlayer.sendChatMessage("/is");
                    cooldown.schedule(5000);
                    teleporting = true;
                }
                return;
            case ISLAND:
                if (jacobExceeded()) {
                    mc.thePlayer.sendChatMessage("/setspawn");
                    MacroHandler.disableCurrentMacro();
                    jacobWait.schedule(getJacobRemaining());
                    mc.thePlayer.sendChatMessage("/lobby");
                } else {
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
                LogUtils.debugLog("Jacob remaining time: " + matcher.group(1) + "m" + matcher.group(2) + "s");
                // LogUtils.webhookLog("Reached jacob threshold - Resuming in " + matcher.group(1) + "m" + matcher.group(2) + "s");
                return TimeUnit.MINUTES.toMillis(Long.parseLong(matcher.group(1))) + TimeUnit.SECONDS.toMillis(Long.parseLong(matcher.group(2)));
            }
        }
        LogUtils.debugLog("Failed to get Jacob remaining time");
        return 0;
    }
}
