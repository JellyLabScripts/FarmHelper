package com.jelly.farmhelper.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jelly.farmhelper.datastructures.ItemDiff;
import com.jelly.farmhelper.datastructures.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.*;

public class InventoryUtils {
    /*
     *  @Author Mostly Apfelsaft
     */
    private static Minecraft mc = Minecraft.getMinecraft();
    // private static List<ItemStack> previousInventory;
    // public static Multimap<String, ItemDiff> itemPickupLog = ArrayListMultimap.create();

    public static String getInventoryName() {
        if (InventoryUtils.mc.currentScreen instanceof GuiChest) {
            final ContainerChest chest = (ContainerChest)InventoryUtils.mc.thePlayer.openContainer;
            final IInventory inv = chest.getLowerChestInventory();
            return inv.hasCustomName() ? inv.getName() : null;
        }
        return null;
    }

    public static ItemStack getStackInSlot(final int slot) {
        return InventoryUtils.mc.thePlayer.inventory.getStackInSlot(slot);
    }

    public static ItemStack getStackInOpenContainerSlot(final int slot) {
        if (InventoryUtils.mc.thePlayer.openContainer.inventorySlots.get(slot).getHasStack()) {
            return InventoryUtils.mc.thePlayer.openContainer.inventorySlots.get(slot).getStack();
        }
        return null;
    }

    public static int getSlotForItem(final String itemName) {
        for (final Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                final ItemStack is = slot.getStack();
                if (is.getDisplayName().contains(itemName)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static void clickOpenContainerSlot(final int slot, final int clickType) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, 0, clickType, mc.thePlayer);
    }

    public static void clickOpenContainerSlot(final int slot) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, 0, 0, mc.thePlayer);
    }

    public static int getAvailableHotbarSlot(final String name) {
        for (int i = 0; i < 8; ++i) {
            final ItemStack is = mc.thePlayer.inventory.getStackInSlot(i);
            if (is == null || is.getDisplayName().contains(name)) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> getAllSlots(final String name) {
        final List<Integer> ret = new ArrayList<>();
        for (int i = 9; i < 44; ++i) {
            final ItemStack is = mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack();
            if (is != null && is.getDisplayName().contains(name)) {
                ret.add(i);
            }
        }
        return ret;
    }

    public static int getAmountInHotbar(final String item) {
        for (int i = 0; i < 8; ++i) {
            final ItemStack is = InventoryUtils.mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && StringUtils.stripControlCodes(is.getDisplayName()).equals(item)) {
                return is.stackSize;
            }
        }
        return 0;
    }

    public static int getItemInHotbar(final String itemName) {
        for (int i = 0; i < 8; ++i) {
            final ItemStack is = InventoryUtils.mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && StringUtils.stripControlCodes(is.getDisplayName()).contains(itemName)) {
                return i;
            }
        }
        return -1;
    }

    public static List<ItemStack> getInventoryStacks() {
        final List<ItemStack> ret = new ArrayList<ItemStack>();
        for (int i = 9; i < 44; ++i) {
            final Slot slot = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null) {
                final ItemStack stack = slot.getStack();
                if (stack != null) {
                    ret.add(stack);
                }
            }
        }
        return ret;
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
        LogUtils.debugFullLog("Error: Cannot find counter on held item");
        return 0;
    }

    public static NBTTagCompound getExtraAttributes(ItemStack item) {
        if (item == null) {
            throw new NullPointerException("The item cannot be null!");
        }
        if (!item.hasTagCompound()) {
            return null;
        }

        return item.getSubCompound("ExtraAttributes", false);
    }

    private static List<ItemStack> copyInventory(ItemStack[] inventory) {
        List<ItemStack> copy = new ArrayList<>(inventory.length);
        for (ItemStack item : inventory) {
            if (item != null) {
                copy.add(ItemStack.copyItemStack(item));
            } else {
                copy.add(null);
            }
        }
        return copy;
    }

