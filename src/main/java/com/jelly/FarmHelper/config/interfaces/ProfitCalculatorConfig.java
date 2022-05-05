package com.jelly.FarmHelper.config.interfaces;

import com.jelly.FarmHelper.config.FarmHelperConfig;

public class ProfitCalculatorConfig {
    public static boolean profitCalculator;
    public static boolean totalProfit;
    public static boolean profitHour;
    public static boolean itemCount;
    public static boolean mushroomCount;
    public static boolean counter;
    public static boolean runtime;

    public static void update() {
        profitCalculator = (boolean) FarmHelperConfig.get("profitCalculator");
        totalProfit = (boolean) FarmHelperConfig.get("totalProfit");
        profitHour = (boolean) FarmHelperConfig.get("profitHour");
        itemCount = (boolean) FarmHelperConfig.get("itemCount");
        mushroomCount = (boolean) FarmHelperConfig.get("mushroomCount");
        counter = (boolean) FarmHelperConfig.get("counter");
        runtime = (boolean) FarmHelperConfig.get("runtime");
    }
}
