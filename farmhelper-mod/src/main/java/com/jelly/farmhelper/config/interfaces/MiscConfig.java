package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;

public class MiscConfig {
    @Config(key = "resync")
    public static boolean resync;
    @Config(key = "fastbreak")
    public static boolean fastbreak;
    @Config(key = "fastbreakSpeed")
    public static double fastbreakSpeed;
    @Config(key = "xray")
    public static boolean xray;
    @Config(key = "randomization")
    public static boolean randomization;
    @Config(key = "autoGodPot")
    public static boolean autoGodPot;
    @Config(key = "autoCookie")
    public static boolean autoCookie;
    @Config(key = "dropStone")
    public static boolean dropStone;
    @Config(key = "debugMode")
    public static boolean debugMode;
    @Config(key = "ungrab")
    public static boolean ungrab;
    @Config(key = "banwaveDisconnect")
    public static boolean banwaveDisconnect;
    @Config(key = "banThreshold")
    public static double banThreshold;
    @Config(key = "reconnectDelay")
    public static double reconnectDelay;
}
