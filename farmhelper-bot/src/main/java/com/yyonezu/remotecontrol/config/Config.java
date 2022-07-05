package com.yyonezu.remotecontrol.config;

import com.yyonezu.remotecontrol.config.interfaces.SecretConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static JSONObject config;
    private static final File configFile = new File("botconfig.json");

    public static void init() {
        // Create config file if it doesn't exist
        String token;
        String password;
        if (!configFile.isFile()) {
            writeConfig(DefaultConfig.getDefaultConfig());
            token = JOptionPane.showInputDialog("Put a discord token here: ");
            password = JOptionPane.showInputDialog("Choose a password (you'll have to set it on the mod as well): ");
            config = DefaultConfig.getDefaultConfig();
            set("token", token);
            set("password", password);
            writeConfig(config);
        } else {
            try (FileReader reader = new FileReader("botconfig.json")) {
                config = (JSONObject) new JSONParser().parse(reader);
                updateInterfaces();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        updateInterfaces();
    }

    private static void     writeConfig(JSONObject json) {
        try (FileWriter file = new FileWriter("botconfig.json")) {
            file.write(json.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateInterfaces() {
        SecretConfig.update();
    }

    public static Object get(String property) {
        if (config.get(property) == null) {
            set(property, DefaultConfig.getDefaultConfig().get(property));
        }
        return config.get(property);
    }

    public static void set(String property, Object value) {
        config.put(property, value);
        writeConfig(config);
        Config.updateInterfaces();
    }
}
