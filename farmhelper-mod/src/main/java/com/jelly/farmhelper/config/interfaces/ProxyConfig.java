package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.ProxyType;

public class ProxyConfig {
    @Config(key = "proxyType")
    public static ProxyType type;
    @Config(key = "proxyAddress")
    public static String address;
    @Config(key = "proxyUsername")
    public static String username;
    @Config(key = "proxyPassword")
    public static String password;
    @Config(key = "connectAtStartup")
    public static boolean connectAtStartup;
}
