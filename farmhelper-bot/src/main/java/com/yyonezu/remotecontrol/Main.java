package com.yyonezu.remotecontrol;

import com.github.kaktushose.jda.commands.JDACommands;
import com.yyonezu.remotecontrol.command.type.Instance;
import com.yyonezu.remotecontrol.command.type.adapters.InstanceAdapter;
import com.yyonezu.remotecontrol.config.Config;
import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import com.yyonezu.remotecontrol.websocket.WebSocketServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import java.io.IOException;

public class Main {
    public static JDA jda;
    static boolean validToken = false;

    public static void main(String[] args) {
        Config.init();
        try {
            WebSocketServer.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "This is running already, shut it down from task manager");
            System.exit(0);
        }

        do {
            try {
                JDA jda = JDABuilder.createDefault(SecretConfig.token).build();
                JDACommands.start(jda, Main.class)
                        .getAdapterRegistry().register(Instance.class, new InstanceAdapter());
                validToken = true;
            } catch (LoginException e) {
                String token = JOptionPane.showInputDialog("Incorrect token, set it again");
                Config.set("token", token);
            }
        } while (!validToken);
    }
}

