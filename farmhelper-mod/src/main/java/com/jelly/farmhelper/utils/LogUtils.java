package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.config.interfaces.RemoteControlConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.DiscordWebhook;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static String lastDebug;
    private static String lastWebhook;
    private static long logMsgTime = 1000;
    private static long statusMsgTime = -1;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public synchronized static void sendLog(ChatComponentText chat) {
        mc.thePlayer.addChatMessage(chat);
    }

    public static void scriptLog(String message) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_GREEN + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GREEN + EnumChatFormatting.BOLD + message
        ));
    }

    public static void scriptLog(String message, EnumChatFormatting color) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_GREEN + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + color + EnumChatFormatting.BOLD + message
        ));
    }

    public static void debugLog(String message) {
        if (MiscConfig.debugMode || lastDebug != message) {
            sendLog(new ChatComponentText(
                EnumChatFormatting.GREEN + "Log " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + message
            ));
        }
        lastDebug = message;
    }

    public static void debugFullLog(String message) {
        if (MiscConfig.debugMode) {
            debugLog(message);
        }
    }

    public static String getRuntimeFormat() {
        if (MacroHandler.startTime == 0) {
            return "0h 0m 0s";
        }
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
            if (timeDiff > RemoteControlConfig.webhookStatusCooldown && RemoteControlConfig.webhookStatus) {
                FarmHelper.gameState.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                        .setTitle("Farm Helper")
                        .setDescription("```" + "I'm still alive!" + "```")
                        .setColor(Color.decode("#ff3b3b"))
                        .setFooter("Jelly", "")
                        .setThumbnail("https://crafatar.com/renders/head/" + mc.thePlayer.getUniqueID())
                        .addField("Username", mc.thePlayer.getName(), true)
                        .addField("Runtime", getRuntimeFormat(), true)
                        .addField("Total Profit", ProfitCalculator.profit.get(), false)
                        .addField("Profit / hr", ProfitCalculator.profitHr.get(), false)
                        .addField(ProfitCalculator.getHighTierName(), ProfitCalculator.enchantedCropCount.get(), true)
                        .addField("Counter", ProfitCalculator.counter.get(), true)
                    // .addField("Screenshot", Screenshot.takeScreenshot(), true)
                );
                new Thread(() -> {
                    try {
                        FarmHelper.gameState.webhook.execute();
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
        debugFullLog("Last webhook message: " + timeDiff);
        if (RemoteControlConfig.webhookLogs && (timeDiff > 20 || lastWebhook != message)) {
            FarmHelper.gameState.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setDescription("**Farm Helper Log** ```" + message + "```")
                .setColor(Color.decode("#741010"))
                .setFooter(mc.thePlayer.getName(), "")
            );
            new Thread(() -> {
                try {
                    FarmHelper.gameState.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            logMsgTime = System.currentTimeMillis();
        }
        lastWebhook = message;
    }
}
