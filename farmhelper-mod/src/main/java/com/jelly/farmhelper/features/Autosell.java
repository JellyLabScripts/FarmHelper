package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.AutoSellConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.PlayerUtils;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Autosell {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean enabled;
    private static int hoeSlot;
    private static int sackSlot;
    private static State currentState;

    private static final Integer[] NPCSellSlots = {11, 16, 21, 23};
    private static Integer[] NPCSellSlotCounts = {999, 999, 999, 999};
    private static final Integer[] BZSellSlots = {10, 13, 19};

    private static boolean fullCheck;
    private static final Clock checkTimer = new Clock();
    private static int fullCount;
    private static int totalCount;

    private static Clock sellClock = new Clock();

    enum State {
        SELL_INVENTORY,
        SACKS
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void enable() {
        if (FarmHelper.gameState.cookie == GameState.EffectState.OFF) {
            LogUtils.debugLog("[AutoSell] You need a cookie for auto sell!");
            disable();
            return;
        }
        LogUtils.debugLog("[AutoSell] Started inventory sell");
        NPCSellSlotCounts = new Integer[]{999, 999, 999, 999};
        hoeSlot = mc.thePlayer.inventory.currentItem;
        sackSlot = getSack();
        sellClock.reset();
        currentState = State.SELL_INVENTORY;
        enabled = true;
    }

    public static void disable() {
        LogUtils.debugLog("[AutoSell] Finished auto sell");
        mc.thePlayer.closeScreen();
        mc.thePlayer.inventory.currentItem = hoeSlot;
        enabled = false;
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || FarmHelper.tickCount % 5 != 0)
            return;

        if (!enabled && AutoSellConfig.autoSell && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled) {
            if (mc.thePlayer.inventory.getFirstEmptyStack() == -1 && !fullCheck) {
                LogUtils.debugLog("[AutoSell] Started inventory full watch");
                fullCheck = true;
                checkTimer.schedule(TimeUnit.SECONDS.toMillis((long) AutoSellConfig.fullTime));
                fullCount = 1;
                totalCount = 1;
            } else if (fullCheck && !checkTimer.passed()) {
                if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    fullCount++;
                }
                totalCount++;
            } else if (fullCheck && checkTimer.passed()) {
                fullCheck = false;
                if (((float) fullCount / totalCount) >= (AutoSellConfig.fullRatio / 100.0)) {
                    MacroHandler.disableCurrentMacro();
                    enable();
                }
            }
        }

        if (!enabled) return;

        switch (currentState) {
            case SELL_INVENTORY:
                if(sellClock.isScheduled() && !sellClock.passed())
                    return;

                if (mc.currentScreen == null) {
                    LogUtils.debugFullLog("[AutoSell] Opening SB menu");
                    mc.thePlayer.sendChatMessage("/trades");
                    sellClock.schedule(250);
                } else if (PlayerUtils.getInventoryName() != null && PlayerUtils.getInventoryName().contains("Trades")) {
                    LogUtils.debugFullLog("[AutoSell] Detected trade menu, selling item");
                    List<Slot> sellList = PlayerUtils.getInventorySlots();
                    sellList.removeIf(item -> !shouldSell(item.getStack()));
                    if (sellList.size() > 0) {
                        PlayerUtils.clickOpenContainerSlot(45 + sellList.get(0).slotNumber);
                        sellClock.schedule(250);
                    } else {
                        LogUtils.debugFullLog("[AutoSell] Out of items to sell!");
                        if (sackContains() && sackSlot != -1) {
                            LogUtils.debugFullLog("[AutoSell] Switching to sacks");
                            currentState = State.SACKS;
                        } else {
                            LogUtils.debugFullLog("[AutoSell] Sacks are empty! Exiting");
                            disable();
                        }
                    }
                } else {
                    LogUtils.debugFullLog("[AutoSell] Unknown menu " + PlayerUtils.getInventoryName());
                    mc.thePlayer.closeScreen();
                }
                break;

            case SACKS:
                if (sackSlot == -1) {
                    LogUtils.debugFullLog("[AutoSell] No sack found! Exiting");
                    disable();
                } else if (!sackSlotsFilled()) {
                    LogUtils.debugFullLog("[AutoSell] Sack item slots not returned, waiting");
                } else if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    LogUtils.debugFullLog("[AutoSell] Full inventory! Selling");
                    countSack();
                    currentState = State.SELL_INVENTORY;
                } else if (PlayerUtils.getInventoryName() != null && PlayerUtils.getInventoryName().contains("Enchanted Agronomy Sack")) {
                    countSack();
                    if (sackContains()) {
                        LogUtils.debugFullLog("[AutoSell] Found item, claiming");
                        for (int i = 0; i < NPCSellSlots.length; i++) {
                            if (NPCSellSlotCounts[i] > 0) {
                                PlayerUtils.clickOpenContainerSlot(NPCSellSlots[i]);
                                return;
                            }
                        }
                    } else {
                        LogUtils.debugFullLog("[AutoSell] Sack is empty, selling");
                        countSack();
                        currentState = State.SELL_INVENTORY;
                    }
                } else if (PlayerUtils.getInventoryName() != null &&
                    (PlayerUtils.getInventoryName().contains("SkyBlock Menu") || PlayerUtils.getInventoryName().contains("Trades"))) {
                    LogUtils.debugFullLog("[AutoSell] In menu, opening sack");
                    PlayerUtils.clickOpenContainerSlot(45 + sackSlot, 1);
                } else if (mc.currentScreen == null) {
                    LogUtils.debugFullLog("[AutoSell] No menu, opening SB menu");
                    mc.thePlayer.inventory.currentItem = 8;
                    KeyBindUtils.rightClick();
                } else {
                    LogUtils.debugFullLog("[AutoSell] Unknown menu " + PlayerUtils.getInventoryName());
                    mc.thePlayer.closeScreen();
                }
        }
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            if (enabled) {
                LogUtils.debugFullLog("[AutoSell] Exiting sell");
                disable();
            }
        }
    }

    private static boolean shouldSell(ItemStack itemStack) {
        String name = net.minecraft.util.StringUtils.stripControlCodes(itemStack.getDisplayName());
        return (name.startsWith("Brown Mushroom") || name.startsWith("Enchanted Brown Mushroom") || name.startsWith("Brown Mushroom Block") || name.startsWith("Brown Enchanted Mushroom Block") ||
            name.startsWith("Red Mushroom") || name.startsWith("Enchanted Red Mushroom") || name.startsWith("Red Mushroom Block") || name.startsWith("Red Enchanted Mushroom Block") ||
            name.startsWith("Nether Wart") || name.startsWith("Enchanted Nether Wart") || name.startsWith("Mutant Nether Wart") ||
            name.startsWith("Sugar Cane") || name.startsWith("Enchanted Sugar") || name.startsWith("Enchanted Sugar Cane") ||
            name.startsWith("Stone")) && !name.contains("Hoe");
    }

    private static int getSack() {
        List<Slot> sellList = PlayerUtils.getInventorySlots();
        for (Slot slot : sellList) {
            String name = net.minecraft.util.StringUtils.stripControlCodes(slot.getStack().getDisplayName());
            if (name.contains("Large Enchanted Agronomy Sack")) {
                return slot.slotNumber;
            }
        }
        return -1;
    }

    private static void countSack() {
        // Count all items in sack NPC
        for (int i = 0; i < NPCSellSlots.length; i++) {
            NPCSellSlotCounts[i] = countSlotInSack(NPCSellSlots[i]);
        }
    }

    private static boolean sackContains() {
        return Arrays.stream(NPCSellSlotCounts).anyMatch(i -> i > 0);
    }

    public static int countSlotInSack(int slotID) {
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

    private static boolean sackSlotsFilled() {
        for (Integer slotID : NPCSellSlots) {
            Slot slot = mc.thePlayer.openContainer.getSlot(slotID);
            if (slot == null || slot.getStack() == null || slot.getStack().stackSize <= 0) {
                return false;
            }
        }
        return true;
    }
}
