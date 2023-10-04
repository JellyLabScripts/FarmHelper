package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.event.BlockChangeEvent;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.hud.ProfitCalculatorHUD;
import com.github.may2beez.farmhelperv2.util.APIUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.CircularFifoQueue;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockReed;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProfitCalculator implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private static ProfitCalculator instance;

    public static ProfitCalculator getInstance() {
        if (instance == null) {
            instance = new ProfitCalculator();
        }
        return instance;
    }

    public long realProfit = 0;
    public double blocksBroken = 0;

    public String getRealProfitString() {
        return formatter.format(realProfit);
    }

    public String getProfitPerHourString() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled()) return formatter.format(0);
        return formatter.format(realProfit * 3_600_000 / MacroHandler.getInstance().getMacroingTimer().getElapsedTime()) + "/hr";
    }

    public String getBPS() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled()) return "0.0 BPS";
        return Math.round(blocksBroken / MacroHandler.getInstance().getMacroingTimer().getElapsedTime() * 10000f) / 10f + " BPS";
    }

    public final HashMap<String, Integer> itemsDropped = new HashMap<>();

    public final List<BazaarItem> cropsToCount = new ArrayList<BazaarItem>() {{
        // enchanted hay bale
        int HAY_ENCHANTED_TIER_1 = 144;
        int ENCHANTED_TIER_1 = 160;
        int ENCHANTED_TIER_2 = 25600;

        add(new BazaarItem("Hay Bale", "ENCHANTED_HAY_BLOCK", HAY_ENCHANTED_TIER_1, 54).setImage());
        add(new BazaarItem("Seeds", "ENCHANTED_SEEDS", ENCHANTED_TIER_1, 3).setImage());
        add(new BazaarItem("Carrot", "ENCHANTED_CARROT", ENCHANTED_TIER_1, 3).setImage());
        add(new BazaarItem("Potato", "ENCHANTED_POTATO", ENCHANTED_TIER_1, 3).setImage());
        add(new BazaarItem("Melon", "ENCHANTED_MELON_BLOCK", ENCHANTED_TIER_2, 2).setImage());
        add(new BazaarItem("Pumpkin", "ENCHANTED_PUMPKIN", ENCHANTED_TIER_2, 10).setImage());
        add(new BazaarItem("Sugar Cane", "ENCHANTED_SUGAR_CANE", ENCHANTED_TIER_2, 4).setImage());
        add(new BazaarItem("Cocoa Beans", "ENCHANTED_COCOA", ENCHANTED_TIER_1, 3).setImage());
        add(new BazaarItem("Nether Wart", "MUTANT_NETHER_STALK", ENCHANTED_TIER_2, 4).setImage());
        add(new BazaarItem("Cactus Green", "ENCHANTED_CACTUS", ENCHANTED_TIER_2, 3).setImage());
        add(new BazaarItem("Red Mushroom", "ENCHANTED_RED_MUSHROOM", ENCHANTED_TIER_1, 10).setImage());
        add(new BazaarItem("Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM", ENCHANTED_TIER_1, 10).setImage());
    }};

    public final List<BazaarItem> rngDropToCount = new ArrayList<BazaarItem>() {{
        add(new BazaarItem("Cropie", "CROPIE", 1, 25_000).setImage());
        add(new BazaarItem("Squash", "SQUASH", 1, 75_000).setImage());
        add(new BazaarItem("Fermento", "FERMENTO", 1, 250_000).setImage());
        add(new BazaarItem("Burrowing Spores", "BURROWING_SPORES", 1, 1).setImage());
    }};

    public HashMap<String, APICrop> bazaarPrices = new HashMap<>();

    @Getter
    private final Clock updateClock = new Clock();
    @Getter
    private final Clock updateBazaarClock = new Clock();
    public static final List<String> rngDropItemsList = Arrays.asList("Cropie", "Squash", "Fermento", "Burrowing Spores");

    @Override
    public String getName() {
        return "Profit Calculator";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public void stop() {
        updateClock.reset();
        updateBazaarClock.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isActivated() {
        return true;
    }

    public void resetProfits() {
        if (!ProfitCalculatorHUD.resetStatsBetweenDisabling) {
            realProfit = 0;
            blocksBroken = 0;
            itemsDropped.clear();
            cropsToCount.forEach(crop -> crop.currentAmount = 0);
            rngDropToCount.forEach(drop -> drop.currentAmount = 0);
        }
    }

    @SubscribeEvent
    public void onTickUpdateProfit(TickEvent.ClientTickEvent event) {

    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (event.pos.distanceSq(mc.thePlayer.getPositionVector().xCoord, mc.thePlayer.getPositionVector().yCoord, mc.thePlayer.getPositionVector().zCoord) > 20) return;

        switch (MacroHandler.getInstance().getCrop()) {
            case NETHER_WART:
                break;
            case CARROT:
            case POTATO:
            case WHEAT:
                if (event.old.getBlock() instanceof BlockCrops && !(event.update.getBlock() instanceof BlockCrops) ||
                        event.old.getBlock() instanceof BlockNetherWart && !(event.update.getBlock() instanceof BlockNetherWart)) {
                    blocksBroken++;
                }
                break;
            case SUGAR_CANE:
                if (event.old.getBlock() instanceof BlockReed && !(event.update.getBlock() instanceof BlockReed)) {
                    blocksBroken += 0.5;
                }
                break;
            case MELON:
                if (event.old.getBlock().equals(Blocks.melon_block) && !event.update.getBlock().equals(Blocks.melon_block)) {
                    blocksBroken++;
                }
                break;
            case PUMPKIN:
                if (event.old.getBlock().equals(Blocks.pumpkin) && !event.update.getBlock().equals(Blocks.pumpkin)) {
                    blocksBroken++;
                }
                break;
            case CACTUS:
                if (event.old.getBlock().equals(Blocks.cactus) && !event.update.getBlock().equals(Blocks.cactus)) {
                    blocksBroken += 0.5;
                }
                break;
            case COCOA_BEANS:
                if (event.old.getBlock().equals(Blocks.cocoa) && !event.update.getBlock().equals(Blocks.cocoa)) {
                    blocksBroken++;
                }
                break;
            case MUSHROOM:
                if (event.old.getBlock().equals(Blocks.red_mushroom_block) && !event.update.getBlock().equals(Blocks.red_mushroom_block) ||
                        event.old.getBlock().equals(Blocks.brown_mushroom_block) && !event.update.getBlock().equals(Blocks.brown_mushroom_block)) {
                    blocksBroken++;
                }
                break;
        }
    }

    private boolean cantConnectToApi = false;

    @SubscribeEvent
    public void onTickUpdateBazaarPrices(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (updateBazaarClock.passed()) {
            updateBazaarClock.schedule(1000 * 60 * 5);
            LogUtils.sendDebug("Updating Bazaar Prices");
            Multithreading.schedule(this::fetchBazaarPrices, 0, TimeUnit.MILLISECONDS);
        }
    }

    public void fetchBazaarPrices() {
        try {
            String url = "https://api.hypixel.net/skyblock/bazaar";
            String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
            JsonObject request = APIUtils.readJsonFromUrl(url, "User-Agent", userAgent);
            if (request == null) {
                LogUtils.sendDebug("Failed to update bazaar prices");
                cantConnectToApi = true;
                return;
            }
            JsonObject json = request.getAsJsonObject();
            JsonObject json1 = json.getAsJsonObject("products");

            getPrices(json1, cropsToCount);

            getPrices(json1, rngDropToCount);

            LogUtils.sendDebug("Bazaar prices updated");
            cantConnectToApi = false;

        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.sendDebug("Failed to update bazaar prices");
            cantConnectToApi = true;
        }
    }

    private void getPrices(JsonObject json1, List<BazaarItem> cropsToCount) {
        for (BazaarItem item : cropsToCount) {
            JsonObject json2 = json1.getAsJsonObject(item.bazaarId);
            JsonArray json3 = json2.getAsJsonArray("buy_summary");
            JsonObject json4 = json3.size() > 1 ? json3.get(1).getAsJsonObject() : json3.get(0).getAsJsonObject();

            double buyPrice = json4.get("pricePerUnit").getAsDouble();
            if (bazaarPrices.get(item.localizedName) == null) {
                APICrop apiCrop = new APICrop();
                apiCrop.currentPrice = buyPrice;
                apiCrop.previousPrices = new CircularFifoQueue<>(10);
                bazaarPrices.put(item.localizedName, apiCrop);
            } else {
                APICrop apiCrop = bazaarPrices.get(item.localizedName);
                apiCrop.previousPrices.add(apiCrop.currentPrice);
                apiCrop.currentPrice = buyPrice;
            }
        }
    }


    public static String getImageName(String name) {
        switch (name) {
            case "Hay Bale":
                return "ehaybale.png";
            case "Seeds":
                return "eseeds.png";
            case "Carrot":
                return "ecarrot.png";
            case "Potato":
                return "epotato.png";
            case "Melon":
                return "emelon.png";
            case "Pumpkin":
                return "epumpkin.png";
            case "Sugar Cane":
                return "ecane.png";
            case "Cocoa Beans":
                return "ecocoabeans.png";
            case "Nether Wart":
                return "mnw.png";
            case "Cactus Green":
                return "ecactus.png";
            case "Red Mushroom":
                return "eredmushroom.png";
            case "Brown Mushroom":
                return "ebrownmushroom.png";
            case "Cropie":
                return "cropie.png";
            case "Squash":
                return "squash.png";
            case "Fermento":
                return "fermento.png";
            case "Burrowing Spores":
                return "burrowingspores.png";
            default:
                throw new IllegalArgumentException("No image for " + name);
        }
    }

    public static class BazaarItem {
        public String localizedName;
        public String bazaarId;
        public int amountToEnchanted;
        public float currentAmount;
        public String imageURL;
        public int npcPrice;

        public BazaarItem(String localizedName, String bazaarId, int amountToEnchanted, int npcPrice) {
            this.localizedName = localizedName;
            this.bazaarId = bazaarId;
            this.amountToEnchanted = amountToEnchanted;
            this.npcPrice = npcPrice;
            this.currentAmount = 0;
        }

        public BazaarItem setImage() {
            this.imageURL = "/farmhelper/textures/gui/" + getImageName(localizedName);
            return this;
        }
    }

    public static class APICrop {
        public double currentPrice;
        public CircularFifoQueue<Double> previousPrices;
        public boolean isManipulated() {
            double average = previousPrices.stream().mapToDouble(a -> a).average().orElse(currentPrice);
            return currentPrice > average * 2;
        }
    }
}
