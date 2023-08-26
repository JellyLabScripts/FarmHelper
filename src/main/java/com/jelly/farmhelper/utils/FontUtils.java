package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;

public class FontUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void drawString(String text, float x, float y, int color) {
        mc.fontRendererObj.drawString(text, x, y, color, false);
    }

    public static void drawString(String text, float x, float y, int color, boolean shadow) {
        mc.fontRendererObj.drawString(text, x, y, color, shadow);
    }

    public static void drawCenteredString(String text, float x, float y, int color) {
        mc.fontRendererObj.drawString(text, x - mc.fontRendererObj.getStringWidth(text) / 2.0F, y, color, false);
    }

    public static void drawScaledString(String string, float scale, int x, int y, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1);
        drawString(string, (int)(x/scale), (int)(y/scale), Color.WHITE.getRGB(),  shadow);
        GlStateManager.popMatrix();
    }
}