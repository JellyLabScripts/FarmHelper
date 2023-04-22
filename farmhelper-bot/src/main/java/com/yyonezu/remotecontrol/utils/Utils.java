package com.yyonezu.remotecontrol.utils;

import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import org.eclipse.jetty.websocket.api.Session;

import java.awt.*;
import java.time.Duration;
import java.util.Map;

public class Utils {
    public static Session getSessionFromIGN(String ign) {
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(ign)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static long toMillis(String strDuration) {
        strDuration = strDuration.replaceAll("\\s+", "").replaceFirst("(\\d+d)", "P$1T");
        strDuration = strDuration.charAt(0) != 'P' ? "PT" + strDuration.replace("min", "m")
                : strDuration.replace("min", "m");
        Duration duration = Duration.parse(strDuration);
        return duration.toMillis();
    }

    public static String withSuffix(Long count) {
        if (count < 1000) return "" + count;
        int exp = (int) (Math.log(count) / Math.log(1000));
        return String.format("%.1f%c",
                count / Math.pow(1000, exp),
                "kMGTPE".charAt(exp-1));
    }
    public static String getIgnFromSession(Session s) {
        for (Map.Entry<Session, String> entry : WebSocketServer.minecraftInstances.entrySet()) {
            if (entry.getKey().equals(s)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void showNotification(String text) {
        try {
            //Obtain only one instance of the SystemTray object
            SystemTray tray = SystemTray.getSystemTray();

            //If the icon is a file
            Image image = Toolkit.getDefaultToolkit().createImage(Utils.class.getResource("/assets/rat.png"));
            TrayIcon trayIcon = new TrayIcon(image, "FarmHelper Bot");
            //Let the system resize the image if needed
            trayIcon.setImageAutoSize(true);
            //Set tooltip text for the tray icon
            trayIcon.setToolTip("FarmHelper Bot");
            tray.add(trayIcon);

            trayIcon.displayMessage("FarmHelper Bot", text, TrayIcon.MessageType.NONE);
            SystemTray.getSystemTray().remove(trayIcon);

        } catch (Exception ignored) {}
    }

}
