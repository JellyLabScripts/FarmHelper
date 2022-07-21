package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class AutoSellConfig {
    @Config(key = "autoSell")
    public static boolean autoSell;
    @Config(key = "fullTime")
    public static double fullTime;
    @Config(key = "fullRatio")
    public static double fullRatio;
}
