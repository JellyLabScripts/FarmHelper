package com.yyonezu.remotecontrol;

import com.github.kaktushose.jda.commands.JDACommands;
import com.yyonezu.remotecontrol.config.Config;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import javax.swing.*;
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
    public static void main(String[] args) {
        setVersions();
        Config.init();
        try {
            WebSocketServer.start();
            JOptionPane.showMessageDialog(null, "Running successfully! Close this window");
        } catch (Exception e) {
            killProcessByPort();
            JOptionPane.showMessageDialog(null, "You ran it twice bozo, I killed the process. Close this and run it again");
            System.exit(0);
        }

        do {
            try {
                jda = JDABuilder.createDefault(SecretConfig.token).build();
                JDACommands.start(jda, Main.class);
/*
                        .getAdapterRegistry().register(Instance.class, new InstanceAdapter());
*/

                validToken = true;
            } catch (LoginException e) {
                String token = JOptionPane.showInputDialog("Incorrect token, set it again");
                Config.set("token", token);
            }
        } while (!validToken);
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
        if (System.getProperty("os.name").contains("win")) { // Probably Windows
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
