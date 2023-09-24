package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.jelly.farmhelper.FarmHelper.config;

public class AutoSellNew {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    private static marketType selectedMarketType = marketType.NONE;
    private static StateNPC currentStateNPC = StateNPC.NONE;
    private static StateBZ currentStateBZ = StateBZ.NONE;
    private static StateSacks currentStateSacks = StateSacks.NONE;
    private static final Clock delayClock = new Clock();
    private static boolean shouldSellSacks = false;
    private static boolean isPickingSackNow = false;
    private static boolean isSackEmpty = false;
    private static final int STUCK_DELAY = (int) (7_500 + config.visitorsMacroGuiDelay * 1000 + config.visitorsMacroGuiDelayRandomness * 1000);
    private static final List<String> itemsToSell = Arrays.asList(
            "Brown Mushroom", "Enchanted Brown Mushroom", "Brown Mushroom Block", "Brown Enchanted Mushroom Block",
            "Red Mushroom", "Enchanted Red Mushroom", "Red Mushroom Block", "Red Enchanted Mushroom Block",
            "Nether Wart", "Enchanted Nether Wart", "Mutant Nether Wart",
            "Sugar Cane", "Enchanted Sugar", "Enchanted Sugar Cane",
            "Cropie", "Squash", "Fermento",
            "Stone", "Iron Hoe"
    );
    private static final List<String> garbageItems = Arrays.asList(
            "Mysterious Crop", " Rune", "Dead Bush", "Green Candy", "Purple Candy", "Iron Hoe", "Stone", "Cobblestone",
            "Velvet Top Hat", "Cashmere Jacket", "Satin Trousers", "Oxford Shoes"
    );
    public enum marketType {
        NONE,
        NPC,
        BZ,
    }
    public enum StateNPC {
        NONE,
        OPEN_MENU,
        SELL,
        CLOSE_MENU
    }
    public enum StateBZ {
        NONE,
        OPEN_MENU,
        SELL,
        CLOSE_MENU
    }
    public enum StateSacks {
        NONE,
        OPEN_MENU,
        SELECT_SACK,
        PICKUP,
        CLOSE_MENU
    }
    public static void stopMacro() {
        KeyBindUtils.stopMovement();
        shouldSellSacks = false;
        isPickingSackNow = false;
        isSackEmpty = false;
        delayClock.reset();
        currentStateNPC = StateNPC.NONE;
        currentStateBZ = StateBZ.NONE;
        currentStateSacks = StateSacks.NONE;
        if (FarmHelper.config.enableScheduler)
            Scheduler.resume();
        if (mc.thePlayer.openContainer instanceof ContainerChest)
            mc.thePlayer.closeScreen();
        UngrabUtils.regrabMouse();
    }
    public static boolean canEnableMacro() {
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return false;
        if (FailsafeNew.emergency) return false;
        if (FarmHelper.gameState.cookie != GameState.EffectState.ON) {
            LogUtils.sendError("[AutoSell] Cookie buff is not active, skipping...");
            return false;
        }
        return !isEnabled();
    }

    public static void enableMacro(marketType type, boolean pickupSacks) {
        if (!canEnableMacro()) return;
        LogUtils.sendDebug("[AutoSell] Starting the macro...");
        selectedMarketType = type;
        shouldSellSacks = pickupSacks;
        isPickingSackNow = pickupSacks;
        isSackEmpty = false;
        KeyBindUtils.stopMovement();
        if (FarmHelper.config.enableScheduler)
            Scheduler.pause();
        if (mc.thePlayer.openContainer instanceof ContainerChest)
            mc.thePlayer.closeScreen();
        if (FarmHelper.config.autoUngrabMouse)
            UngrabUtils.ungrabMouse();
        delayClock.schedule(1000);
    }

    public static boolean isEnabled() {
        return currentStateNPC != StateNPC.NONE || currentStateBZ != StateBZ.NONE || currentStateSacks != StateSacks.NONE;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FailsafeNew.emergency) return;
        if (!isEnabled()) return;
        if (LagDetection.isLagging()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (shouldSellSacks && isPickingSackNow && !isSackEmpty)
            pickupSacks();
        else if (selectedMarketType == marketType.BZ)
            sellBZ();
        else if (selectedMarketType == marketType.NPC)
            sellNPC();
    }

