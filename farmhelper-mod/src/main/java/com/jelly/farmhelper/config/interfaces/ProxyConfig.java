package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.ProxyType;

public class ProxyConfig {
    @Config()
    public static ProxyType proxyType = ProxyType.SOCKS4;
    @Config()
    public static String proxyAddress = "";
    @Config()
    public static String proxyUsername = "";
    @Config()
    public static String proxyPassword = "";
    @Config()
    public static boolean connectAtStartup = false;
}
