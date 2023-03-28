package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.APIHelper;
import gg.essential.elementa.state.BasicState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import org.json.simple.JSONObject;

import java.util.HashMap;
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
    public static float realProfit = 0;
    public static final int ENCHANTED_TIER_1 = 160;
    public static final int ENCHANTED_TIER_2 = 25600;
    public static final int HAY_ENCHANTED_TIER_1 = 144; // enchanted hay bale

    public static HashMap<CropEnum, Double> cropBazaarPrice = new HashMap<>();
    public static HashMap<ArmorDrop, Double> armorDropBazaarPrice = new HashMap<>();
    public static long lastTickCropCount = 0;
    public static long cumulativeCropCount = 0;
    public static long armorDropProfit = 0;

    public enum RNG {
        UNCOMMON,
        RARE,
        CRAZY_RARE,
        PRAY
    }

    public enum ArmorDrop {
        FERMENTO ("FERMENTO"),
        CROPIE ("CROPIE"),
        SQUASH ("SQUASH");

        final String formalName;
        ArmorDrop(String formalName) {
            this.formalName = formalName;
        }
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
                case COCOA_BEANS: // add detection later
                case CACTUS:
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
                // Add detection
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

    // https://hypixel-skyblock.fandom.com/wiki/Melon_Dicer, https://hypixel-skyblock.fandom.com/wiki/Pumpkin_Dicer collection
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

    public static void addArmorDropProfit(ArmorDrop drop) {
        armorDropProfit += armorDropBazaarPrice.get(drop);
    }

    private static void updateProfit() {
        counter.set(Utils.formatNumber(PlayerUtils.getCounter()));
        runtime.set(LogUtils.getRuntimeFormat());

        float enchantedCount = 0;
        switch (FarmConfig.cropType) {
            case NETHERWART:
            case SUGARCANE:
            case POTATO:
            case MELON:
            case CACTUS:
                enchantedCount = cumulativeCropCount * 1.0f / ENCHANTED_TIER_2;
                break;
            case CARROT:
            case COCOA_BEANS:
            case PUMPKIN:
                enchantedCount = cumulativeCropCount * 1.0f / ENCHANTED_TIER_1;
                break;
            case WHEAT:
                enchantedCount = cumulativeCropCount * 1.0f / HAY_ENCHANTED_TIER_1;
                break;

        }
        enchantedCropCount.set(Utils.formatNumber(enchantedCount));
        realProfit = (float) (enchantedCount * cropBazaarPrice.get(FarmConfig.cropType) + armorDropProfit);

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
        armorDropProfit = 0;
        cumulativeCropCount = 0;
        lastTickCropCount = 0;
    }

    public static double getHourProfit(double total) {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime) > 0) {
            return 3600f * total / TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime);
        }
        return 0;
    }

    public static String getHighTierCommonName(CropEnum type, boolean formalName) {
        switch (type) {
            case NETHERWART:
                return formalName ? "MUTANT_NETHER_STALK" : "Mutant Nether Wart";
            case CARROT:
                return formalName ? "ENCHANTED_CARROT" : "Enchanted Carrot";
            case POTATO:
                return formalName ? "ENCHANTED_BAKED_POTATO" : "Enchanted Baked Potato";
            case WHEAT:
                return formalName ? "ENCHANTED_HAY_BLOCK" :"Enchanted Hay Bale";
            case SUGARCANE:
                return formalName ? "ENCHANTED_SUGAR_CANE" :"Enchanted Sugar Cane";
            case COCOA_BEANS:
                return formalName ? "ENCHANTED_COCOA" :"Enchanted Cocoa Bean";
            case MELON:
                return formalName ? "ENCHANTED_MELON_BLOCK" : "Enchanted Melon Block";
            case PUMPKIN:
                return formalName ? "ENCHANTED_PUMPKIN" : "Enchanted Pumpkin";
            case CACTUS:
                return formalName ? "ENCHANTED_CACTUS" : "Enchanted Cactus";
            case MUSHROOM:
                return formalName ? "ENCHANTED_BROWN_MUSHROOM" : "Enchanted Brown Mushroom";
            default:
                return formalName ? "UNKNOWN" : "Unknown";
        }
    }



    public static void fetchBazaarPrices() {
        try {
            JSONObject json = APIHelper.readJsonFromUrl("https://api.hypixel.net/skyblock/bazaar","User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            JSONObject json1 = (JSONObject) json.get("products");

            for (int i = 0; i < CropEnum.values().length; i++) {
                JSONObject json2 = (JSONObject)json1.get(getHighTierCommonName(CropEnum.values()[i], true));
                JSONObject json3 = (JSONObject)json2.get("quick_status");
                cropBazaarPrice.put(CropEnum.values()[i], (Double) (json3).get("buyPrice")); // assume sell order
            }
            for (int i = 0; i < ArmorDrop.values().length; i++) {
                JSONObject json2 = (JSONObject)json1.get(ArmorDrop.values()[i].formalName);
                JSONObject json3 = (JSONObject)json2.get("quick_status");
                armorDropBazaarPrice.put(ArmorDrop.values()[i], (Double) (json3).get("buyPrice")); // assume sell order
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }

}