package com.jelly.farmhelper.config;

import java.io.*;

import com.jelly.farmhelper.config.interfaces.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FarmHelperConfig {
    private static JSONObject config;
    private static File configFile = new File("farmhelper.json");

    public static void init() {
        // Create config file if it doesn't exist
        if (!configFile.isFile()) {
            writeConfig(DefaultConfig.getDefaultConfig());
        }

        // Read config file
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("farmhelper.json")) {
            Object obj = jsonParser.parse(reader);
            config = (JSONObject) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        // Update all config categories
        updateInterfaces();
    }

    private static void writeConfig(JSONObject json) {
        try (FileWriter file = new FileWriter("farmhelper.json")) {
            file.write(json.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateInterfaces() {
        FarmConfig.update();
        JacobConfig.update();
        AutoSellConfig.update();
        MiscConfig.update();
        ProfitCalculatorConfig.update();
        WebhookConfig.update();
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
        updateInterfaces();
    }

}
