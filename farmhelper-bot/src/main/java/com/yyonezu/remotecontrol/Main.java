package com.yyonezu.remotecontrol;

import com.github.kaktushose.jda.commands.JDACommands;
import com.yyonezu.remotecontrol.config.Config;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Main {
    public static JDA jda;
    static boolean validToken = false;
    public static String BOTVERSION;
    public static String MODVERSION;
    public static final int port = 58637;
    public static final boolean onWindows = System.getProperty("os.name").toLowerCase().contains("win");
    private static JDACommands jdaCommands;

    public static void main(String[] args) {
        if (System.getProperty("java.version").contains("1.8")) {
            String message = "<html>This program only works with Java 8. Please check this link for instructions on how to switch Java versions on Windows:<br><a href=\"https://www.happycoders.eu/java/how-to-switch-multiple-java-versions-windows/\">https://www.happycoders.eu/java/how-to-switch-multiple-java-versions-windows/</a></html>";
            JOptionPane.showMessageDialog(null, message, "Java Version Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        new FarmHelperBotGUI();
    }

    public static void start() {

        setVersions();
        Config.init();
        try {
            WebSocketServer.start();
            FarmHelperBotGUI.setInfoText("The bot is running successfully, you should minimize this window (goes to system tray)");
        } catch (Exception e) {
            e.printStackTrace();
            killProcessByPort();
            FarmHelperBotGUI.setInfoText("The bot is already running somehow? I've killed the process for you, try pressing start again");
            stop();
        }

        do {
            try {
                jda = JDABuilder.createDefault(SecretConfig.token).build();
                jdaCommands = JDACommands.start(jda, Main.class);
                validToken = true;
            } catch (InvalidTokenException e) {
                String token = JOptionPane.showInputDialog("Invalid discord bot token, type it again: ");
                Config.set("token", token);
            }
        } while(!validToken);
    }

    public static void stop() {
        if (jda != null) jda.shutdownNow();
        if (jdaCommands != null) jdaCommands.shutdown();
        WebSocketServer.stop();
    }
    @SneakyThrows
    public static void setVersions() {
        Class clazz = Main.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) return;

        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
                "/META-INF/MANIFEST.MF";
        Manifest manifest = new Manifest(new URL(manifestPath).openStream());
        Attributes attr = manifest.getMainAttributes();
        MODVERSION = attr.getValue("modversion");
        BOTVERSION = attr.getValue("botversion");
    }

    private static void killProcessByPort() {
        if (onWindows) {
            try {
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec("cmd /c netstat -ano | findstr " + Main.port);

                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(proc.getInputStream()));
                String s;
                if ((s = stdInput.readLine()) != null) {
                    int index = s.lastIndexOf(" ");
                    String sc = s.substring(index);

                    Process pr = rt.exec("cmd /c Taskkill /PID" + sc + " /T /F");
                    pr.waitFor();

                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Something went wrong while killing process. Report this");
            }
        } else { // mac & linux
            try {
                Runtime rt = Runtime.getRuntime();
                Process p = rt.exec("lsof -t -i:" + Main.port);
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));
                String s;
                if ((s = stdInput.readLine()) != null) {
                    Process pr = rt.exec("kill -9 " + s);
                    pr.waitFor();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Something went wrong while killing process. Report this");
            }
        }
    }
}
