package com.jelly.FarmHelper.config.interfaces;

import com.jelly.FarmHelper.config.FarmHelperConfig;

public class WebhookConfig {
    public static boolean webhookLogs;
    public static boolean webhookStatus;
    public static double webhookStatusCooldown;
    public static String webhookURL;

    public static void update() {
        webhookLogs = (boolean) FarmHelperConfig.get("webhookLogs");
        webhookStatus = (boolean) FarmHelperConfig.get("webhookStatus");
        webhookStatusCooldown = (double) FarmHelperConfig.get("webhookStatusCooldown");
        webhookURL = (String) FarmHelperConfig.get("webhookURL");
    }
}
