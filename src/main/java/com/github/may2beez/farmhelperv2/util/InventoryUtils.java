package com.github.may2beez.farmhelperv2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class InventoryUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static int getSlotOfItemInContainer(String item) {
        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static int getSlotOfItemInHotbar(String item) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.stripControlCodes(slot.getDisplayName());
                if (itemName.contains(item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int getSlotOfItemInInventory(String item) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return slot.slotNumber;
                }
            }
        }
        return -1;
    }

    public static String getInventoryName() {
        if (mc.currentScreen instanceof GuiChest) {
            final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            final IInventory inv = chest.getLowerChestInventory();
            return inv.hasCustomName() ? inv.getName() : null;
        }
        return null;
    }

    public static boolean hasItemInInventory(String item) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.getHasStack()) {
                String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                if (itemName.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasItemInHotbar(String item) {
        for (int i = 0; i < 9; i++) {
            ItemStack slot = mc.thePlayer.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                String itemName = StringUtils.stripControlCodes(slot.getDisplayName());
                if (itemName.contains(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromInventory(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static ArrayList<Slot> getIndexesOfItemsFromContainer(Predicate<Slot> predicate) {
        ArrayList<Slot> indexes = new ArrayList<>();
        for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size(); i++) {
            Slot slot = mc.thePlayer.openContainer.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                if (predicate.test(slot)) {
                    indexes.add(slot);
                }
            }
        }
        return indexes;
    }

    public static enum ClickType {
        LEFT,
        RIGHT
    }
    public static enum ClickMode {
        PICKUP,
        QUICK_MOVE,
        SWAP
    }

    public static void clickSlotWithId(int id, ClickType mouseButton, ClickMode mode, int windowId) {
        mc.playerController.windowClick(windowId, id, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void clickContainerSlot(int slot, ClickType mouseButton, ClickMode mode) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void clickSlot(int slot, ClickType mouseButton, ClickMode mode) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, mouseButton.ordinal(), mode.ordinal(), mc.thePlayer);
    }

    public static void swapSlots(int slot, int hotbarSlot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hotbarSlot, 2, mc.thePlayer);
    }

    public static void openInventory() {
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
    }

    public static Slot getSlotOfId(int id) {
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (slot.slotNumber == id) {
                return slot;
            }
        }
        return null;
    }

    public static ArrayList<String> getItemLore(ItemStack itemStack) {
        NBTTagList loreTag = itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
        ArrayList<String> loreList = new ArrayList<>();
        for (int i = 0; i < loreTag.tagCount(); i++) {
            loreList.add(StringUtils.stripControlCodes(loreTag.getStringTagAt(i)));
        }
        return loreList;
    }

    public static List<String> getLoreOfItemInContainer(int slot) {
        if (slot == -1) return new ArrayList<>();
        ItemStack itemStack = mc.thePlayer.openContainer.getSlot(slot).getStack();
        if (itemStack == null) return new ArrayList<>();
        return getItemLore(itemStack);
    }

    public static int getRancherBootSpeed() {
        final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(8).getStack();
        int speed = -1;
        if (stack != null && stack.hasTagCompound()) {
            final NBTTagCompound tag = stack.getTagCompound();
            final Pattern pattern = Pattern.compile("(Current Speed Cap: §a\\d+)", Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(tag.toString());
            while (matcher.find()) {
                if (matcher.group(0) != null) {
                    speed = parseInt((matcher.group(0).replaceAll("Current Speed Cap: §a", "")));
                }
            }
        }
        return speed;
    }
}
