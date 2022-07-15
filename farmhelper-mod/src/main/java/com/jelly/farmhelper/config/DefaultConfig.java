package com.jelly.farmhelper.config;

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
        config.put("openGUIKeybind", 54L);
        config.put("startScriptKeybind", 41L);
        config.put("scheduler", false);
        config.put("statusGUI", true);
        config.put("farmTime", 60.0);
        config.put("breakTime", 5.0);
        config.put("banThreshold", 10.0);
        config.put("banwaveDisconnect", true);
        config.put("reconnectDelay", 5.0);
        config.put("websocketPassword", "");
        config.put("enableRemoteControl", false);
        config.put("xray", false);
        config.put("randomization", false);
        config.put("proxyType", 0);
        config.put("proxyAddress", "");
        config.put("proxyUsername", "");
        config.put("proxyPassword", "");
        config.put("connectAtStartup", true);
        return config;
    }
}
