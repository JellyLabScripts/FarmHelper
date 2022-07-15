package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.FarmHelperConfig;

public class RemoteControlConfig {
    public static boolean webhookLogs;
    public static boolean webhookStatus;
    public static boolean enableRemoteControl;
    public static String websocketPassword;
    public static String websocketIP;

    public static double webhookStatusCooldown;
    public static String webhookURL;

    public static void update() {
        enableRemoteControl = (boolean) FarmHelperConfig.get("enableRemoteControl");
        websocketPassword = (String) FarmHelperConfig.get("websocketPassword");
        websocketIP = (String) FarmHelperConfig.get("websocketIP");

        webhookLogs = (boolean) FarmHelperConfig.get("webhookLogs");
        webhookStatus = (boolean) FarmHelperConfig.get("webhookStatus");
        webhookStatusCooldown = (double) FarmHelperConfig.get("webhookStatusCooldown");
        webhookURL = (String) FarmHelperConfig.get("webhookURL");
    }
}
