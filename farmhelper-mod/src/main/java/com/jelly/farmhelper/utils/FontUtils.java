package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;

public class FontUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void drawString(String text, float x, float y, int color) {
        mc.fontRendererObj.drawString(text, x, y, color, false);
    }

    public static void drawCenteredString(String text, float x, float y, int color) {
        mc.fontRendererObj.drawString(text, x - mc.fontRendererObj.getStringWidth(text) / 2.0F, y, color, false);
    }
}