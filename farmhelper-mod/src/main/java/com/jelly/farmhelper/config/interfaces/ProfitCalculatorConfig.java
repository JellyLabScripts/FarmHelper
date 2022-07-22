package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;

public class ProfitCalculatorConfig {
    @Config(key = "profitCalculator")
    public static boolean profitCalculator;
    @Config(key = "totalProfit")
    public static boolean totalProfit;
    @Config(key = "profitHour")
    public static boolean profitHour;
    @Config(key = "itemCount")
    public static boolean itemCount;
    @Config(key = "mushroomCount")
    public static boolean mushroomCount;
    @Config(key = "counter")
    public static boolean counter;
    @Config(key = "runtime")
    public static boolean runtime;
}