//    public static void getInventoryDifference(ItemStack[] currentInventory) {
//        List<ItemStack> newInventory = copyInventory(currentInventory);
//        Map<String, Pair<Integer, NBTTagCompound>> previousInventoryMap = new HashMap<>();
//        Map<String, Pair<Integer, NBTTagCompound>> newInventoryMap = new HashMap<>();
//
//        if (previousInventory != null) {
//            for (int i = 0; i < newInventory.size(); i++) {
//                if (i == 8) { // Skip the SkyBlock Menu slot altogether (which includes the Quiver Arrow now)
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
//                    if (previousItem != null) {
//                        int amount;
//                        if (previousInventoryMap.containsKey(previousItem.getDisplayName())) {
//                            amount = previousInventoryMap.get(previousItem.getDisplayName()).getKey() + previousItem.stackSize;
//                        } else {
//                            amount = previousItem.stackSize;
//                        }
//                        NBTTagCompound extraAttributes = getExtraAttributes(previousItem);
//                        if (extraAttributes != null) {
//                            extraAttributes = (NBTTagCompound) extraAttributes.copy();
//                        }
//                        previousInventoryMap.put(previousItem.getDisplayName(), new Pair<>(amount, extraAttributes));
//                    }
//
//                    if (newItem != null) {
//                        if (newItem.getDisplayName().contains(" " + EnumChatFormatting.DARK_GRAY + "x")) {
//                            String newName = newItem.getDisplayName().substring(0, newItem.getDisplayName().lastIndexOf(" "));
//                            newItem.setStackDisplayName(newName); // This is a workaround for merchants, it adds x64 or whatever to the end of the name.
//                        }
//                        int amount;
//                        if (newInventoryMap.containsKey(newItem.getDisplayName())) {
//                            amount = newInventoryMap.get(newItem.getDisplayName()).getKey() + newItem.stackSize;
//                        } else {
//                            amount = newItem.stackSize;
//                        }
//                        NBTTagCompound extraAttributes = getExtraAttributes(newItem);
//                        if (extraAttributes != null) {
//                            extraAttributes = (NBTTagCompound) extraAttributes.copy();
//                        }
//                        newInventoryMap.put(newItem.getDisplayName(), new Pair<>(amount, extraAttributes));
//                    }
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }
//
//            List<ItemDiff> inventoryDifference = new LinkedList<>();
//            Set<String> keySet = new HashSet<>(previousInventoryMap.keySet());
//            keySet.addAll(newInventoryMap.keySet());
//
//            keySet.forEach(key -> {
//                int previousAmount = 0;
//                if (previousInventoryMap.containsKey(key)) {
//                    previousAmount = previousInventoryMap.get(key).getKey();
//                }
//
//                int newAmount = 0;
//                if (newInventoryMap.containsKey(key)) {
//                    newAmount = newInventoryMap.get(key).getKey();
//                }
//
//                int diff = newAmount - previousAmount;
//                if (diff != 0) { // Get the NBT tag from whichever map the name exists in
//                    inventoryDifference.add(new ItemDiff(key, diff, newInventoryMap.getOrDefault(key, previousInventoryMap.get(key)).getValue()));
//                }
//            });
//
//            // Add changes to already logged changes of the same item, so it will increase/decrease the amount
//            // instead of displaying the same item twice
//            if (true) {
//                for (ItemDiff diff : inventoryDifference) {
//                    Collection<ItemDiff> itemDiffs = itemPickupLog.get(diff.getDisplayName());
//                    if (itemDiffs.size() <= 0) {
//                        itemPickupLog.put(diff.getDisplayName(), diff);
//
//                    } else {
//                        boolean added = false;
//                        for (ItemDiff loopDiff : itemDiffs) {
//                            if ((diff.getAmount() < 0 && loopDiff.getAmount() < 0) || (diff.getAmount() > 0 && loopDiff.getAmount() > 0)) {
//                                loopDiff.add(diff.getAmount());
//                                added = true;
//                            }
//                        }
//                        if (!added) {
//                            itemPickupLog.put(diff.getDisplayName(), diff);
//                        }
//                    }
//                }
//            }
//        }
//
//        previousInventory = newInventory;
//    }

    public static int getSCHoeSlot() {
        for (int i = 36; i < 44; i++) {
            if (Minecraft.getMinecraft().thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {
                if (Minecraft.getMinecraft().thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Turing")) {
                    return i - 36;
                }
            }
        }
        return 0;
    }

    /**
     * Resets the previously stored Inventory state
     */
//    public static void resetPreviousInventory() {
//        previousInventory = null;
//        itemPickupLog = ArrayListMultimap.create();
//    }
}
