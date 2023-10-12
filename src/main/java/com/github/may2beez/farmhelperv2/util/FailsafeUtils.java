package com.github.may2beez.farmhelperv2.util;

import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class FailsafeUtils {
    private static FailsafeUtils instance;

    public static FailsafeUtils getInstance() {
        if (instance == null) {
            instance = new FailsafeUtils();
        }
        return instance;
    }

    public FailsafeUtils() {
        if (Minecraft.isRunningOnMac) {
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

    private final TrayIcon trayIcon;

    public void sendNotification(String text, TrayIcon.MessageType type) {
        if (!FarmHelperConfig.popUpNotification) return;
        try {
            if (Minecraft.isRunningOnMac) {
                Notify.create()
                        .title("Farm Helper Failsafes") // not enough space
                        .position(Pos.TOP_RIGHT)
                        .text(text)
                        .darkStyle()
                        .showWarning();
                return;
            }
            trayIcon.displayMessage("Farm Helper Failsafe Notification", text, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
