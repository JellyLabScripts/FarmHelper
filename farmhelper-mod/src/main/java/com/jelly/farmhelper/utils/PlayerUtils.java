package com.jelly.farmhelper.utils;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.VerticalMacroEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.config.Config.CropEnum
    ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class PlayerUtils {

    /*
     *  @Author partly Apfelsaft
     */
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Clock clock = new Clock();

    public static void setSpawn() {
        mc.thePlayer.sendChatMessage("/setspawn");
    }

    public static void attemptSetSpawn() {
        if(FarmHelper.config.enableAutoSetSpawn && FarmHelper.config.ladderDesign) {
            double diff = (FarmHelper.config.autoSetSpawnMaxDelay * 1000) - (FarmHelper.config.autoSetSpawnMinDelay * 1000);
            if (diff <= 0) {
                LogUtils.scriptLog("autoSetSpawnMaxDelay must be greater than autoSetSpawnMinDelay", EnumChatFormatting.RED);
                return;
            }
            System.out.println(clock.isScheduled());
            if (clock.isScheduled() && clock.passed()) {
                mc.thePlayer.sendChatMessage("/setspawn");
                long time = (long) (new Random().nextInt((int) (diff)) + (FarmHelper.config.autoSetSpawnMinDelay * 1000L));
                System.out.println("time: " + time);
                clock.schedule(time);
            } else if (!clock.isScheduled()) {
                long time = (long) (new Random().nextInt((int) (diff)) + (FarmHelper.config.autoSetSpawnMinDelay * 1000L));
                System.out.println("time: " + time);
                clock.schedule(time);
            }
        }
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
                    speed = parseInt((matcher.group(0).replaceAll("Current Speed Cap: §a" ,"")));
                }
            }
        }
        return speed;
    }
    public static String getInventoryName() {
        if (PlayerUtils.mc.currentScreen instanceof GuiChest) {
            final ContainerChest chest = (ContainerChest) PlayerUtils.mc.thePlayer.openContainer;
            final IInventory inv = chest.getLowerChestInventory();
            return inv.hasCustomName() ? inv.getName() : null;
        }
        return null;
    }

    public static boolean inventoryNameStartsWith(String startsWithString) {
        return PlayerUtils.getInventoryName() != null && PlayerUtils.getInventoryName().startsWith(startsWithString);
    }

    public static boolean inventoryNameContains(String startsWithString) {
        return PlayerUtils.getInventoryName() != null && PlayerUtils.getInventoryName().contains(startsWithString);
    }

    public static void openInventory() {
            mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
    }
    public static ItemStack getStackInSlot(final int slot) {
        return PlayerUtils.mc.thePlayer.inventory.getStackInSlot(slot);
    }

    public static ItemStack getStackInOpenContainerSlot(final int slot) {
        if (PlayerUtils.mc.thePlayer.openContainer.inventorySlots.get(slot).getHasStack()) {
            return PlayerUtils.mc.thePlayer.openContainer.inventorySlots.get(slot).getStack();
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
            final ItemStack is = PlayerUtils.mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && StringUtils.stripControlCodes(is.getDisplayName()).equals(item)) {
                return is.stackSize;
            }
        }
        return 0;
    }

    public static int getItemInHotbar(final String itemName) {
        for (int i = 0; i < 8; ++i) {
            final ItemStack is = PlayerUtils.mc.thePlayer.inventory.getStackInSlot(i);
            if (is != null && StringUtils.stripControlCodes(is.getDisplayName()).contains(itemName)) {
                return i;
            }
        }
        return -1;
    }

    public static List<ItemStack> getInventoryStacks() {
        final List<ItemStack> ret = new ArrayList<ItemStack>();
        for (int i = 9; i < 44; ++i) {
            final Slot slot = PlayerUtils.mc.thePlayer.inventoryContainer.getSlot(i);
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
            final Slot slot = PlayerUtils.mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot != null) {
                final ItemStack stack = slot.getStack();
                if (stack != null) {
                    ret.add(slot);
                }
            }
        }
        return ret;
    }

    public static long getCounter() {
        if (mc.thePlayer != null && mc.theWorld != null) {
            final ItemStack stack = mc.thePlayer.getHeldItem();
            if (stack != null && stack.hasTagCompound()) {
                final NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("ExtraAttributes", 10)) {
                    final NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");
                    if (ea.hasKey("mined_crops", 99)) {
                        return ea.getLong("mined_crops");
                    } else if (ea.hasKey("farmed_cultivating", 99)) {
                        return ea.getLong("farmed_cultivating");
                    }
                }
            }
            LogUtils.debugFullLog("Error: Cannot find counter on held item");
        }
        return 0L;
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

    public static NBTTagList getLore(ItemStack item) {
        if (item == null) {
            throw new NullPointerException("The item cannot be null!");
        }
        if (!item.hasTagCompound()) {
            return null;
        }

        return item.getSubCompound("display", false).getTagList("Lore", 8);
    }

    public static List<ItemStack> copyInventory(ItemStack[] inventory) {
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

    public static int getHoeSlot(CropEnum crop) {
        for (int i = 36; i < 44; i++) {
            if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {
                switch (crop){
                    case NETHER_WART:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Newton")) {
                            return i - 36;
                        }
                        continue;
                    case CARROT:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Gauss")) {
                            return i - 36;
                        }
                        continue;
                    case WHEAT:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Euclid")) {
                            return i - 36;
                        }
                        continue;
                    case POTATO:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Pythagorean")) {
                            return i - 36;
                        }
                        continue;
                    case SUGAR_CANE:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Turing")) {
                            return i - 36;
                        }
                        continue;
                    case CACTUS:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Knife")) {
                            return i - 36;
                        }
                        continue;
                    case MUSHROOM:
                        if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Fungi")) {
                            return i - 36;
                        }
                        continue;
                }
            }
        }
        return 0;
    }

    public static int getAxeSlot() {
        for (int i = 36; i < 44; i++) {
            if (mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack() != null) {

                if (!FarmHelper.config.macroType) {
                    if (FarmHelper.config.VerticalMacroType == SMacroEnum.PUMPKIN_MELON.ordinal() && mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Dicer")) {
                        return i - 36;
                    }
                }
                else {
                    if ((FarmHelper.config.VerticalMacroType == SMacroEnum.COCOA_BEANS.ordinal() || FarmHelper.config.VerticalMacroType == SMacroEnum.COCOA_BEANS_RG.ordinal()) && mc.thePlayer.inventoryContainer.inventorySlots.get(i).getStack().getDisplayName().contains("Chopper")) {
                        return i - 36;
                    }
                }
            }
        }
        return 0;
    }

    public static Entity getEntityCuttingOtherEntity(Entity e) {
        List<Entity> possible = mc.theWorld.getEntitiesInAABBexcluding(e, e.getEntityBoundingBox().expand(0.3D, 2.0D, 0.3D), a -> {
            boolean flag1 = (!a.isDead && !a.equals(mc.thePlayer));
            boolean flag2 = !(a instanceof EntityArmorStand);
            boolean flag3 = !(a instanceof net.minecraft.entity.projectile.EntityFireball);
            boolean flag4 = !(a instanceof net.minecraft.entity.projectile.EntityFishHook);
            return flag1 && flag2 && flag3 && flag4;
        });
        if (!possible.isEmpty())
            return Collections.min(possible, Comparator.comparing(e2 -> e2.getDistanceToEntity(e)));
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

    public static String getItemLore(ItemStack itemStack, int index) {
        if (itemStack.hasTagCompound()) {
            return itemStack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8).getStringTagAt(index);
        }
        return null;
    }

    public static void rightClick() {
        try {
            Method rightClick = mc.getClass().getDeclaredMethod("func_147121_ag");
            rightClick.setAccessible(true);
            rightClick.invoke(mc);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            try {
                Method rightClick = mc.getClass().getDeclaredMethod("rightClickMouse");
                rightClick.setAccessible(true);
                rightClick.invoke(mc);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex2) {
                ex2.printStackTrace();
            }
        }
    }
}
