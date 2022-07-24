package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class JacobConfig {
    @Config()
    public static boolean jacobFailsafe = true;
    @Config()
    public static int mushroomCap = 200000;
    @Config()
    public static int netherWartCap = 400000;
    @Config()
    public static int carrotCap = 400000;
    @Config()
    public static int potatoCap = 400000;
    @Config()
    public static int wheatCap = 400000;
    @Config()
    public static int sugarcaneCap = 400000;
}