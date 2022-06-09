package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.FarmHelperConfig;

public class MiscConfig {
    public static boolean resync;
    public static boolean fastbreak;
    public static boolean autoGodPot;
    public static boolean autoCookie;
    public static boolean dropStone;
    public static boolean debugMode;
    public static boolean ungrab;
    public static double banThreshold;

    public static void update() {
        resync = (boolean) FarmHelperConfig.get("resync");
        fastbreak = (boolean) FarmHelperConfig.get("fastbreak");
        autoGodPot = (boolean) FarmHelperConfig.get("autoGodPot");
        autoCookie = (boolean) FarmHelperConfig.get("autoCookie");
        dropStone = (boolean) FarmHelperConfig.get("dropStone");
        ungrab = (boolean) FarmHelperConfig.get("ungrab");
        debugMode = (boolean) FarmHelperConfig.get("debugMode");
        banThreshold = (double) FarmHelperConfig.get("banThreshold");
    }
}
