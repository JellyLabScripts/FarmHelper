package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.datastructures.ItemDiff;
import com.jelly.farmhelper.macros.MacroHandler;
import gg.essential.elementa.state.BasicState;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfitUtils {
    private static int lastCounter = 0;
    public static final BasicState<String> profit = new BasicState<>("$0");
    public static final BasicState<String> profitHr = new BasicState<>("$0");
    public static final BasicState<String> cropCount = new BasicState<>("0");
    public static final BasicState<String> redMushroomCount = new BasicState<>("0");
    public static final BasicState<String> brownMushroomCount = new BasicState<>("0");
    public static final BasicState<String> counter = new BasicState<>("0");
    public static final BasicState<String> runtime = new BasicState<>("0h 0m 0s");

    public static void updateProfitState() {
        counter.set(Utils.formatNumber(InventoryUtils.getCounter()));
        runtime.set(LogUtils.getRuntimeFormat());
        // redMushroomCount.set(Utils.formatNumber(getCropDiff("§fRed Mushroom") / 160));
        // brownMushroomCount.set(Utils.formatNumber(getCropDiff("§fBrown Mushroom") / 160));
        double realProfit = 0;
        switch (FarmConfig.cropType) {
            case NETHERWART:
                cropCount.set(Utils.formatNumber(getCropDiff("§fNether Wart") / 25600));
                realProfit = getCropDiff("") * 2;
                break;
            case SUGARCANE:
                cropCount.set(Utils.formatNumber(getCropDiff("§fSugar Cane") / 25600));
                realProfit = getCropDiff("") * 2;
                break;
            case CARROT:
                cropCount.set(Utils.formatNumber(getCropDiff("§fCarrot") / 160));
                realProfit = getCropDiff("") * 1.5625;
                break;
            case POTATO:
                cropCount.set(Utils.formatNumber(getCropDiff("§fPotato") / 25600));
                realProfit = getCropDiff("") * 1.2065;
                break;
            case WHEAT:
                cropCount.set(Utils.formatNumber(getCropDiff("§fWheat") / 1296));
                realProfit = getCropDiff("") * 6.52;
                break;
        }
//        double realProfit = getCropDiff("§fNether Wart") * 2 +
//          getCropDiff("§fSugar Cane") * 2 +
//          getCropDiff("§fCarrot") * 1.5625 +
//          getCropDiff("§fPotato") * 1.2065 +
//          getCropDiff("§fWheat") * 6.52 +
//          getCropDiff("§fRed Mushroom") * 4 +
//          getCropDiff("§fBrown Mushroom") * 4;
        profit.set("$" + Utils.formatNumber(Math.round(realProfit)));
        profitHr.set("$" + Utils.formatNumber(Math.round(getHourProfit(realProfit))));
    }

    public static void resetProfit() {
        profit.set("$0");
        profitHr.set("$0");
        cropCount.set("0");
        redMushroomCount.set("0");
        brownMushroomCount.set("0");
        counter.set("0");
        runtime.set("0h 0m 0s");
        // InventoryUtils.resetPreviousInventory();
    }

    public static int getCropDiff(String crop) {
//        List<ItemDiff> cropDiff = (List<ItemDiff>) InventoryUtils.itemPickupLog.get(crop);
//        if (cropDiff == null) return 0;
//        if (cropDiff.size() == 0) return 0;
//        if (cropDiff.size() == 1) return Math.max(cropDiff.get(0).getAmount(), 0);
//        return Math.max(cropDiff.get(0).getAmount(), cropDiff.get(1).getAmount());
        lastCounter = InventoryUtils.getCounter() != 0 ? InventoryUtils.getCounter() : lastCounter;
        return lastCounter - MacroHandler.startCounter;
    }

    public static double getHourProfit(double total) {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime) > 0) {
            return 3600f * total / TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime);
        }
        return 0;
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
}