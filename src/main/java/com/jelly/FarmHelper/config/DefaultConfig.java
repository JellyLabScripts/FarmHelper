package com.jelly.FarmHelper.config;

import org.json.simple.JSONObject;

public class DefaultConfig {
    public static JSONObject getDefaultConfig() {
        JSONObject config = new JSONObject();
        config.put("jacobFailsafe", false);
        config.put("mushroomCap", 200000);
        config.put("netherWartCap", 400000);
        config.put("carrotCap", 400000);
        config.put("potatoCap", 400000);
        config.put("wheatCap", 400000);
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
        config.put("autoGodPot", false);
        config.put("autoCookie", false);
        config.put("dropStone", true);
        config.put("ungrab", true);
        config.put("debugMode", false);
        config.put("cropType", 1);
        config.put("farmType", 0);
        config.put("openGUIKeybind", 54L);
        config.put("startScriptKeybind", 41L);
        return config;
    }
}
