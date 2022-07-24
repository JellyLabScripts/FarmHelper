package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class RemoteControlConfig {
    @Config()
    public static boolean webhookLogs = false;
    @Config()
    public static boolean webhookStatus = false;
    @Config()
    public static boolean enableRemoteControl = false;
    @Config()
    public static String websocketPassword = "";
    @Config()
    public static String websocketIP = "localhost";
    @Config()
    public static double webhookStatusCooldown = 1.0;
    @Config()
    public static String webhookURL = "";
}
