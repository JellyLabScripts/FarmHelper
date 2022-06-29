package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.FarmHelperConfig;

public class SchedulerConfig {
    public static boolean scheduler;
    public static boolean statusGUI;
    public static double farmTime;
    public static double breakTime;

    public static void update() {
        scheduler = (boolean) FarmHelperConfig.get("scheduler");
        statusGUI = (boolean) FarmHelperConfig.get("statusGUI");
        farmTime = (double) FarmHelperConfig.get("farmTime");
        breakTime = (double) FarmHelperConfig.get("breakTime");
    }
}
