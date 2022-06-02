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

    public static void clickOpenContainerSlot(final int slot, final int button, final int clickType) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, button, clickType, mc.thePlayer);
    }

    public static void clickOpenContainerSlot(final int slot, final int button) {
        clickOpenContainerSlot(slot, button, 0);
    }

    public static void clickOpenContainerSlot(final int slot) {
        clickOpenContainerSlot(slot, 0, 0);
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

    public static List<Slot> getInventorySlots() {
        final List<Slot> ret = new ArrayList<>();
        for (int i = 9; i < 44; ++i) {
            final Slot slot = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null) {
                final ItemStack stack = slot.getStack();
                if (stack != null) {
                    ret.add(slot);
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
}
