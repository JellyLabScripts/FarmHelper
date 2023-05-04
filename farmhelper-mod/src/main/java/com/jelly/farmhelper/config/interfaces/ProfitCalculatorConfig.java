package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;

public class ProfitCalculatorConfig {
    @Config()
    public static boolean profitCalculator = false;
    @Config()
    public static boolean totalProfit = true;
    @Config()
    public static boolean profitHour = true;
    @Config()
    public static boolean itemCount = true;
    @Config()
    public static boolean countRNGtoHourly = true;
    @Config()
    public static boolean blocksPerSecond = true;
    @Config()
    public static boolean runtime = true;
}
