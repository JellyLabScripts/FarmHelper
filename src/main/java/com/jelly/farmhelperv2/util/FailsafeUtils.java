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

    private static final String[] FAILSAFE_MESSAGES = new String[]{
            "WHAT", "what?", "what", "what??", "what???", "wut?", "?", "what???",
            "yo huh", "yo huh?", "yo?", "ehhhhh??", "eh", "yo", "ahmm", "ehh", "LOL what", "lol :skull:", "bro wtf was that?",
            "lmao", "lmfao", "wtf is this", "wtf", "WTF", "wtf is this?", "wtf???", "tf", "tf?", "wth", "lmao what?", "????",
            "??", "???????", "???", "UMMM???", "umm", "ummm???", "damn wth", "Damn", "damn wtf", "damn", "hmmm", "hm", "sus",
            "hmm", "ok??", "ok?", "give me a rest", "again lol", "again??", "ok damn", "seriously?", "seriously????",
            "seriously", "really?", "really", "are you kidding me?", "are you serious?", "are you fr???", "not again",
            "give me a break", "youre kidding right?", "youre joking", "youre kidding me", "you must be joking", "seriously bro?",
            "cmon now", "cmon", "this is too much", "stop messing with me", "um what's going on?", "yo huh? did something happen?",
            "lol, what was that?", "ehhh what's happening here?", "bro, wtf was that move?", "wth just happened?", "again seriously?",
            "ok seriously what's up?", "are you kidding me right now?", "give me a break, what's going on?", "seriously bro?",
            "lmao, what was that move?", "damn wth just happened?", "ok, damn what's happening?", "you're joking right?",
            "cmon now whats happening?", "wtf seriously?", "really, again?", "are you serious right now?",
            "you must be joking, right?", "stop messing with me, seriously", "this is too much, come on!", "give me a rest",
            "hmmm, what's going on?", "um, seriously, what's up?", "ok, seriously, what's the deal?", "lmao what's going on here?",
            "damn, wtf is this?", "ok, seriously, really?", "what, again?", "wtf, what's happening?", "yo seriously what's up?",
            "hmmm, what was that?", "seriously, bro, really?", "again? are you kidding me?", "give me a break",
            "wtf is this???", "ok damn seriously?", "are you fr? what just happened?", "bro seriously what's wrong with you?",
            "you're kidding me, right?", "cmon this is too much!", "really, again? why??", "are you joking??",
            "um whats going on?", "ok seriously give me a rest", "what just happened lmao"};
    private static final String[] FAILSAFE_CONTINUE_MESSAGES = new String[]{
            "can i keep farming?", "lemme farm ok?", "leave me alone next time ok?", "leave me alone lol", "let me farm",
            "nice one admin", "hello admin???", "hello admin", "bro let me farm", "bro let me farm ok?", "bro let me farm pls",
            "dude leave me alone", "dude tf was that", "hey can i just do my thing?", "can i farm in peace please?",
            "let me farm bro seriously", "admin can i keep farming?", "hey admin leave me alone this time alright?",
            "admin seriously let me farm in peace", "admin bro let me farm okay?", "admin let me do my farming thing please",
            "can i keep doing my thing admin?", "bro seriously let me farm in peace", "admin nice one but can i still farm?",
            "hello admin can i keep farming?", "admin let me do my farm routine okay?", "admin seriously can i farm in peace?",
            "can i continue farming?", "admin let me farm in peace please", "can i keep farming admin? pretty please?",
            "admin seriously let me farm in peace ok?", "can i continue my farming admin?", "admin let me farm please and thank you",
            "leave me alone this time alright?", "seriously let me farm in peace", "bro let me farm okay?",
            "let me do my farming thing please", "can i keep doing my thing?", "nice one but can i still farm?",
            "hello can i keep farming?", "let me do my farm routine okay?", "seriously can i farm in peace?", "let me farm in peace please",
            "can i keep farming? pretty please?", "seriously let me farm in peace alright?", "can i continue my farming?",
            "let me farm please and thank you", "let me farm and dont interrupt me please", "let me farm dude seriously",
            "admin dude let me farm okay?", "dude seriously let me farm in peace", "dude let me farm okay?"};


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

    public static String getRandomMessage(String[] messages) {
        if (messages.length > 1) {
            return messages[(int) (Math.random() * (messages.length - 1))];
        } else {
            return messages[0];
        }
    }

    public static String getRandomMessage() {
        return getRandomMessage(FAILSAFE_MESSAGES);
    }

    public static String getRandomContinueMessage() {
        return getRandomMessage(FAILSAFE_CONTINUE_MESSAGES);
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
