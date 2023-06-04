package com.jelly.farmhelper.config;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.interfaces.*;
import com.jelly.farmhelper.config.structs.Rewarp;
import com.jelly.farmhelper.utils.LogUtils;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigHandler {
    private static JSONObject config;
    private static final File configFile = new File("farmhelper.json");
    private static final File configRewarpFile = new File("farmhelper_rewarp.json");
    private static final List<Class<?>> registeredConfigs = new ArrayList<>();
    public static JSONObject defaultconfig;

    public static ArrayList<Rewarp> rewarpList = new ArrayList<>();

    public static void init() {
        // Register config classes so as to check annotations
        registeredConfigs.add(AutoSellConfig.class);
        registeredConfigs.add(FarmConfig.class);
        registeredConfigs.add(JacobConfig.class);
        registeredConfigs.add(MiscConfig.class);
        registeredConfigs.add(ProfitCalculatorConfig.class);
        registeredConfigs.add(ProxyConfig.class);
        registeredConfigs.add(RemoteControlConfig.class);
        registeredConfigs.add(SchedulerConfig.class);
        registeredConfigs.add(KeyBindConfig.class);
        registeredConfigs.add(FailsafeConfig.class);


        // Create config file if it doesn't exist
        if (!configFile.isFile()) {
            writeConfig(DefaultConfig.getDefaultConfig());
        }

        if (!configRewarpFile.isFile()) {
            try {
                Files.write(configRewarpFile.toPath(), new ArrayList<Rewarp>().toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Read config file
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("farmhelper.json")) {
            Object obj = jsonParser.parse(reader);
            config = (JSONObject) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        try {
            String json = new String(Files.readAllBytes(configRewarpFile.toPath()), StandardCharsets.UTF_8);
            rewarpList = FarmHelper.gson.fromJson(json, new TypeToken<List<Rewarp>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Update all config categories
        updateInterfaces();
    }

    public static void saveRewarpConfig() {
        try {
            Files.write(configRewarpFile.toPath(), FarmHelper.gson.toJson(rewarpList).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addRewarp(Rewarp rewarp) {
        rewarpList.add(rewarp);
        LogUtils.scriptLog("Added rewarp: " + rewarp.toString());
        saveRewarpConfig();
    }

    public static void removeRewarp(Rewarp rewarp) {
        rewarpList.remove(rewarp);
        LogUtils.scriptLog("Removed closest rewarp: " + rewarp.toString());
        saveRewarpConfig();
    }

    public static void removeAllRewarps() {
        rewarpList.clear();
        LogUtils.scriptLog("Removed all rewarp points");
        saveRewarpConfig();
    }

    private static void writeConfig(JSONObject json) {
        try (FileWriter file = new FileWriter("farmhelper.json")) {
            file.write(json.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getConfig() {
        return config;
    }

    private static void updateInterfaces() {
        try {
            for (Class<?> clazz : registeredConfigs) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Config.class)) {
                        String property = String.valueOf(ConfigHandler.get(f.getName()));
                        if(f.getType().isEnum()){
                            f.set(null, f.getType().getEnumConstants()[Integer.parseInt(property)]);
                        } else {
                            if (f.getType().equals(int.class)) {
                                f.set(null, Integer.parseInt(property));
                            } else if (f.getType().equals(long.class)) {
                                f.set(null, Long.parseLong(property));
                            } else if (f.getType().equals(double.class)) {
                                f.set(null, Double.parseDouble(property));
                            } else if (f.getType().equals(boolean.class)) {
                                f.set(null, Boolean.parseBoolean(property));
                            } else {
                                f.set(null, property);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Object get(String property) {
        try {
            if (config.get(property) == null) {
                set(property, DefaultConfig.getDefaultConfig().get(property));
            }
        } catch (Exception e) {
            System.out.println("Error getting property: " + property);
            set(property, DefaultConfig.getDefaultConfig().get(property));
        }
        return config.get(property);
    }

    public static void set(String property, Object value) {
        config.put(property, value);
        writeConfig(config);
        updateInterfaces();
    }

    private static class DefaultConfig {
        @SneakyThrows
        public static JSONObject getDefaultConfig() {
            if (defaultconfig == null) {
                defaultconfig = new JSONObject();
                for (Class<?> clazz : registeredConfigs) {
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.isAnnotationPresent(Config.class)) {
                            String key = f.getName();
                            f.setAccessible(true);
                            if (f.getType().isEnum()) {
                                for (int i = 0; i < f.getType().getEnumConstants().length; i++) {
                                    if (f.getType().getEnumConstants()[i].equals(f.get(clazz))) {
                                        defaultconfig.put(key, i);
                                        break;
                                    }
                                }
                            } else {
                                defaultconfig.put(key, f.get(clazz));
                            }
                        }
                    }
                }
            }
            return defaultconfig;
        }
    }
}
