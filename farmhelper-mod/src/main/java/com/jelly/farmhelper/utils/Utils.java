package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumChatFormatting;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class Utils {

    public static void clickWindow(int windowID, int slotID, int mouseButtonClicked, int mode) throws Exception {
        if (Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerChest || Minecraft.getMinecraft().currentScreen instanceof GuiInventory) {
            Minecraft.getMinecraft().playerController.windowClick(windowID, slotID, mouseButtonClicked, mode, Minecraft.getMinecraft().thePlayer);
            LogUtils.scriptLog("Pressing slot : " + slotID);
        } else {
            LogUtils.scriptLog(EnumChatFormatting.RED + "Didn't open window! This shouldn't happen and the script has been disabled. Please immediately report to the developer.");
            updateKeys(false, false, false, false, false, false, false);
            throw new Exception();
        }
    }

    public static String formatNumber(int number) {
        String s = Integer.toString(number);
        return String.format("%,d", number);
    }

    public static String formatNumber(float number) {
        String s = Integer.toString(Math.round(number));
        return String.format("%,d", Math.round(number));
    }

    public static String formatTime(long millis) {
        return String.format("%dh %dm %ds",
            TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
            TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    public static int nextInt(int upperbound) {
        Random r = new Random();
        return r.nextInt(upperbound);
    }
}