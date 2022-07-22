package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;

public class SchedulerConfig {
    @Config(key = "scheduler")
    public static boolean scheduler;
    @Config(key = "statusGUI")
    public static boolean statusGUI;
    @Config(key = "farmTime")
    public static double farmTime;
    @Config(key = "breakTime")
    public static double breakTime;
}
