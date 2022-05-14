package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.concurrent.TimeUnit;

public class LogUtils {
    private static String lastDebug;
    private static String lastWebhook;
    private static long logMsgTime = 1000;
    private static long statusMsgTime = -1;
    private static Minecraft mc = Minecraft.getMinecraft();

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

    public static void debugLog(String message) {
        if (MiscConfig.debugMode || (lastDebug != null && lastDebug.equals(message))) {
            sendLog(new ChatComponentText(
              EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "Log " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.GRAY + message
            ));
        }
        lastDebug = message;
    }

    public static void debugFullLog(String message) {
//        if (MiscConfig.debugMode) {
//            debugLog(message);
//        }
        debugLog(message);
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

}
