package com.jelly.farmhelper.config;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.*;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class ConfigHandler {
    private static JSONObject config;
    private static final File configFile = new File("farmhelper.json");
    private static final List<Class<?>> registeredConfigs = new ArrayList<>();

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
        try {
            for (Class<?> clazz : registeredConfigs) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.isAnnotationPresent(Config.class)) {
                        if(f.getType().isEnum()){
                            f.set(null, f.getType().getEnumConstants()[((Long)ConfigHandler.get(f.getAnnotation(Config.class).key())).intValue()]);
                        } else {
                            f.set(null, ConfigHandler.get(f.getAnnotation(Config.class).key()));
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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


    static class DefaultConfig{
        public static JSONObject getDefaultConfig() {
            JSONObject config = new JSONObject();
            config.put("jacobFailsafe", false);
            config.put("mushroomCap", 200000);
            config.put("netherWartCap", 400000);
            config.put("carrotCap", 400000);
            config.put("potatoCap", 400000);
            config.put("wheatCap", 400000);
            config.put("sugarcaneCap", 400000);
            config.put("webhookLogs", false);
            config.put("webhookStatus", false);
            config.put("webhookStatusCooldown", 1.0);
            config.put("webhookURL", "");
            config.put("autoSell", false);
            config.put("fullTime", 6.0);
            config.put("fullRatio", 65.0);
            config.put("profitCalculator", false);
            config.put("totalProfit", true);
            config.put("profitHour", true);
            config.put("itemCount", true);
            config.put("mushroomCount", true);
            config.put("counter", true);
            config.put("runtime", true);
            config.put("resync", true);
            config.put("fastbreak", true);
            config.put("fastbreakSpeed", 3.0);
            config.put("autoGodPot", false);
            config.put("autoCookie", false);
            config.put("dropStone", true);
            config.put("ungrab", true);
            config.put("debugMode", false);
            config.put("cropType", 1);
            config.put("farmType", 0);
            config.put("scheduler", false);
            config.put("statusGUI", true);
            config.put("farmTime", 60.0);
            config.put("breakTime", 5.0);
            config.put("banThreshold", 10.0);
            config.put("banwaveDisconnect", true);
            config.put("reconnectDelay", 5.0);
            config.put("websocketPassword", "");
            config.put("enableRemoteControl", false);
            config.put("websocketIP", "localhost:58637");
            config.put("xray", false);
            config.put("randomization", false);
            config.put("proxyType", 0);
            config.put("proxyAddress", "");
            config.put("proxyUsername", "");
            config.put("proxyPassword", "");
            config.put("connectAtStartup", false);
            return config;
        }
    }

}