    private void sellNPC() {
        switch (currentStateNPC) {
            case NONE:
                break;
            case OPEN_MENU:
                if (mc.thePlayer.openContainer instanceof ContainerChest) break;
                LogUtils.sendDebug("[AutoSell] Opening Trades menu");
                mc.thePlayer.sendChatMessage("/trades");
                currentStateNPC = StateNPC.SELL;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case SELL:
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Trades")) return;
                LogUtils.sendDebug("[AutoSell] Detected trade menu, selling item");
                for (Slot slot : PlayerUtils.getInventorySlots()) {
                    if (slot.getHasStack() && itemsToSell.contains(slot.getStack().getDisplayName())) {
                        LogUtils.sendDebug("[AutoSell] Selling: " + slot.getStack().getDisplayName());
                        PlayerUtils.clickOpenContainerSlot(slot.getSlotIndex());
                        delayClock.schedule(getRandomGuiDelay());
                        break;
                    }
                }
                currentStateNPC = StateNPC.CLOSE_MENU;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Trades menu");
                if (isSackEmpty) {
                    LogUtils.sendDebug("[AutoSell] Sack is empty, stopping...");
                    stopMacro();
                }
                else if (shouldSellSacks) {
                    if (mc.thePlayer.openContainer instanceof ContainerChest)
                        mc.thePlayer.closeScreen();
                    if (FarmHelper.config.autoUngrabMouse)
                        UngrabUtils.ungrabMouse();
                    isPickingSackNow = true;
                }
                break;
        }
    }

    private void sellBZ() {
        switch (currentStateBZ) {
            case NONE:
                break;
            case OPEN_MENU:
                if (mc.thePlayer.openContainer instanceof ContainerChest) break;
                LogUtils.sendDebug("[AutoSell] Opening Bazaar menu");
                mc.thePlayer.sendChatMessage("/bz");
                currentStateBZ = StateBZ.SELL;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case SELL:
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Bazaar")) return;
                int sellSlot = PlayerUtils.getSlotFromGui("Sell Inventory Now");
                if (sellSlot == -1) break;
                String sellItemsLore = PlayerUtils.getLoreFromGuiByItemName("Sell Inventory Now");
                if (sellItemsLore == null) break;
                if (sellItemsLore.contains("You don't have anything")) {
                    currentStateBZ = StateBZ.CLOSE_MENU;
                    delayClock.schedule(getRandomGuiDelay());
                } else {
                    LogUtils.sendDebug("[AutoSell] Detected Bazaar menu, selling item");
                    PlayerUtils.clickOpenContainerSlot(sellSlot);
                    delayClock.schedule(getRandomGuiDelay());
                }
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Bazaar menu");
                if (isSackEmpty) {
                    LogUtils.sendDebug("[AutoSell] Sack is empty, stopping...");
                    stopMacro();
                }
                else if (shouldSellSacks) {
                    if (mc.thePlayer.openContainer instanceof ContainerChest)
                        mc.thePlayer.closeScreen();
                    if (FarmHelper.config.autoUngrabMouse)
                        UngrabUtils.ungrabMouse();
                    isPickingSackNow = true;
                }
                break;
        }
    }

    private void pickupSacks() {
        switch (currentStateSacks) {
            case NONE:
                break;
            case OPEN_MENU:
                if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    LogUtils.sendError("[AutoSell] Inventory is full, selling...");
                    currentStateSacks = StateSacks.CLOSE_MENU;
                    break;
                }
                if (mc.thePlayer.openContainer instanceof ContainerChest) break;
                LogUtils.sendDebug("[AutoSell] Opening Sacks menu");
                mc.thePlayer.sendChatMessage("/sacks");
                currentStateSacks = StateSacks.SELECT_SACK;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case SELECT_SACK:
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Sacks")) return;
                int sackSlot = PlayerUtils.getSlotFromGui("Agronomy Sack");
                if (sackSlot == -1) {
                    // TODO: Add multiple sack support
                    LogUtils.sendDebug("[AutoSell] No agronomy sack found, closing menu");
                    currentStateSacks = StateSacks.CLOSE_MENU;
                } else {
                    LogUtils.sendDebug("[AutoSell] Detected Sacks menu, selecting agronomy sack");
                    PlayerUtils.clickOpenContainerSlot(sackSlot, 1);
                }
                delayClock.schedule(getRandomGuiDelay());
                break;
            case PICKUP:
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Agronomy Sack")) return;
                if (PlayerUtils.getSlotFromGui("Go Back") == -1) break;
                int pickupSlot = PlayerUtils.getSlotFromGui("Pickup All");
                if (pickupSlot == -1) {
                    LogUtils.sendDebug("[AutoSell] You don't have any items to pickup, closing menu");
                    isSackEmpty = true;
                    currentStateSacks = StateSacks.CLOSE_MENU;
                } else {
                    if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                        LogUtils.sendDebug("[AutoSell] Picking up items");
                        PlayerUtils.clickOpenContainerSlot(pickupSlot);
                    } else {
                        LogUtils.sendError("[AutoSell] Inventory is full, selling soon...");
                        currentStateSacks = StateSacks.CLOSE_MENU;
                    }
                }
                delayClock.schedule(getRandomGuiDelay());
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Sacks menu");
                if (mc.thePlayer.openContainer instanceof ContainerChest) {
                    mc.thePlayer.closeScreen();
                    if (FarmHelper.config.autoUngrabMouse)
                        UngrabUtils.ungrabMouse();
                }
                isPickingSackNow = false;
                break;
        }
    }

    private static long getRandomGuiDelay() {
        return (long) ((random.nextDouble() * config.visitorsMacroGuiDelayRandomness * 1000) + (config.visitorsMacroGuiDelay * 1000));
    }
}
