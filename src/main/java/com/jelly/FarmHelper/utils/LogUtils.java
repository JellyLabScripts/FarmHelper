package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.FarmHelper;
import com.jelly.FarmHelper.config.interfaces.FarmConfig;
import com.jelly.FarmHelper.config.interfaces.MiscConfig;
import com.jelly.FarmHelper.config.interfaces.WebhookConfig;
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

    private static Minecraft mc = Minecraft.getMinecraft();

    public static String getCropLog() {
        switch (FarmConfig.cropType) {
            case WHEAT:
                return EnumChatFormatting.GOLD + "WHEAT";
            case CARROT:
                return EnumChatFormatting.DARK_GREEN + "CARROT";
            case NETHERWART:
                return EnumChatFormatting.DARK_RED + "NETHER WART";
            case POTATO:
                return EnumChatFormatting.YELLOW + "POTATO";
            default:
                return "UNKNOWN";
        }
    }

    public static String smallURL() {
        if (WebhookConfig.webhookURL.length() > 10) {
            return WebhookConfig.webhookURL.substring(WebhookConfig.webhookURL.length() - 10);
        } else {
            return WebhookConfig.webhookURL;
        }
    }

    public static void configLog() {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Configuration"
        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Crop - " + getCropLog()
        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Resync - " + (Config.resync ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Jacob Failsafe - " + (Config.jacobFailsafe ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Jacob Score Threshold - " + EnumChatFormatting.DARK_AQUA + Config.jacobThreshold
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Webhook Logging - " + (Config.webhookLog ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
//        ));
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Webhook URL - " + EnumChatFormatting.BLUE + smallURL()
        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Status Cooldown - " + EnumChatFormatting.GOLD + Config.statusTime + "min"
//        ));
//        sendLog(new ChatComponentText(
//            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + "Debug Mode - " + (Config.debug ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF")
//        ));
    }

    public static String getRuntimeFormat() {
        if (FarmHelper.startTime == 0) {
            return "0h 0m 0s";
        }
        long millis = System.currentTimeMillis() - FarmHelper.startTime;
        return String.format("%dh %dm %ds",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static void webhookStatus() {
        if (statusMsgTime == -1) {
            statusMsgTime = System.currentTimeMillis();
        }
        long timeDiff = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - statusMsgTime);
        if (timeDiff > WebhookConfig.webhookStatusCooldown && WebhookConfig.webhookStatus) {
            FarmHelper.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("Farm Helper")
                .setDescription("```" + "I'm still alive!" + "```")
                .setColor(Color.decode("#ff3b3b"))
                .setFooter("Jelly", "")
                .setThumbnail("https://crafatar.com/renders/head/" + mc.thePlayer.getUniqueID())
                .addField("Username", mc.thePlayer.getName(), true)
                .addField("Runtime", getRuntimeFormat(), true)
                .addField("Total Profit", ProfitUtils.profit.get(), false)
                .addField("Profit / hr", "$" + ProfitUtils.profitHr.get(), false)
                .addField(ProfitUtils.getHighTierName(), ProfitUtils.cropCount.get(), true)
                .addField("Counter", ProfitUtils.counter.get(), true)
                .addField("Screenshot", Screenshot.takeScreenshot(), true)
            );
            new Thread(() -> {
                try {
                    FarmHelper.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            statusMsgTime = System.currentTimeMillis();
        }
    }

    public static void webhookLog(String message) {
        long timeDiff = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - logMsgTime);
        debugFullLog("Last webhook message: " + timeDiff);
        if (WebhookConfig.webhookLogs && (timeDiff > 20 || lastWebhook != message)) {
            FarmHelper.webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setDescription("**Farm Helper Log** ```" + message + "```")
                .setColor(Color.decode("#741010"))
                .setFooter(mc.thePlayer.getName(), "")
            );
            new Thread(() -> {
                try {
                    FarmHelper.webhook.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            logMsgTime = System.currentTimeMillis();
        }
        lastWebhook = message;
    }

    public static void debugLog(String message) {
        if (MiscConfig.debugMode || lastDebug != message) {
            sendLog(new ChatComponentText(
                EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Log " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + message
            ));
        }
        lastDebug = message;
    }

    public static void debugFullLog(String message) {
        if (MiscConfig.debugMode) {
            debugLog(message);
        }
    }

    public synchronized static void sendLog(ChatComponentText chat) {
        mc.thePlayer.addChatMessage(chat);
    }

    public static void scriptLog(String message) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.RED + EnumChatFormatting.BOLD + message
        ));
    }

    public static void scriptLog(String message, EnumChatFormatting color) {
        sendLog(new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Farm Helper " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + color + EnumChatFormatting.BOLD + message
        ));
    }
}
