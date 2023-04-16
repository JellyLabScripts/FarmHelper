package com.jelly.farmhelper.features;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.events.BlockChangeEvent;
import com.jelly.farmhelper.gui.Stat;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.network.APIHelper;
import com.jelly.farmhelper.utils.*;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.state.BasicState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ProfitCalculator {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static long realProfit = 0;
    public static long blocksBroken = 0;

    public static final BasicState<String> profit = new BasicState<>("$0");
    public static final BasicState<String> profitHr = new BasicState<>("$0");
    public static final BasicState<String> runtime = new BasicState<>("0h 0m 0s");
    public static final BasicState<String> blocksPerSecond = new BasicState<>("0 BPS");

    private List<ItemStack> previousInventory;
    public static Multimap<String, DroppedItem> itemsDropped = ArrayListMultimap.create();
    private final Pattern pattern = Pattern.compile(" x([0-9]+)");

    public static final HashMap<String, GuiItem> dropToShow = new HashMap<>();

    private static final String path = "/assets/farmhelper/textures/gui/";

    public static final List<BazaarItem> cropsToCount = new ArrayList<BazaarItem>() {{
        add(new BazaarItem("Hay Bale", "ENCHANTED_HAY_BLOCK", HAY_ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Seeds", "ENCHANTED_SEEDS", ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Carrot", "ENCHANTED_CARROT", ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Potato", "ENCHANTED_POTATO", ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Melon", "ENCHANTED_MELON_BLOCK", ENCHANTED_TIER_2).setImage());
        add(new BazaarItem("Pumpkin", "ENCHANTED_PUMPKIN", ENCHANTED_TIER_2).setImage());
        add(new BazaarItem("Sugar Cane", "ENCHANTED_SUGAR_CANE", ENCHANTED_TIER_2).setImage());
        add(new BazaarItem("Cocoa Beans", "ENCHANTED_COCOA", ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Nether Wart", "MUTANT_NETHER_STALK", ENCHANTED_TIER_2).setImage());
        add(new BazaarItem("Cactus Green", "ENCHANTED_CACTUS", ENCHANTED_TIER_2).setImage());
        add(new BazaarItem("Red Mushroom", "ENCHANTED_RED_MUSHROOM", ENCHANTED_TIER_1).setImage());
        add(new BazaarItem("Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM", ENCHANTED_TIER_1).setImage());
    }};

    public static final List<BazaarItem> armorDropToCount = new ArrayList<BazaarItem>() {{
        add(new BazaarItem("Cropie", "CROPIE", 1).setImage());
        add(new BazaarItem("Squash", "SQUASH", 1).setImage());
        add(new BazaarItem("Fermento", "FERMENTO", 1).setImage());
    }};

    public static HashMap<String, Double> bazaarPrices = new HashMap<>();

    public static final int ENCHANTED_TIER_1 = 160;
    public static final int ENCHANTED_TIER_2 = 25600;
    public static final int HAY_ENCHANTED_TIER_1 = 144; // enchanted hay bale

    private final Clock updateClock = new Clock();
    private static final Clock updateBazaarClock = new Clock();

    // SECOND WAY TO COUNT THE ITEMS, LESS ACCURATE

//    @SubscribeEvent
//    public void onTick(TickEvent event) {
//        if (!MacroHandler.isMacroing) return;
//        if (mc.thePlayer == null || mc.theWorld == null) return;
//
//        List<ItemStack> newInventory = PlayerUtils.copyInventory(mc.thePlayer.inventory.mainInventory);
//        Map<String, Integer> previousInventoryMap = new HashMap<>();
//        Map<String, Integer> newInventoryMap = new HashMap<>();
//
//        if (previousInventory != null) {
//
//            for(int i = 0; i < newInventory.size(); i++) {
//                if (i == 8) {
//                    continue;
//                }
//
//                ItemStack previousItem = null;
//                ItemStack newItem = null;
//
//                try {
//                    previousItem = previousInventory.get(i);
//                    newItem = newInventory.get(i);
//
//                    if(previousItem != null) {
//                        int amount;
//                        if (previousInventoryMap.containsKey(previousItem.getDisplayName())) {
//                            amount = previousInventoryMap.get(previousItem.getDisplayName()) + previousItem.stackSize;
//                        } else {
//                            amount = previousItem.stackSize;
//                        }
//                        previousInventoryMap.put(previousItem.getDisplayName(), amount);
//                    }
//
//                    if(newItem != null) {
//                        if (pattern.matcher(newItem.getDisplayName()).matches()) {
//                            String newName = newItem.getDisplayName().substring(0, newItem.getDisplayName().lastIndexOf(" "));
//                            newItem.setStackDisplayName(newName);
//                        }
//                        int amount;
//                        if (newInventoryMap.containsKey(newItem.getDisplayName())) {
//                            amount = newInventoryMap.get(newItem.getDisplayName()) + newItem.stackSize;
//                        }  else {
//                            amount = newItem.stackSize;
//                        }
//                        newInventoryMap.put(newItem.getDisplayName(), amount);
//                    }
//                } catch (RuntimeException exception) {
//                    CrashReport crashReport = CrashReport.makeCrashReport(exception, "Comparing current inventory to previous inventory");
//                    CrashReportCategory inventoryDetails = crashReport.makeCategory("Inventory Details");
//                    inventoryDetails.addCrashSection("Previous", "Size: " + previousInventory.size());
//                    inventoryDetails.addCrashSection("New", "Size: " + newInventory.size());
//                    CrashReportCategory itemDetails = crashReport.makeCategory("Item Details");
//                    itemDetails.addCrashSection("Previous Item", "Item: " + (previousItem != null ? previousItem.toString() : "null") + "\n"
//                            + "Display Name: " + (previousItem != null ? previousItem.getDisplayName() : "null") + "\n"
//                            + "Index: " + i + "\n"
//                            + "Map Value: " + (previousItem != null ? (previousInventoryMap.get(previousItem.getDisplayName()) != null ? previousInventoryMap.get(previousItem.getDisplayName()).toString() : "null") : "null"));
//                    itemDetails.addCrashSection("New Item", "Item: " + (newItem != null ? newItem.toString() : "null") + "\n"
//                            + "Display Name: " + (newItem != null ? newItem.getDisplayName() : "null") + "\n"
//                            + "Index: " + i + "\n"
//                            + "Map Value: " + (newItem != null ? (previousInventoryMap.get(newItem.getDisplayName()) != null ? previousInventoryMap.get(newItem.getDisplayName()).toString() : "null") : "null"));
//                    throw new ReportedException(crashReport);
//                }
//            }
//
//            List<DroppedItem> inventoryDifference = new LinkedList<>();
//            Set<String> keySet = new HashSet<>(previousInventoryMap.keySet());
//            keySet.addAll(newInventoryMap.keySet());
//
//            keySet.forEach(key -> {
//                int previousAmount = 0;
//                if (previousInventoryMap.containsKey(key)) {
//                    previousAmount = previousInventoryMap.get(key);
//                }
//
//                int newAmount = 0;
//                if (newInventoryMap.containsKey(key)) {
//                    newAmount = newInventoryMap.get(key);
//                }
//
//                int diff = newAmount - previousAmount;
//                if (diff != 0) {
//                    inventoryDifference.add(new DroppedItem(key, diff));
//                }
//            });
//
//            for (DroppedItem diff : inventoryDifference) {
//                Collection<DroppedItem> itemDiffs = itemsDropped.get(diff.name);
//                if (itemDiffs.size() == 0) {
//                    itemsDropped.put(diff.name, diff);
//
//                } else {
//                    boolean added = false;
//                    for (DroppedItem loopDiff : itemDiffs) {
//                        if ((diff.amount < 0 && loopDiff.amount < 0) || (diff.amount > 0 && loopDiff.amount > 0)) {
//                            loopDiff.add(diff.amount);
//                            added = true;
//                        }
//                    }
//                    if (!added) {
//                        if (diff.amount < 0) continue;
//                        itemsDropped.put(diff.name, diff);
//                    }
//                }
//            }
//        }
//
//        previousInventory = newInventory;
//    }

    @SubscribeEvent
    public void onTickUpdateProfit(TickEvent.ClientTickEvent event) {
        if (!MacroHandler.isMacroing) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (updateClock.passed()) {
            updateClock.reset();
            updateClock.schedule(100);

            long totalProfit = 0;

            for (DroppedItem item : itemsDropped.values()) {
                if (item.amount < 0) continue;
                if (bazaarPrices.containsKey(StringUtils.stripControlCodes(item.name))) {
                    Optional<BazaarItem> isCrop = cropsToCount.stream().filter(crop -> StringUtils.stripControlCodes(item.name).equalsIgnoreCase(crop.localizedName)).findFirst();
                    if (isCrop.isPresent()) {
                        BazaarItem crop = isCrop.get();
                        double price = bazaarPrices.get(crop.localizedName);
                        totalProfit += price * (item.amount * 1.0f / crop.amountToEnchanted);
                        dropToShow.put(item.name, new GuiItem((int) Math.floor(item.amount * 1.0F / crop.amountToEnchanted), crop.image));
                    } else {
                        Optional<BazaarItem> isArmor = armorDropToCount.stream().filter(armor -> StringUtils.stripControlCodes(item.name).equalsIgnoreCase(armor.localizedName)).findFirst();
                        if (isArmor.isPresent()) {
                            double price = bazaarPrices.get(isArmor.get().localizedName);
                            totalProfit += price * item.amount * 1.0f;
                            dropToShow.put(item.name, new GuiItem(item.amount, isArmor.get().image));
                        }
                    }
                }
            }

            realProfit = totalProfit;
            profit.set("$" + Utils.formatNumber(Math.round(totalProfit * 0.95)));
            profitHr.set("$" + Utils.formatNumber(Math.round(getHourProfit(totalProfit * 0.95))));
            runtime.set(Utils.formatTime(System.currentTimeMillis() - MacroHandler.startTime));
            blocksPerSecond.set(Math.round((float) blocksBroken / (System.currentTimeMillis() - MacroHandler.startTime) * 10000f) / 10f + " BPS");
        }
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (FarmConfig.cropType == CropEnum.CACTUS && event.old.getBlock() == Blocks.cactus && event.update.getBlock() != Blocks.cactus) {
            blocksBroken++;
        }
    }

    @SubscribeEvent
    public void onTickUpdateBazaarPrices(TickEvent.ClientTickEvent event) {
        if (updateBazaarClock.passed()) {
            updateBazaarClock.reset();
            updateBazaarClock.schedule(1000 * 60 * 5);
            new Thread(ProfitCalculator::fetchBazaarPrices).start();
        }
    }

    public static void onInventoryChanged(ItemStack item, int size) {
        if (itemsDropped.containsKey(StringUtils.stripControlCodes(item.getDisplayName()))) {
            Collection<DroppedItem> droppedItem = itemsDropped.get(StringUtils.stripControlCodes(item.getDisplayName()));
            if (droppedItem.size() == 0) {
                itemsDropped.put(StringUtils.stripControlCodes(item.getDisplayName()), new DroppedItem(StringUtils.stripControlCodes(item.getDisplayName()), size));
            } else {
                boolean added = false;
                for (DroppedItem loopDiff : droppedItem) {
                    if ((size < 0 && loopDiff.amount < 0) || (size > 0 && loopDiff.amount > 0)) {
                        loopDiff.add(size);
                        added = true;
                    }
                }
                if (!added) {
                    if (size < 0) return;
                    itemsDropped.put(StringUtils.stripControlCodes(item.getDisplayName()), new DroppedItem(StringUtils.stripControlCodes(item.getDisplayName()), size));
                }
            }
        } else {
            itemsDropped.put(StringUtils.stripControlCodes(item.getDisplayName()), new DroppedItem(StringUtils.stripControlCodes(item.getDisplayName()), size));
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
            default:
                throw new IllegalArgumentException("No image for " + name);
        }
    }

    public static double getHourProfit(double total) {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime) > 0) {
            return 3600f * total / TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - MacroHandler.startTime);
        }
        return 0;
    }

    public static void resetProfit() {
        blocksBroken = 0;
        itemsDropped.clear();
        dropToShow.clear();
    }

    private static class DroppedItem {
        public String name;
        public int amount;

        public DroppedItem(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public void add(int amount) {
            this.amount += amount;
        }
    }

    public static void fetchBazaarPrices() {
        updateBazaarClock.reset();
        try {
            JSONObject json = APIHelper.readJsonFromUrl("https://api.hypixel.net/skyblock/bazaar","User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
            JSONObject json1 = (JSONObject) json.get("products");

            for (int i = 0; i < cropsToCount.size(); i++) {
                JSONObject json2 = (JSONObject)json1.get(cropsToCount.get(i).bazaarId);
                JSONObject json3 = (JSONObject)json2.get("quick_status");
                bazaarPrices.put(cropsToCount.get(i).localizedName, (Double) (json3).get("sellPrice"));
            }
            for (int i = 0; i < armorDropToCount.size(); i++) {
                JSONObject json2 = (JSONObject)json1.get(armorDropToCount.get(i).bazaarId);
                JSONObject json3 = (JSONObject)json2.get("quick_status");
                bazaarPrices.put(armorDropToCount.get(i).localizedName, (Double) (json3).get("sellPrice"));
            }
            if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().theWorld != null)
                LogUtils.debugLog("Bazaar prices updated");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static class BazaarItem {
        public String localizedName;
        public String bazaarId;
        public int amountToEnchanted;

        public UIComponent image;

        public BazaarItem(String localizedName, String bazaarId, int amountToEnchanted) {
            this.localizedName = localizedName;
            this.bazaarId = bazaarId;
            this.amountToEnchanted = amountToEnchanted;
        }

        public BazaarItem setImage() {
            this.image = UIImage.ofResource(path + getImageName(localizedName));
            return this;
        }
    }

    public static class GuiItem {
        public int enchantedAmount;
        public UIComponent img;
        private final BasicState<String> enchantedAmountState = new BasicState<>("0");
        public UIComponent stat;

        public GuiItem(int enchantedAmount, UIComponent img) {
            this.enchantedAmount = enchantedAmount;
            this.img = img;
            this.stat = new Stat(img).bind(getEnchantedAmount());
        }

        public BasicState<String> getEnchantedAmount() {
            enchantedAmountState.set(enchantedAmount + "");
            return enchantedAmountState;
        }
    }
}
