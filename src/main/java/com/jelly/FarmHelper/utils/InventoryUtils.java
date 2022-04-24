package com.jelly.FarmHelper.utils;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryUtils {
    private static Minecraft mc = Minecraft.getMinecraft();

    public static void sellInventory() {
        try {
            LogUtils.debugLog("Selling Inventory");
            // Sell to NPC
            PlayerUtils.openTrades();
            Thread.sleep(500);
            for (int j = 0; j < 36; j++) {
                ItemStack sellStack = mc.thePlayer.inventory.getStackInSlot(j);
                if (sellStack != null) {
                    String name = sellStack.getDisplayName();
                    if ((name.contains("Brown Mushroom") || name.contains("Enchanted Brown Mushroom") || name.contains("Brown Mushroom Block") || name.contains("Brown Enchanted Mushroom Block") ||
                        name.contains("Red Mushroom") || name.contains("Enchanted Red Mushroom") || name.contains("Red Mushroom Block") || name.contains("Red Enchanted Mushroom Block") ||
                        name.contains("Nether Wart") || name.contains("Enchanted Nether Wart") || name.contains("Mutant Nether Wart") ||
                        name.contains("Sugar Cane") || name.contains("Enchanted Sugar") || name.contains("Enchanted Sugar Cane") ||
                        name.contains("Stone")) && !name.contains("Hoe")
                    ) {
                        LogUtils.debugLog("Found stack, selling");
                        InventoryUtils.clickWindow(mc.thePlayer.openContainer.windowId, (j < 9 ? j + 45 + 36 : j + 45));
                        Thread.sleep(200);
                    }
                }
                Thread.sleep(20);
            }
            mc.thePlayer.closeScreen();

            // Sell to Bazaar
            for (int j = 0; j < 36; j++) {
                ItemStack sellStack = mc.thePlayer.inventory.getStackInSlot(j);
                if (sellStack != null) {
                    String name = sellStack.getDisplayName();
                    if (name.contains("Carrot") && !name.contains("Hoe")) {
                        LogUtils.debugLog("Found carrots, selling");
                        PlayerUtils.openBazaar();
                        waitForItemClick(12, "Carrot", 0, "Farming");
                        waitForItemClick(29, "Sell Inventory Now", 12, "Carrot", "Enchanted");
                        waitForItemClick(11, "Selling whole inventory", 29, "Sell Inventory Now");
                        waitForItemClick(11, "Items sold!", 11, "Selling whole inventory");
                        LogUtils.debugLog("Successfully sold all carrots");
                    }
                    if (name.contains("Potato") && !name.contains("Hoe")) {
                        LogUtils.debugLog("Found potatoes, selling");
                        PlayerUtils.openBazaar();
                        waitForItemClick(13, "Potato", 0, "Farming");
                        waitForItemClick(29, "Sell Inventory Now", 13, "Potato", "Enchanted");
                        waitForItemClick(11, "Selling whole inventory", 29, "Sell Inventory Now");
                        waitForItemClick(11, "Items sold!", 11, "Selling whole inventory");
                        LogUtils.debugLog("Successfully sold all potatoes");
                    }
                    if ((name.contains("Wheat") || name.contains("Hay Bale") || name.contains("Bread")) && !name.contains("Hoe")) {
                        LogUtils.debugLog("Found wheat, selling");
                        PlayerUtils.openBazaar();
                        waitForItemClick(11, "Wheat & Seeds", 0, "Farming");
                        waitForItemClick(29, "Sell Inventory Now", 11, "Wheat & Seeds");
                        waitForItemClick(11, "Selling whole inventory", 29, "Sell Inventory Now");
                        waitForItemClick(11, "Items sold!", 11, "Selling whole inventory");
                        LogUtils.debugLog("Successfully sold all wheat");
                    }
                }
                Thread.sleep(20);
            }
            mc.thePlayer.closeScreen();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int countSack(int slotID) {
        ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
        NBTTagList list = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
        Pattern pattern = Pattern.compile("^([a-zA-Z]+): ([0-9]+)(.*)");
        for (int j = 0; j < list.tagCount(); j++) {
            Matcher matcher = pattern.matcher(StringUtils.stripControlCodes(list.getStringTagAt(j)));
            if (matcher.matches()) {
                LogUtils.debugLog("Stored: " + matcher.group(2));
                return Integer.parseInt(matcher.group(2));
            }
        }
        return 0;
    }

    public static void clickWindow(int windowID, int slotID, int button, int clickType) {
        mc.playerController.windowClick(windowID, slotID, clickType, clickType, mc.thePlayer);
    }
    public static void clickWindow(int windowID, int slotID, int button) {
        clickWindow(windowID, slotID, button, 0);
    }
    public static void clickWindow(int windowID, int slotID) {
        clickWindow(windowID, slotID, 0);
    }


    public static int findItemInventory(String name) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                System.out.println("findItemInventory: " + stack.getDisplayName());
                if (stack.getDisplayName().contains(name)) {
                    return i + (i < 9 ? 36 : 0);
                }
            }
        }
        return -1;
    }

    public static void waitForItemClick(int slotID, String displayName, int clickSlotID, String clickDisplayName, String exclude) {
        try {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
            ItemStack clickStack;
            while (stack == null || (!stack.getDisplayName().contains(displayName))) {
                clickStack = mc.thePlayer.openContainer.getSlot(clickSlotID).getStack();
                stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
                if (clickStack != null && (clickStack.getDisplayName().contains(clickDisplayName) && !clickStack.getDisplayName().contains(exclude))) {
                    clickWindow(mc.thePlayer.openContainer.windowId, clickSlotID);
                }
                Thread.sleep(100);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void waitForItemClick(int slotID, String displayName, int clickSlotID, Item item) {
        try {
            ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
            ItemStack clickStack;
            while (stack == null || !stack.getDisplayName().contains(displayName)) {
                clickStack = mc.thePlayer.openContainer.getSlot(clickSlotID).getStack();
                stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
                if (checkItem(clickStack, item)) {
                    clickWindow(mc.thePlayer.openContainer.windowId, clickSlotID);
                }
                Thread.sleep(100);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void waitForItemClick(int slotID, String displayName, int clickSlotID, String clickDisplayName) {
        // Runs with exclude with something that wont ever be in a name, idk if you can put null
        waitForItemClick(slotID, displayName, clickSlotID, clickDisplayName, "1234567890");
    }

    public static void waitForItem(int slotID, String displayName) {
        waitForItemClick(slotID, displayName, 999, "");
    }

    public static boolean checkItem(ItemStack stack, Item item) {
        return stack != null && stack.getItem() == item;
    }

    public static boolean checkItem(ItemStack stack, Block item) {
        return stack != null && stack.getItem() == Item.getItemFromBlock(item);
    }

    public static boolean checkItem(int slotID, Block item) {
        ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
        return stack != null && stack.getItem() == Item.getItemFromBlock(item);
    }

    public static boolean checkItem(int slotID, Item item) {
        ItemStack stack = mc.thePlayer.openContainer.getSlot(slotID).getStack();
        return stack != null && stack.getItem() == item;
    }

    public static int getCounter() {
        final ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack != null && stack.hasTagCompound()) {
            final NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("ExtraAttributes", 10)) {
                final NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
                if (ea.hasKey("mined_crops", 99)) {
                    return ea.getInteger("mined_crops");
                } else if (ea.hasKey("farmed_cultivating", 99)) {
                    return ea.getInteger("farmed_cultivating");
                }
            }
        }
        LogUtils.debugLog("Error: Cannot find counter on held item");
        return 0;
    }
}
