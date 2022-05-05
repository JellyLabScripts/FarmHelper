package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.config.interfaces.FarmConfig;
import gg.essential.elementa.state.BasicState;
import me.acattoXD.WartMacro;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProfitUtils {
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
        redMushroomCount.set(Utils.formatNumber(getCropDiff("§fRed Mushroom") / 160));
        brownMushroomCount.set(Utils.formatNumber(getCropDiff("§fBrown Mushroom") / 160));
        switch (FarmConfig.cropType) {
            case NETHERWART:
                cropCount.set(Utils.formatNumber(getCropDiff("§fNether Wart") / 25600));
                break;
            case CARROT:
                cropCount.set(Utils.formatNumber(getCropDiff("§fCarrot") / 160));
                break;
            case POTATO:
                cropCount.set(Utils.formatNumber(getCropDiff("§fPotato") / 25600));
                break;
            case WHEAT:
                cropCount.set(Utils.formatNumber(getCropDiff("§fWheat") / 1296));
                break;
        }
        double realProfit = getCropDiff("§fNether Wart") * 2 +
            getCropDiff("§fCarrot") * 1.5625 +
            getCropDiff("§fPotato") * 1.2065 +
            getCropDiff("§fWheat") * 6.52 +
            getCropDiff("§fRed Mushroom") * 4 +
            getCropDiff("§fBrown Mushroom") * 4;
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
        InventoryUtils.resetPreviousInventory();
    }

    public static int getCropDiff(String crop) {
        List<ItemDiff> cropDiff = (List<ItemDiff>) InventoryUtils.itemPickupLog.get(crop);
        if (cropDiff == null) return 0;
        if (cropDiff.size() == 0) return 0;
        if (cropDiff.size() == 1) return Math.max(cropDiff.get(0).getAmount(), 0);
        return Math.max(cropDiff.get(0).getAmount(), cropDiff.get(1).getAmount());
    }

    public static double getHourProfit(double total) {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - WartMacro.startTime) > 0) {
            return 3600f * total / TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - WartMacro.startTime);
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
        return (WartMacro.currentCounter - WartMacro.startCounter) / 25600;
    }

    public static int getTier2() {
        return ((WartMacro.currentCounter - WartMacro.startCounter) % 25600) / 160;
    }

    public static int getTier1() {
        return ((WartMacro.currentCounter - WartMacro.startCounter) % 25600) % 160;
    }
}
