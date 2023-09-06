package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.main.Main;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class Utils{
    static Minecraft mc = Minecraft.getMinecraft();
    public static String signText = "";

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
        } catch (Exception ignored) {
        }
    }

    private static final Clock pingAlertClock = new Clock();
    private static int numPings = 15;
    public static void disablePingAlert() {
        pingAlertPlaying = false;
        numPings = 15;
    }

    public static void playMcFailsafeSound() {
        pingAlertPlaying = true;
        numPings = 15;
    }

    public static void stopMcFailsafeSound() {
        pingAlertPlaying = false;
        numPings = 15;
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
            switch (FarmHelper.config.failsafeMcSoundSelected) {
                case 0:
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 10.0F, 1.0F, false);
                    break;
                case 1:
                    mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.anvil_land", 10.0F, 1.0F, false);
                    break;
            }
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
                Notify.create()
                        .title("Farm Helper Failsafes") // not enough space
                        .position(Pos.TOP_RIGHT)
                        .text(text)
                        .darkStyle()
                        .showWarning();
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

                float initialSensitivity = mc.gameSettings.mouseSensitivity;
                mc.gameSettings.mouseSensitivity = 0;
                mc.thePlayer.closeScreen();
                mc.gameSettings.mouseSensitivity = initialSensitivity;

            }
            catch (AWTException e)
            {
                System.out.println("Failed to use Robot, got exception: " + e.getMessage());
            }
        });

    }

    public static void bringWindowToFrontWinApi() {
        User32 user32 = User32.INSTANCE;

        WinDef.HWND hWnd = user32.FindWindow(null, Display.getTitle());
        if (user32.IsWindowVisible(hWnd)) {
            user32.ShowWindow(hWnd, User32.SW_SHOWMAXIMIZED);
            user32.SetForegroundWindow(hWnd);
            user32.SetFocus(hWnd);
        }
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

    private static Clip clip;

    public static synchronized void playFailsafeSound() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    AudioInputStream inputStream = null;
                    switch (FarmHelper.config.failsafeSoundSelected) {
                        case 0:
                            File audioFile = new File(mc.mcDataDir.getAbsolutePath() + "/farmhelper_sound.wav");
                            if (audioFile.exists() && audioFile.isFile())
                                inputStream = AudioSystem.getAudioInputStream(audioFile);
                            break;
                        case 1:
                            inputStream = AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("/assets/farmhelper/sounds/staff_check_voice_notification.wav"));
                            break;
                        case 2:
                            inputStream = AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("/assets/farmhelper/sounds/metal_pipe.wav"));
                            break;
                        case 3:
                            inputStream = AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("/assets/farmhelper/sounds/AAAAAAAAAA.wav"));
                            break;
                        case 4:
                            inputStream = AudioSystem.getAudioInputStream(Main.class.getResourceAsStream("/assets/farmhelper/sounds/loud_buzz.wav"));
                            break;
                    }
                    clip = AudioSystem.getClip();
                    clip.open(inputStream);
                    FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    volume.setValue(FarmHelper.config.failsafeSoundVolume);
                    clip.start();
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }).start();
    }

    public static boolean isFailsafeSoundPlaying() {
        return clip != null && clip.isRunning();
    }

    public static void stopFailsafeSound() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
}