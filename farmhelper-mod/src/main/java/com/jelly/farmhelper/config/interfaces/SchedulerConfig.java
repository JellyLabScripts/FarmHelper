package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class SchedulerConfig {
    @Config()
    public static boolean scheduler = false;
    @Config()
    public static boolean statusGUI = true;
    @Config()
    public static double farmTime = 60.0;
    @Config()
    public static double breakTime = 5.0;
    @Config()
    public static double bro = 69.0;
}
