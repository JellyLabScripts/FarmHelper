package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.FarmHelperConfig;
import com.jelly.farmhelper.config.enums.ProxyType;

public class ProxyConfig {
    public static ProxyType type;
    public static String address;
    public static String username;
    public static String password;
    public static boolean connectAtStartup;

    public static void update() {
        type = ProxyType.values()[((Long)(FarmHelperConfig.get("proxyType"))).intValue()];
        address = (String) FarmHelperConfig.get("proxyAddress");
        username = (String) FarmHelperConfig.get("proxyUsername");
        password = (String) FarmHelperConfig.get("proxyPassword");
        connectAtStartup = (boolean) FarmHelperConfig.get("connectAtStartup");


    }
}
