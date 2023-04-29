package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class MiscConfig {
    @Config()
    public static boolean xray = false;
    @Config()
    public static boolean autoGodPot = false;
    @Config()
    public static boolean autoCookie = false;
    @Config()
    public static boolean debugMode = false;
    @Config()
    public static boolean ungrab = true;
    @Config()
    public static boolean muteGame = false;
}
