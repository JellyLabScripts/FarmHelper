package com.yyonezu.remotecontrol.config.interfaces;

import com.yyonezu.remotecontrol.config.Config;

public class SecretConfig {

    public static String token;
    public static String password;

    public static void update() {
        token = (String) Config.get("token");
        password = (String) Config.get("password");

    }
}
