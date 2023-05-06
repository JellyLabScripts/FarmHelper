package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class Utils {
    static Minecraft mc = Minecraft.getMinecraft();

    public static boolean pingAlertPlaying = false;

    public static void openURL(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (Desktop.isDesktopSupported()) { // Probably Windows
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } else { // Definitely Non-windows
                Runtime runtime = Runtime.getRuntime();
                if (os.contains("mac")) { // Apple
                    runtime.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) { // Linux
                    runtime.exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {

        }
    }

    private static final Clock pingAlertClock = new Clock();
    private static int numPings = 15;

    public static void sendPingAlert() {
        pingAlertPlaying = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!pingAlertPlaying) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase == TickEvent.Phase.START) return;

        if (numPings <= 0) {
            pingAlertPlaying = false;
            numPings = 15;
            return;
        }

        if (pingAlertClock.isScheduled() && pingAlertClock.passed()) {
            mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 10.0F, 1.0F, false);
            pingAlertClock.schedule(100);
            numPings--;
        } else if (!pingAlertClock.isScheduled()) {
            pingAlertClock.schedule(100);
        }
    }

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

    public static void createNotification(String text, SystemTray tray, TrayIcon.MessageType messageType) {
        new Thread(() -> {

            if(Minecraft.isRunningOnMac) {
                try {
                    Runtime.getRuntime().exec(new String[]{"osascript", "-e", "display notification"});
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                TrayIcon trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), "Farm Helper Failsafe Notification");
                trayIcon.setToolTip("Farm Helper Failsafe Notification");
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }

                trayIcon.displayMessage("Farm Helper - Failsafes", text, messageType);

                tray.remove(trayIcon);
            }

        }).start();

    }

    public static void bringWindowToFront() {

        SwingUtilities.invokeLater(() -> {

            int TAB_KEY = Minecraft.isRunningOnMac ? KeyEvent.VK_META : KeyEvent.VK_ALT;
            try
            {
                Robot robot = new Robot();

                int i = 0;
                while (!Display.isActive())
                {
                    i++;
                    robot.keyPress(TAB_KEY);
                    for(int j = 0; j < i; j++) {
                        robot.keyPress(KeyEvent.VK_TAB);
                        robot.delay(100);
                        robot.keyRelease(KeyEvent.VK_TAB);

                    }
                    robot.keyRelease(TAB_KEY);
                    robot.delay(100);
                }
                UngrabUtils.regrabMouse();

            }
            catch (AWTException e)
            {
                System.out.println("Failed to use Robot, got exception: " + e.getMessage());
            }
        });

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