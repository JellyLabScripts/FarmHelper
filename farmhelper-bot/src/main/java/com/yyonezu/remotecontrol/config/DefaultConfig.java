package com.yyonezu.remotecontrol.config;

import org.json.simple.JSONObject;

public class DefaultConfig {
    public static JSONObject getDefaultConfig() {
        JSONObject config = new JSONObject();
        config.put("token", "");
        config.put("password", "");
        return config;
    }
}
