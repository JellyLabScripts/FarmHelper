package com.jelly.FarmHelper.config.interfaces;

import com.jelly.FarmHelper.config.FarmHelperConfig;

public class ProfitCalculatorConfig {
    public static boolean profitCalculator;
    public static boolean totalProfit;
    public static boolean profitHour;
    public static boolean highTierItem;
    public static boolean lowTierItem;
    public static boolean runtime;

    public static void update() {
        profitCalculator = (boolean) FarmHelperConfig.get("profitCalculator");
        totalProfit = (boolean) FarmHelperConfig.get("totalProfit");
        profitHour = (boolean) FarmHelperConfig.get("profitHour");
        highTierItem = (boolean) FarmHelperConfig.get("highTierItem");
        lowTierItem = (boolean) FarmHelperConfig.get("lowTierItem");
        runtime = (boolean) FarmHelperConfig.get("runtime");
    }
}
