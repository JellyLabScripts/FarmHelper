package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.DiscordWebhook;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static String lastDebugMessage;
    private static String lastWebhook;
    private static long logMsgTime = 1000;
    private static long statusMsgTime = -1;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public synchronized static void sendLog(ChatComponentText chat) {
        if (mc.thePlayer != null && !FarmHelper.config.hideLogs)
           mc.thePlayer.addChatMessage(chat);
    }

    public static void sendSuccess(String message) {
        sendLog(new ChatComponentText("§2§lFarm Helper §8» §a" + message));
    }
    public static void sendWarning(String message) {
        sendLog(new ChatComponentText("§6§lFarm Helper §8» §e" + message));
    }
    public static void sendError(String message) {
        sendLog(new ChatComponentText("§4§lFarm Helper §8» §c" + message));
    }
    public static void sendDebug(String message) {
        if (lastDebugMessage != null && lastDebugMessage.equals(message)) return;
        if (FarmHelper.config.debugMode)
            sendLog(new ChatComponentText("§3§lFarm Helper §8» §7" + message));
        else
            System.out.println("Farm Helper » " + message);
        lastDebugMessage = message;
    }
    public static void sendFailsafeMessage(String message) {
        sendLog(new ChatComponentText("§5§lFarm Helper §8» §d§l" + message));
    }

    public static String getRuntimeFormat() {
        if (MacroHandler.startTime == 0)
            return "0h 0m 0s";
        long millis = System.currentTimeMillis() - MacroHandler.startTime;
        return String.format("%dh %dm %ds",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static void webhookStatus() {
        try {
            if (statusMsgTime == -1) {
                statusMsgTime = System.currentTimeMillis();
            }
            long timeDiff = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - statusMsgTime);
            if (timeDiff > FarmHelper.config.statusUpdateInterval && FarmHelper.config.sendStatusUpdates) {
                GameState.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                        .setTitle("Farm Helper")
                        .setDescription("```" + "I'm still alive!" + "```")
                        .setColor(Color.decode("#ff3b3b"))
                        .setFooter("Jelly", "")
                        .setThumbnail("https://crafatar.com/renders/head/" + mc.thePlayer.getUniqueID())
                        .addField("Username", mc.thePlayer.getName(), true)
                        .addField("Runtime", getRuntimeFormat(), true)
                        .addField("Total Profit", ProfitCalculator.profit, false)
                        .addField("Profit / hr", ProfitCalculator.profitHr, false)
                        .addField("Crop Type", String.valueOf(MacroHandler.crop), true)
                        .addField(BanwaveChecker.getBanDisplay(), "", false)
                );
                new Thread(() -> {
                    try {
                        GameState.webhook.execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
                statusMsgTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void webhookLog(String message) {
        long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - logMsgTime);
        sendDebug("Last webhook message: " + timeDiff);
        if (FarmHelper.config.sendLogs && (timeDiff > 20 || !Objects.equals(lastWebhook, message))) {
            GameState.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setDescription("**Farm Helper Log** ```" + message + "```")
                .setColor(Color.decode("#741010"))
                .setFooter(mc.thePlayer.getName(), "")
            );
            new Thread(() -> {
                try {
                    GameState.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            logMsgTime = System.currentTimeMillis();
        }
        lastWebhook = message;
    }
}
