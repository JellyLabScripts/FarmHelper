package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;

public class JacobConfig {
    @Config(key = "jacobFailsafe")
    public static boolean jacobFailsafe;
    @Config(key = "mushroomCap")
    public static int mushroomCap;
    @Config(key = "netherWartCap")
    public static int netherWartCap;
    @Config(key = "carrotCap")
    public static int carrotCap;
    @Config(key = "potatoCap")
    public static int potatoCap;
    @Config(key = "wheatCap")
    public static int wheatCap;
    @Config(key = "sugarcaneCap")
    public static int sugarcaneCap;
}