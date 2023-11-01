package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.ClickedBlockEvent;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
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
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfitCalculator implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    private final NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    {
        formatter.setMaximumFractionDigits(0);
    }
    private final NumberFormat oneDecimalDigitFormatter = NumberFormat.getNumberInstance(Locale.US);
    {
        oneDecimalDigitFormatter.setMaximumFractionDigits(1);
    }
    private static ProfitCalculator instance;

    public static ProfitCalculator getInstance() {
        if (instance == null) {
            instance = new ProfitCalculator();
        }
        return instance;
    }

    public double realProfit = 0;
    public double realHourlyProfit = 0;
    public double bountifulProfit = 0;
    public double blocksBroken = 0;

    public String getRealProfitString() {
        return formatter.format(realProfit);
    }

    public String getProfitPerHourString() {
        return formatter.format(realHourlyProfit) + "/hr";
    }

    public String getBPS() {
        if (!MacroHandler.getInstance().getMacroingTimer().isScheduled()) return "0.0 BPS";
        return oneDecimalDigitFormatter.format(blocksBroken / (MacroHandler.getInstance().getMacroingTimer().getElapsedTime() / 1000f)) + " BPS";
    }

    public final HashMap<String, Integer> itemsDropped = new HashMap<>();

    public final List<BazaarItem> visitorsMacroPrices = new ArrayList<BazaarItem>() {{
        add(new BazaarItem("_Wheat", "WHEAT"));
        add(new BazaarItem("_Enchanted Bread", "ENCHANTED_BREAD"));
        add(new BazaarItem("_Hay Bale", "HAY_BLOCK"));
        add(new BazaarItem("_Enchanted Hay Bale", "ENCHANTED_HAY_BLOCK"));
        add(new BazaarItem("_Tightly-Tied Hay Bale", "TIGHTLY_TIED_HAY_BALE"));

        add(new BazaarItem("-Potato", "POTATO_ITEM"));
        add(new BazaarItem("-Enchanted Potato", "ENCHANTED_POTATO"));
        add(new BazaarItem("-Enchanted Baked Potato", "ENCHANTED_BAKED_POTATO"));

        add(new BazaarItem("_Nether Wart", "NETHER_STALK"));
        add(new BazaarItem("_Enchanted Nether Wart", "ENCHANTED_NETHER_STALK"));
        add(new BazaarItem("_Mutant Nether Wart", "MUTANT_NETHER_STALK"));

        add(new BazaarItem("_Carrot", "CARROT_ITEM"));
        add(new BazaarItem("_Enchanted Carrot", "ENCHANTED_CARROT"));
        add(new BazaarItem("_Enchanted Golden Carrot", "ENCHANTED_GOLDEN_CARROT"));

        add(new BazaarItem("_Cactus", "CACTUS"));
        add(new BazaarItem("_Enchanted Cactus Green", "ENCHANTED_CACTUS_GREEN"));
        add(new BazaarItem("_Enchanted Cactus", "ENCHANTED_CACTUS"));

        add(new BazaarItem("_Sugar Cane", "SUGAR_CANE"));
        add(new BazaarItem("_Enchanted Sugar", "ENCHANTED_SUGAR"));
        add(new BazaarItem("_Enchanted Sugar Cane", "ENCHANTED_SUGAR_CANE"));

        add(new BazaarItem("_Melon", "MELON"));
        add(new BazaarItem("_Enchanted Melon", "ENCHANTED_MELON"));
        add(new BazaarItem("_Melon Block", "MELON_BLOCK"));
        add(new BazaarItem("_Enchanted Melon Block", "ENCHANTED_MELON_BLOCK"));

        add(new BazaarItem("_Cocoa Beans", "INK_SACK:3"));
        add(new BazaarItem("_Enchanted Cocoa Beans", "ENCHANTED_COCOA"));
        add(new BazaarItem("_Enchanted Cookie", "ENCHANTED_COOKIE"));

        add(new BazaarItem("_Red Mushroom", "RED_MUSHROOM"));
        add(new BazaarItem("_Enchanted Red Mushroom", "ENCHANTED_RED_MUSHROOM"));
        add(new BazaarItem("_Red Mushroom Block", "HUGE_MUSHROOM_1"));
        add(new BazaarItem("_Enchanted Red Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_1"));

        add(new BazaarItem("_Brown Mushroom", "BROWN_MUSHROOM"));
        add(new BazaarItem("_Enchanted Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM"));
        add(new BazaarItem("_Brown Mushroom Block", "HUGE_MUSHROOM_2"));
        add(new BazaarItem("_Enchanted Brown Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_2"));

        add(new BazaarItem("_Pumpkin", "PUMPKIN"));
        add(new BazaarItem("_Enchanted Pumpkin", "ENCHANTED_PUMPKIN"));
        add(new BazaarItem("_Polished Pumpkin", "POLISHED_PUMPKIN"));

        add(new BazaarItem("_Raw Porkchop", "PORK"));
        add(new BazaarItem("_Enchanted Pork", "ENCHANTED_PORK"));
        add(new BazaarItem("_Enchanted Grilled Pork", "ENCHANTED_GRILLED_PORK"));

        add(new BazaarItem("_Raw Rabbit", "RABBIT"));
        add(new BazaarItem("_Enchanted Raw Rabbit", "ENCHANTED_RABBIT"));

        add(new BazaarItem("_Compost", "COMPOST"));
        add(new BazaarItem("_Mutton", "MUTTON"));
        add(new BazaarItem("_Enchanted Mutton", "ENCHANTED_MUTTON"));
        add(new BazaarItem("_Enchanted Cookied Mutton", "ENCHANTED_COOKED_MUTTON"));

        add(new BazaarItem("_Seeds", "SEEDS"));
        add(new BazaarItem("_Enchanted Seeds", "ENCHANTED_SEEDS"));
        add(new BazaarItem("_Box of Seeds", "BOX_OF_SEEDS"));
    }};

    public final List<BazaarItem> cropsToCount = new ArrayList<BazaarItem>() {{
        final int HAY_ENCHANTED_TIER_1 = 144;
        final int ENCHANTED_TIER_1 = 160;
        final int ENCHANTED_TIER_2 = 25600;

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
    public final List<String> cropsToCountList = Arrays.asList("Hay Bale", "Seeds", "Carrot", "Potato", "Melon", "Pumpkin", "Sugar Cane", "Cocoa Beans", "Nether Wart", "Cactus Green", "Red Mushroom", "Brown Mushroom");
    public final List<String> rngToCountList = Arrays.asList("Cropie", "Squash", "Fermento", "Burrowing Spores");
    @Override
    public String getName() {
        return "Profit Calculator";
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return true;
    }

    @Override
    public void start() {
        if (ProfitCalculatorHUD.resetStatsBetweenDisabling) {
            resetProfits();
        }
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
    public boolean isToggled() {
        return true;
    }

    public void resetProfits() {
        realProfit = 0;
        realHourlyProfit = 0;
        bountifulProfit = 0;
        blocksBroken = 0;
        itemsDropped.clear();
        cropsToCount.forEach(crop -> crop.currentAmount = 0);
        rngDropToCount.forEach(drop -> drop.currentAmount = 0);
    }

    @SubscribeEvent
    public void onTickUpdateProfit(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!MacroHandler.getInstance().isCurrentMacroEnabled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        double profit = 0;
        for (BazaarItem item : cropsToCount) {
            if (cantConnectToApi) {
                profit += item.currentAmount / item.amountToEnchanted * item.npcPrice;
            } else {
                double price = 0;
                if (!bazaarPrices.containsKey(item.localizedName)) {
                    LogUtils.sendDebug("No price for " + item.localizedName);
                    price = item.npcPrice;
                } else if (bazaarPrices.get(item.localizedName).isManipulated()) {
                    price = bazaarPrices.get(item.localizedName).getMedian();
                } else {
                    price = bazaarPrices.get(item.localizedName).currentPrice;
                }
                profit += (float) (item.currentAmount / item.amountToEnchanted * price);
            }
        }
        double rngPrice = 0;
        for (BazaarItem item : rngDropToCount) {
            if (cantConnectToApi) {
                rngPrice += item.currentAmount * item.npcPrice;
            } else {
                double price;
                if (!bazaarPrices.containsKey(item.localizedName)) {
                    LogUtils.sendDebug("No price for " + item.localizedName);
                    price = item.npcPrice;
                } else if (bazaarPrices.get(item.localizedName).isManipulated()) {
                    price = bazaarPrices.get(item.localizedName).getMedian();
                } else {
                    price = bazaarPrices.get(item.localizedName).currentPrice;
                }
                rngPrice += (float) (item.currentAmount * price);
            }
        }

        ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();
        if (currentItem != null && StringUtils.stripControlCodes(currentItem.getDisplayName()).startsWith("Bountiful")) {
            double value = GameStateHandler.getInstance().getCurrentPurse() - GameStateHandler.getInstance().getPreviousPurse();
            if (value > 0)
                bountifulProfit += value;
        }
        profit += bountifulProfit;
        realProfit = profit;
        realProfit += rngPrice;

        if (ProfitCalculatorHUD.countRNGToProfitCalc) {
            realHourlyProfit = (realProfit / (MacroHandler.getInstance().getMacroingTimer().getElapsedTime() / 1000f / 60 / 60));
        } else {
            realHourlyProfit = profit / (MacroHandler.getInstance().getMacroingTimer().getElapsedTime() / 1000f / 60 / 60);
        }
    }

    @SubscribeEvent
    public void onBlockChange(ClickedBlockEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        switch (MacroHandler.getInstance().getCrop()) {
            case NETHER_WART:
            case CARROT:
            case POTATO:
            case WHEAT:
                if (event.getBlock() instanceof BlockCrops ||
                        event.getBlock() instanceof BlockNetherWart) {
                    blocksBroken++;
                }
                break;
            case SUGAR_CANE:
                if (event.getBlock() instanceof BlockReed) {
                    blocksBroken++;
                }
                break;
            case MELON:
                if (event.getBlock().equals(Blocks.melon_block)) {
                    blocksBroken++;
                }
                break;
            case PUMPKIN:
                if (event.getBlock().equals(Blocks.pumpkin)) {
                    blocksBroken++;
                }
                break;
            case CACTUS:
                if (event.getBlock().equals(Blocks.cactus)) {
                    blocksBroken++;
                }
                break;
            case COCOA_BEANS:
                if (event.getBlock().equals(Blocks.cocoa)) {
                    blocksBroken++;
                }
                break;
            case MUSHROOM:
                if (event.getBlock().equals(Blocks.red_mushroom) ||
                        event.getBlock().equals(Blocks.brown_mushroom)) {
                    blocksBroken++;
                }
                break;
        }
    }

    @SubscribeEvent
    public void onReceivedPacket(ReceivePacketEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!MacroHandler.getInstance().isCurrentMacroEnabled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            int slotNumber = packet.func_149173_d();
            if (slotNumber < 0 || slotNumber > 44) return;
            Slot currentSlot = mc.thePlayer.inventoryContainer.getSlot(slotNumber);
            ItemStack newItem = packet.func_149174_e();
            ItemStack oldItem = currentSlot.getStack();
            if (newItem == null) return;
            if (newItem.getItem() instanceof ItemTool || newItem.getItem() instanceof ItemArmor || newItem.getItem() instanceof ItemHoe) return;
            if (oldItem == null || !oldItem.getItem().equals(newItem.getItem())) {
                int newStackSize = newItem.stackSize;
                String name = StringUtils.stripControlCodes(newItem.getDisplayName());
                addDroppedItem(name, (int) Math.ceil(newStackSize * 0.98f));
            } else if (oldItem.getItem().equals(newItem.getItem())) {
                int newStackSize = newItem.stackSize;
                int oldStackSize = oldItem.stackSize;
                String name = StringUtils.stripControlCodes(newItem.getDisplayName());
                int amount = Math.max((newStackSize - oldStackSize), 0);
                addDroppedItem(name, (int) Math.ceil(amount * 0.98f));
            }
        }
    }

    private final Pattern regex = Pattern.compile("Dicer dropped (\\d+)x ([\\w\\s]+)!");

    @SubscribeEvent
    public void onReceivedChat(ClientChatReceivedEvent event) {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (event.type != 0) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("Sold")) return;
        if (message.contains(":")) return;

        Optional<String> optional = rngToCountList.stream().filter(message::contains).findFirst();
        if (optional.isPresent()) {
            String name = optional.get();
            LogUtils.sendDebug("RNG DROP. Adding " + name + " to rng drops");
            addRngDrop(name);
            return;
        }

        if (message.contains("Dicer dropped")) {
            String itemDropped;
            int amountDropped;
            Matcher matcher = regex.matcher(message);
            if (matcher.find()) {
                amountDropped = Integer.parseInt(matcher.group(1));
                if (matcher.group(2).contains("Melon")) {
                    itemDropped = "Melon";
                } else if (matcher.group(2).contains("Pumpkin")) {
                    itemDropped = "Pumpkin";
                } else {
                    return;
                }
            } else {
                return;
            }
            amountDropped *= 160;
            if (matcher.group(2).contains("Block") || matcher.group(2).contains("Polished")) {
                amountDropped *= 160;
            }
            LogUtils.sendDebug("RNG DROP. Adding " + amountDropped + " " + itemDropped + " to drops");
            addDroppedItem(itemDropped, amountDropped);
        }
    }

    private void addDroppedItem(String name, int amount) {
        if (cropsToCountList.contains(name)) {
            cropsToCount.stream().filter(crop -> crop.localizedName.equals(name)).forEach(crop -> crop.currentAmount += amount);
        }
    }

    private void addRngDrop(String name) {
        rngDropToCount.stream().filter(drop -> drop.localizedName.equals(name)).forEach(drop -> drop.currentAmount += 1);
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
        getPricesPerList(json1, cropsToCount);
        getPricesPerList(json1, visitorsMacroPrices);
    }

    private void getPricesPerList(JsonObject json1, List<BazaarItem> list) {
        for (BazaarItem item : list) {
            JsonObject json2 = json1.getAsJsonObject(item.bazaarId);
            JsonArray json3 = json2.getAsJsonArray("sell_summary");
            JsonObject json4 = json3.size() > 1 ? json3.get(1).getAsJsonObject() : json3.get(0).getAsJsonObject();

            double buyPrice = json4.get("pricePerUnit").getAsDouble();
            APICrop apiCrop;
            if (bazaarPrices.containsKey(item.localizedName)) {
                apiCrop = bazaarPrices.get(item.localizedName);
                apiCrop.previousPrices.add(apiCrop.currentPrice);
            } else {
                apiCrop = new APICrop();
            }
            apiCrop.currentPrice = buyPrice;
            bazaarPrices.put(item.localizedName, apiCrop);
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
        public int npcPrice = 0;
        public boolean dontCount = false;

        public BazaarItem(String localizedName, String bazaarId, int amountToEnchanted, int npcPrice) {
            this.localizedName = localizedName;
            this.bazaarId = bazaarId;
            this.amountToEnchanted = amountToEnchanted;
            this.npcPrice = npcPrice;
            this.currentAmount = 0;
        }

        public BazaarItem(String localizedName, String bazaarId) {
            this.localizedName = localizedName;
            this.bazaarId = bazaarId;
            this.dontCount = true;
        }

        public BazaarItem setImage() {
            this.imageURL = "/farmhelper/textures/gui/" + getImageName(localizedName);
            return this;
        }
    }

    public static class APICrop {
        public double currentPrice = 0;
        public CircularFifoQueue<Double> previousPrices = new CircularFifoQueue<>(10);
        public boolean isManipulated() {
            if (previousPrices.size() < 5) return false;
            double average = previousPrices.stream().mapToDouble(a -> a).average().orElse(currentPrice);
            return currentPrice > average * FarmHelperConfig.visitorsMacroPriceManipulationMultiplier;
        }
        public double getMedian() {
            List<Double> list = new ArrayList<>(previousPrices);
            Collections.sort(list);
            return list.get(list.size() / 2);
        }
    }
}
