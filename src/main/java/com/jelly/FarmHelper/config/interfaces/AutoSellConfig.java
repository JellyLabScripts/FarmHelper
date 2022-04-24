package com.jelly.FarmHelper.config.interfaces;

import com.jelly.FarmHelper.config.FarmHelperConfig;

public class AutoSellConfig {
    public static boolean autoSell;
    public static double fullTime;
    public static double fullRatio;

    public static void update() {
        autoSell = (boolean) FarmHelperConfig.get("autoSell");
        fullTime = (double) FarmHelperConfig.get("fullTime");
        fullRatio = (double) FarmHelperConfig.get("fullRatio");
    }
}
