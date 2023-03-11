package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class AutoSellConfig {
    @Config()
    public static boolean autoSell = false;
    @Config()
    public static boolean sellToNPC = false;
    @Config()
    public static double fullTime = 6.0;
    @Config()
    public static double fullRatio = 65.0;
}
