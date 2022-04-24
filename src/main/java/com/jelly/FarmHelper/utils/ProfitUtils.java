package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.FarmHelper;
import com.jelly.FarmHelper.config.interfaces.FarmConfig;

import java.util.concurrent.TimeUnit;

public class ProfitUtils {
    public static float getHourProfit(int total) {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - FarmHelper.startTime) > 0) {
            return 3600f * total / TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - FarmHelper.startTime);
        }
        return 0;
    }

    public static int getProfit() {
        switch (FarmConfig.cropType) {
            case NETHERWART:
                return getNWProfit();
            case CARROT:
                return getCarrotProfit();
            default:
                return 0;
        }
    }

    public static int getHighTierCount() {
        switch (FarmConfig.cropType) {
            case NETHERWART:
                return getTier3();
            case CARROT:
                return getTier3() * 160 + getTier2();
            default:
                return 0;
        }
    }

    public static String getHighTierName() {
        switch (FarmConfig.cropType) {
            case NETHERWART:
                return "Mutant Netherwart";
            case CARROT:
                return "Enchanted Carrots";
            case POTATO:
                return "Enchanted Baked Potatoes";
            case WHEAT:
                return "Enchanted Hay Bales";
            default:
                return "Unknown";
        }
    }

    public static int getNWProfit() {
        return getTier3() * 51200 + getTier2() * 320;
    }

    public static int getCarrotProfit() {
        return ((getTier3() * 160) + getTier2()) * 240 + getTier1() * 2;
    }

    public static int getTier3() {
        return (FarmHelper.currentCounter - FarmHelper.startCounter) / 25600;
    }

    public static int getTier2() {
        return ((FarmHelper.currentCounter - FarmHelper.startCounter) % 25600) / 160;
    }

    public static int getTier1() {
        return ((FarmHelper.currentCounter - FarmHelper.startCounter) % 25600) % 160;
    }
}
