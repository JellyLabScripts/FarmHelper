package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.FarmHelperConfig;

public class JacobConfig {
    public static boolean jacobFailsafe;
    public static Integer mushroomCap;
    public static Integer netherWartCap;
    public static Integer carrotCap;
    public static Integer potatoCap;
    public static Integer wheatCap;

    public static void update() {
        jacobFailsafe = (Boolean) FarmHelperConfig.get("jacobFailsafe");
        mushroomCap = ((Long) FarmHelperConfig.get("mushroomCap")).intValue();
        netherWartCap = ((Long) FarmHelperConfig.get("netherWartCap")).intValue();
        carrotCap = ((Long) FarmHelperConfig.get("carrotCap")).intValue();
        potatoCap = ((Long) FarmHelperConfig.get("potatoCap")).intValue();
        wheatCap = ((Long) FarmHelperConfig.get("wheatCap")).intValue();
    }
}