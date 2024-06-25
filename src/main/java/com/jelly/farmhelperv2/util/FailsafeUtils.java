package com.jelly.farmhelperv2.util;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.util.helper.KeyCodeConverter;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class FailsafeUtils {
    private static FailsafeUtils instance;
    private final TrayIcon trayIcon;

    public FailsafeUtils() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            trayIcon = null;
            return;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getResource("/farmhelper/icon-mod/rat.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        trayIcon = new TrayIcon(image, "Farm Helper Failsafe Notification");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Farm Helper Failsafe Notification");
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static FailsafeUtils getInstance() {
        if (instance == null) {
            instance = new FailsafeUtils();
        }
        return instance;
    }

    public static void bringWindowToFront() {
        if (SystemUtils.IS_OS_WINDOWS) {
            bringWindowToFrontUsingWinApi();
            System.out.println("Bringing window to front using WinApi.");
        } else {
            bringWindowToFrontUsingRobot();
            System.out.println("Bringing window to front using Robot.");
        }
    }

    public static void bringWindowToFrontUsingWinApi() {
        try {
            User32 user32 = User32.INSTANCE;
            WinDef.HWND hWnd = user32.FindWindow(null, Display.getTitle());
            if (hWnd == null) {
                System.out.println("Window not found.");
                bringWindowToFrontUsingRobot();
                return;
            }
            if (!user32.IsWindowVisible(hWnd)) {
                user32.ShowWindow(hWnd, WinUser.SW_RESTORE);
                System.out.println("Window is not visible, restoring.");
            }
            user32.ShowWindow(hWnd, WinUser.SW_SHOW);
            user32.SetForegroundWindow(hWnd);
            user32.SetFocus(hWnd);
        } catch (Exception e) {
            System.out.println("Failed to restore the game window.");
            e.printStackTrace();
            System.out.println("Trying to bring window to front using Robot instead.");
            bringWindowToFrontUsingRobot();
        }
    }

    public static void bringWindowToFrontUsingRobot() {
        SwingUtilities.invokeLater(() -> {
            int TAB_KEY = Minecraft.isRunningOnMac ? KeyEvent.VK_META : KeyEvent.VK_ALT;
            try {
                Robot robot = new Robot();
                int i = 0;
                while (!Display.isActive()) {
                    i++;
                    robot.keyPress(TAB_KEY);
                    for (int j = 0; j < i; j++) {
                        robot.keyPress(KeyEvent.VK_TAB);
                        robot.delay(100);
                        robot.keyRelease(KeyEvent.VK_TAB);
                    }
                    robot.keyRelease(TAB_KEY);
                    robot.delay(100);
                    if (i > 25) {
                        System.out.println("Failed to bring window to front.");
                        return;
                    }
                }
            } catch (AWTException e) {
                System.out.println("Failed to use Robot, got exception: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void captureClip() {
        SwingUtilities.invokeLater(() -> {
            try {
                ArrayList<Integer> keys = FarmHelperConfig.captureClipKeybind.getKeyBinds();
                if (hasUndefinedKey(keys)) {
                    LogUtils.sendError("Failed to capture a clip! Keybind is either not set or invalid!");
                    return;
                }
                Robot robot = new Robot();
                keys.forEach(key -> {
                    robot.keyPress(KeyCodeConverter.convertToAwtKeyCode(key));
                });
                robot.delay(250);
                keys.forEach(key -> {
                    robot.keyRelease(KeyCodeConverter.convertToAwtKeyCode(key));
                });
            } catch (AWTException e) {
                System.out.println("Failed to use Robot, got exception: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static boolean hasUndefinedKey(ArrayList<Integer> keys) {
        for (int key : keys)
            if (KeyCodeConverter.convertToAwtKeyCode(key) == KeyEvent.VK_UNDEFINED)
                return true;
        return false;
    }

    public void sendNotification(String text, TrayIcon.MessageType type) {
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                windows(text, type);
            } else if (SystemUtils.IS_OS_MAC_OSX) {
                mac(text);
            } else if (SystemUtils.IS_OS_LINUX) {
                linux(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void windows(String text, TrayIcon.MessageType type) {
        if (SystemTray.isSupported()) {
            try {
                trayIcon.displayMessage("Farm Helper", text, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("SystemTray is not supported");
        }
    }

    private void mac(String text) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("osascript", "-e", "display notification \"" + text + "\" with title \"FarmHelper\"");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void linux(String text) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("notify-send", "-a", "Farm Helper", text);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
