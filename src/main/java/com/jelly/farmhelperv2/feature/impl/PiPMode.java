package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.Native;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.awt.*;

// This class is responsible for the Picture-in-Picture mode feature.
// This feature is only available on Windows.
// This feature is used to make the game window smaller and always on top.
// Made by CatalizCS with love <3
// spent 6 hours coding and debugging this feature :pray:

public class PiPMode implements IFeature {
    private int width;
    private int height;
    private final Minecraft mc = Minecraft.getMinecraft();

    public boolean borderlessMode;

    private static PiPMode instance;

    public static PiPMode getInstance() {
        if (instance == null) {
            instance = new PiPMode();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "PiPMode";
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return isToggled();
    }

    @Override
    public void start() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            LogUtils.sendError("[PiPMode] This feature is only available on Windows.");
            FarmHelperConfig.pipMode = false;
            return;
        }

        DisplayMode displayMode = Display.getDisplayMode();
        width = displayMode.getWidth();
        height = displayMode.getHeight();

        LogUtils.sendDebug("[PiPMode] Enabled.");
        setPiPMode(true);
    }

    @Override
    public void stop() {
        LogUtils.sendDebug("[PiPMode] Disabled.");
        setPiPMode(false);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        setPiPMode(false);
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.pipMode;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public boolean setAlwaysOnTop(boolean enabled) {
        try {
            User32 user32 = User32.INSTANCE;
            char[] buffer = new char[1024];

            user32.EnumWindows((hWnd, data) -> {
                user32.GetWindowText(hWnd, buffer, buffer.length);
                String title = Native.toString(buffer);
                if (title.contains("Farm Helper") && title.contains(mc.getSession().getUsername())) {
                    user32.SetWindowPos(hWnd, new WinDef.HWND(Pointer.createConstant(enabled ? -1 : -2)), 0, 0, 0, 0, 0x0001 | 0x0002 | 0x0008);
                    return false;
                }
                return true;
            }, null);
        } catch (Exception e) {
            LogUtils.sendError("[PiPMode] Failed to set or unset always on top: " + e.getMessage());
            return false;
        }

        return enabled;
    }

    public void setPiPMode(boolean enabled) {
        try {
            if (width == 0 || height == 0) {
                DisplayMode displayMode = Display.getDisplayMode();
                width = displayMode.getWidth();
                height = displayMode.getHeight();
            }

            if (enabled) {
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
                Display.setDisplayMode(new DisplayMode(420, 252));
                Display.setFullscreen(false);
                Display.setResizable(false);

                Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
                int x = (int) ((dimension.getWidth() - Display.getWidth()) / 2);
                int y = (int) ((dimension.getHeight() - Display.getHeight()) / 2);
                Display.setLocation(x, y);

                mc.resize(Display.getWidth(), Display.getHeight());
                Mouse.setCursorPosition((Display.getX() + Display.getWidth()) / 2, (Display.getY() + Display.getHeight()) / 2);

                setAlwaysOnTop(true);


            } else {
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
                Display.setDisplayMode(new DisplayMode(width, height));
                Display.setFullscreen(true);
                Display.setResizable(true);

                Display.setResizable(false);
                Display.setResizable(true);

                mc.resize(Display.getWidth(), Display.getHeight());
                Mouse.setCursorPosition((Display.getX() + Display.getWidth()) / 2, (Display.getY() + Display.getHeight()) / 2);

                Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
                int x = (int) ((dimension.getWidth() - Display.getWidth()) / 2);
                int y = (int) ((dimension.getHeight() - Display.getHeight()) / 2);
                Display.setLocation(x, y);

                setAlwaysOnTop(false);

                width = 0;
                height = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!FarmHelperConfig.pipMode || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !MacroHandler.getInstance().getCurrentMacro().isPresent())
            return;

//        Need optimize this code later
        if (Mouse.isButtonDown(2)) {
            Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
            float damping = 0.8f;

            int dx = Mouse.getDX();
            int dy = Mouse.getDY();

            if (Display.getX() + dx * damping < 0 || Display.getX() + dx * damping + Display.getWidth() > dimension.getWidth()) {
                dx = 0;
            }
            if (Display.getY() + dy * damping < 0 || Display.getY() + dy * damping + Display.getHeight() > dimension.getHeight()) {
                dy = 0;
            }

            int newX = (int)(Display.getX() + dx * damping);
            int newY = (int)(Display.getY() + dy * damping);

            Display.setLocation(newX, newY);
        }

        if (borderlessMode) {
            DisplayMode displayMode = Display.getDisplayMode();
            if (displayMode.getWidth() != width || displayMode.getHeight() != height) {
                setPiPMode(true);
            }
        }
    }
}
