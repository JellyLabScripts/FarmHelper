package com.yyonezu.remotecontrol;

import com.github.kaktushose.jda.commands.JDACommands;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.command.type.adapters.InstanceAdapter;
import com.yyonezu.remotecontrol.config.Config;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Main {
    public static JDA jda;
    static boolean validToken = false;
    public static String BOTVERSION;
    public static String MODVERSION;
    public static void main(String[] args) {
        setVersions();
        System.out.println(BOTVERSION);
        System.out.println(MODVERSION);
        Config.init();
        try {
            WebSocketServer.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
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
}
