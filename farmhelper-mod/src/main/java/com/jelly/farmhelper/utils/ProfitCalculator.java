package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import gg.essential.elementa.state.BasicState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;

import java.util.concurrent.TimeUnit;

public class ProfitCalculator {
    static Minecraft mc = Minecraft.getMinecraft();
    public static final BasicState<String> profit = new BasicState<>("$0");
    public static final BasicState<String> profitHr = new BasicState<>("$0");
    public static final BasicState<String> enchantedCropCount = new BasicState<>("0");
    public static final BasicState<String> redMushroomCount = new BasicState<>("0");
    public static final BasicState<String> brownMushroomCount = new BasicState<>("0");
    public static final BasicState<String> counter = new BasicState<>("0");
    public static final BasicState<String> runtime = new BasicState<>("0h 0m 0s");
    public static double realProfit = 0;

    public static final int ENCHANTED_TIER_1 = 160;
    public static final int ENCHANTED_TIER_2 = 25600;
    public static final int HAY_ENCHANTED_TIER_1 = 144; // enchanted hay bale

    public static long lastTickCropCount = 0;
    public static long cumulativeCropCount = 0;

    public enum RNG {
        UNCOMMON,
        RARE,
        CRAZY_RARE,
        PRAY
    }

    public static void iterateInventory(){
        long newCropCount = 0, cropDiff;
        for(Slot s : mc.thePlayer.inventoryContainer.inventorySlots){
            if(!s.getHasStack())
                continue;

            switch (FarmConfig.cropType){
                case WHEAT:
                    if(!(s.getStack().getItem() instanceof ItemBlock) || !((ItemBlock) s.getStack().getItem()).getBlock().equals(Blocks.hay_block))
                        continue;
                    break;
                case CARROT:
                    if(!s.getStack().getItem().equals(Items.carrot))
                        continue;
                    break;
                case COCOA_BEANS:
                    if(!s.getStack().getItem().equals(Items.dye))
                        continue;
                    break;
                case MELON:
                    if(!s.getStack().getItem().equals(Items.melon))
                        continue;
                    break;
                case PUMPKIN:
                    if(!(s.getStack().getItem() instanceof ItemBlock) || !((ItemBlock) s.getStack().getItem()).getBlock().equals(Blocks.pumpkin))
                        continue;
                    break;
                case POTATO:
                    if(!s.getStack().getItem().equals(Items.potato))
                        continue;
                    break;
                case SUGARCANE:
                    if(!s.getStack().getItem().equals(Items.reeds))
                        continue;
                    break;
                case NETHERWART:
                    if(!s.getStack().getItem().equals(Items.nether_wart))
                        continue;
                    break;
            }
            newCropCount += s.getStack().stackSize;
        }

        if(lastTickCropCount == 0) {
            lastTickCropCount = newCropCount;
            return;
        }

        if(newCropCount < lastTickCropCount)
            cropDiff = newCropCount - lastTickCropCount + (FarmConfig.cropType == CropEnum.WHEAT ? HAY_ENCHANTED_TIER_1 : ENCHANTED_TIER_1);
        else
            cropDiff = newCropCount - lastTickCropCount;

        lastTickCropCount = newCropCount;
        cumulativeCropCount += cropDiff;

        updateProfit();
    }

    // https://hypixel-skyblock.fandom.com/wiki/Melon_Dicer
    // https://hypixel-skyblock.fandom.com/wiki/Pumpkin_Dicer
    // collection equivalent
    public static void addRNGProfit(RNG rng) {
        if(FarmConfig.cropType != CropEnum.PUMPKIN && FarmConfig.cropType != CropEnum.MELON)
            return;

        switch (rng) {
            case UNCOMMON:
                cumulativeCropCount += FarmConfig.cropType == CropEnum.PUMPKIN ? 64 : 160;
            case RARE:
                cumulativeCropCount += FarmConfig.cropType == CropEnum.PUMPKIN ? 160 : 800;
            case CRAZY_RARE:
                cumulativeCropCount += FarmConfig.cropType == CropEnum.PUMPKIN ? 1600 : 8000;
            case PRAY:
                cumulativeCropCount += FarmConfig.cropType == CropEnum.PUMPKIN ? 10240 : 51200;
        }
        updateProfit();
    }

    private static void updateProfit() {
        counter.set(Utils.formatNumber(PlayerUtils.getCounter()));
        runtime.set(LogUtils.getRuntimeFormat());

        //TODO: Change these price respectively to max(bazaarPirce, npcPrice) (fetch)
        switch (FarmConfig.cropType) {
            case NETHERWART:
            case SUGARCANE:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / ENCHANTED_TIER_2));
                realProfit = cumulativeCropCount * 2;
                break;
            case CARROT:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / ENCHANTED_TIER_1));
                realProfit = cumulativeCropCount * 1.5625;
                break;
            case POTATO:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / ENCHANTED_TIER_2));
                realProfit = cumulativeCropCount * 1.2065;
                break;
            case WHEAT:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / HAY_ENCHANTED_TIER_1));
                realProfit = cumulativeCropCount * 6.52;
                break;
            case COCOA_BEANS:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f/ ENCHANTED_TIER_1));
                realProfit = cumulativeCropCount * 1.5; //TODO: Change, temp only
                break;
            case PUMPKIN:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / ENCHANTED_TIER_1));
                realProfit = cumulativeCropCount * 2;
                break;
            case MELON:
                enchantedCropCount.set(Utils.formatNumber(cumulativeCropCount * 1.0f / ENCHANTED_TIER_2));
                realProfit = cumulativeCropCount * 3;
                break;
        }

        profit.set("$" + Utils.formatNumber(Math.round(realProfit)));
        profitHr.set("$" + Utils.formatNumber(Math.round(getHourProfit(realProfit))));

    }


    public static void resetProfit() {
        profit.set("$0");
        profitHr.set("$0");
        enchantedCropCount.set("0");
        redMushroomCount.set("0");
        brownMushroomCount.set("0");
        counter.set("0");
        runtime.set("0h 0m 0s");
        cumulativeCropCount = 0;
        lastTickCropCount = 0;
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
            case SUGARCANE:
                return "Enchanted Sugar Cane";
            case COCOA_BEANS:
                return "Enchanted Cocoa Bean";
            case MELON:
                return "Enchanted Melon Block";
            case PUMPKIN:
                return "Enchanted Pumpkin";
            default:
                return "Unknown";
        }
    }
}