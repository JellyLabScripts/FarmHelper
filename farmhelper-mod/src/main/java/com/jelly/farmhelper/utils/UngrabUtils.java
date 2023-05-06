package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import org.lwjgl.input.Mouse;

public class UngrabUtils {
    public static boolean isUngrabbed = false;
    private static MouseHelper oldMouseHelper;
    private static boolean doesGameWantUngrabbed;

    public static void ungrabMouse() {
        Minecraft m = Minecraft.getMinecraft();
        if (isUngrabbed) return;
        m.gameSettings.pauseOnLostFocus = false;
        if (oldMouseHelper == null) oldMouseHelper = m.mouseHelper;
        doesGameWantUngrabbed = !Mouse.isGrabbed();
        oldMouseHelper.ungrabMouseCursor();
        m.inGameHasFocus = true;
        m.mouseHelper = new MouseHelper() {
            @Override
            public void mouseXYChange() {
            }
            @Override
            public void grabMouseCursor() {
                doesGameWantUngrabbed = false;
            }
            @Override
            public void ungrabMouseCursor() {
                doesGameWantUngrabbed = true;
            }
        };
        isUngrabbed = true;
    }

    /**
     * This function performs all the steps required to regrab the mouse.
     */
    public static void regrabMouse() {
        if (!isUngrabbed) return;

        Minecraft m = Minecraft.getMinecraft();
        m.mouseHelper = oldMouseHelper;
        if (!doesGameWantUngrabbed) m.mouseHelper.grabMouseCursor();
        oldMouseHelper = null;
        isUngrabbed = false;
    }
}

