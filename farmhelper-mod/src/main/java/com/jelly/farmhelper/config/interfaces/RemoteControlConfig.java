package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;

public class RemoteControlConfig {
    @Config(key = "webhookLogs")
    public static boolean webhookLogs;
    @Config(key = "webhookStatus")
    public static boolean webhookStatus;
    @Config(key = "enableRemoteControl")
    public static boolean enableRemoteControl;
    @Config(key = "websocketPassword")
    public static String websocketPassword;
    @Config(key = "websocketIP")
    public static String websocketIP;
    @Config(key = "webhookStatusCooldown")
    public static double webhookStatusCooldown;
    @Config(key = "webhookURL")
    public static String webhookURL;
}
