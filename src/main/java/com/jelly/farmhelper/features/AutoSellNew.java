package com.jelly.farmhelper.features;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.AutoSellNPCItemsPage;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.*;
import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.jelly.farmhelper.FarmHelper.config;

public class AutoSellNew {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    public static marketType selectedMarketType = marketType.NONE;
    public static StateNPC currentStateNPC = StateNPC.NONE;
    public static StateBZ currentStateBZ = StateBZ.NONE;
    public static StateSacks currentStateSacks = StateSacks.NONE;
    public static final Clock delayClock = new Clock();
    public static boolean shouldSellSacks = false;
    public static boolean isPickingSackNow = false;
    public static boolean isSackEmpty = false;
    public static boolean isTriggeredManually = false;
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
        SELL_CONFIRM,
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
        if (!isTriggeredManually) {
            MacroHandler.enableCurrentMacro();
        }
        isTriggeredManually = false;
    }

    public static boolean canEnableMacro() {
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) {
            LogUtils.sendError("[AutoSell] You are outside the garden!");
            return false;
        }
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
        PlayerUtils.closeGuiAndUngrabMouse();

        if (shouldSellSacks && isPickingSackNow && !isSackEmpty)
            currentStateSacks = StateSacks.OPEN_MENU;
        else if (selectedMarketType == marketType.BZ)
            currentStateBZ = StateBZ.OPEN_MENU;
        else if (selectedMarketType == marketType.NPC)
            currentStateNPC = StateNPC.OPEN_MENU;

        delayClock.schedule(1000);
    }

    public static boolean isEnabled() {
        return currentStateNPC != StateNPC.NONE || currentStateBZ != StateBZ.NONE || currentStateSacks != StateSacks.NONE;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FailsafeNew.emergency) return;
        checkIfInventoryIsFull();
        if (!isEnabled()) return;
        if (LagDetection.isLagging()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (shouldSellSacks && isPickingSackNow)
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
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Trades")) break;
                int sellSlot = PlayerUtils.getSlotFromGui("Go Back");
                if (sellSlot == -1) break;
                LogUtils.sendDebug("[AutoSell] Detected trade menu, selling item");
                List<Slot> sellList = PlayerUtils.getInventorySlots();
                sellList.removeIf(item -> !shouldSellGarbage(item.getStack()));
                if (!sellList.isEmpty()) {
                    Slot slot = sellList.get(0);
                    LogUtils.sendDebug("[AutoSell] Selling: " + slot.getStack().getDisplayName() + " slot: " + slot.slotNumber);
                    PlayerUtils.clickOpenContainerSlot(45 + slot.slotNumber);
                } else {
                    LogUtils.sendDebug("[AutoSell] No items to sell, closing menu");
                    currentStateNPC = StateNPC.CLOSE_MENU;
                }
                delayClock.schedule(getRandomGuiDelay());
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Trades menu");
                if (shouldSellSacks) {
                    isPickingSackNow = true;
                    PlayerUtils.closeGuiAndUngrabMouse();
                    currentStateNPC = StateNPC.NONE;
                    delayClock.schedule(getRandomGuiDelay());
                } else {
                    if (isSackEmpty)
                        LogUtils.sendDebug("[AutoSell] Sack is empty, stopping...");
                    else
                        LogUtils.sendDebug("[AutoSell] Stopping...");
                    stopMacro();
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
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Bazaar")) break;
                int sellSlot = PlayerUtils.getSlotFromGui("Sell Inventory Now");
                if (sellSlot == -1) break;
                String sellItemsLore = PlayerUtils.getLoreFromGuiByItemName("Sell Inventory Now");
                if (sellItemsLore == null) break;
                if (sellItemsLore.contains("You don't have anything")) {
                    LogUtils.sendError("[AutoSell] You don't have anything to sell, stopping...");
                    stopMacro();
                    delayClock.schedule(getRandomGuiDelay());
                } else {
                    LogUtils.sendDebug("[AutoSell] Detected Bazaar menu, selling item");
                    PlayerUtils.clickOpenContainerSlot(sellSlot);
                    currentStateBZ = StateBZ.SELL_CONFIRM;
                    delayClock.schedule(getRandomGuiDelay());
                }
                break;
            case SELL_CONFIRM:
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Are you sure?")) break;
                int sellConfirmSlot = PlayerUtils.getSlotFromGui("Selling whole inventory");
                if (sellConfirmSlot == -1) break;
                LogUtils.sendDebug("[AutoSell] Detected Sell Confirmation menu, selling item");
                PlayerUtils.clickOpenContainerSlot(sellConfirmSlot);
                currentStateBZ = StateBZ.CLOSE_MENU;
                delayClock.schedule(getRandomGuiDelay());
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Bazaar menu");
                if (shouldSellSacks) {
                    isPickingSackNow = true;
                    PlayerUtils.closeGuiAndUngrabMouse();
                    currentStateBZ = StateBZ.NONE;
                    delayClock.schedule(getRandomGuiDelay());
                } else {
                    if (isSackEmpty)
                        LogUtils.sendDebug("[AutoSell] Sack is empty, stopping...");
                    else
                        LogUtils.sendDebug("[AutoSell] Stopping...");
                    stopMacro();
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
                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Sacks")) break;
                int sackSlot = PlayerUtils.getSlotFromGui("Agronomy Sack");
                if (sackSlot == -1) {
                    // TODO: Add multiple sack support
                    LogUtils.sendDebug("[AutoSell] No agronomy sack found, closing menu");
                    currentStateSacks = StateSacks.CLOSE_MENU;
                } else {
                    LogUtils.sendDebug("[AutoSell] Detected Sacks menu, selecting agronomy sack");
                    PlayerUtils.clickOpenContainerSlot(sackSlot, 1);
                    currentStateSacks = StateSacks.PICKUP;
                }
                delayClock.schedule(getRandomGuiDelay());
                break;
            case PICKUP:
//                if (PlayerUtils.getInventoryName() == null || !PlayerUtils.getInventoryName().contains("Agronomy Sack")) break; // This doesn't work, I don't know why
                if (Minecraft.getMinecraft().thePlayer.openContainer.inventorySlots.get(0).inventory.getName() == null) break;
                if (!Minecraft.getMinecraft().thePlayer.openContainer.inventorySlots.get(0).inventory.getDisplayName().getUnformattedText().contains("Agronomy Sack")) break;
                LogUtils.sendDebug("[AutoSell] Detected Agronomy Sack menu");
                if (PlayerUtils.getSlotFromGui("Go Back") == -1) break;
                LogUtils.sendDebug("[AutoSell] Go Back found");
                int pickupSlot = PlayerUtils.getSlotFromGui("Pickup All");
                if (pickupSlot != -1) {
                    if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
                        LogUtils.sendDebug("[AutoSell] Picking up items");
                        PlayerUtils.clickOpenContainerSlot(pickupSlot);
                    } else {
                        LogUtils.sendDebug("[AutoSell] Inventory is full, selling soon...");
                        currentStateSacks = StateSacks.CLOSE_MENU;
                    }
                } else {
                    LogUtils.sendDebug("[AutoSell] You don't have any items to pickup, closing menu");
                    isSackEmpty = true;
                    currentStateSacks = StateSacks.CLOSE_MENU;
                }
                delayClock.schedule(getRandomGuiDelay());
                break;
            case CLOSE_MENU:
                LogUtils.sendDebug("[AutoSell] Closing Sacks menu");
                PlayerUtils.closeGuiAndUngrabMouse();
                isPickingSackNow = false;
                currentStateSacks = StateSacks.NONE;
                if (selectedMarketType == marketType.BZ) {
                    LogUtils.sendDebug("[AutoSell] Selling to BZ");
                    currentStateBZ = StateBZ.OPEN_MENU;
                    delayClock.schedule(getRandomGuiDelay());
                    break;
                }
                else if (selectedMarketType == marketType.NPC) {
                    LogUtils.sendDebug("[AutoSell] Selling to NPC");
                    currentStateNPC = StateNPC.OPEN_MENU;
                    delayClock.schedule(getRandomGuiDelay());
                    break;
                }
                LogUtils.sendDebug("[AutoSell] Stopping...");
                stopMacro();
                break;
        }
    }

    private static long getRandomGuiDelay() {
        return (long) ((random.nextDouble() * config.autoSellGuiDelayRandomness * 1000) + (config.autoSellGuiDelay * 1000));
    }

    private static boolean shouldSellGarbage(ItemStack itemStack) {
        String name = net.minecraft.util.StringUtils.stripControlCodes(itemStack.getDisplayName());
        if (AutoSellNPCItemsPage.autoSellMysteriousCrop && name.contains("Mysterious Crop")) return true;
        if (AutoSellNPCItemsPage.autoSellRunes && name.contains(" Rune")) return true;
        if (AutoSellNPCItemsPage.autoSellVelvetTopHat && name.contains("Velvet Top Hat")) return true;
        if (AutoSellNPCItemsPage.autoSellCashmereJacket && name.contains("Cashmere Jacket")) return true;
        if (AutoSellNPCItemsPage.autoSellSatinTrousers && name.contains("Satin Trousers")) return true;
        if (AutoSellNPCItemsPage.autoSellOxfordShoes && name.contains("Oxford Shoes")) return true;
        if (!AutoSellNPCItemsPage.autoSellCustomItems.isEmpty()) {
            List<String> customItems = Arrays.asList(AutoSellNPCItemsPage.autoSellCustomItems.split("\\|"));
            return customItems.contains(name);
        }
        return false;
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            if (isEnabled()) {
                LogUtils.sendDebug("[AutoSell] Interruption detected, stopping...");
                stopMacro();
            }
        }
    }


    private static boolean fullCheck;
    private static final Clock checkTimer = new Clock();
    private static int fullCount;
    private static int totalCount;
    public void checkIfInventoryIsFull() {
        if (!isEnabled() && FarmHelper.config.enableAutoSell && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled) {
            if (mc.thePlayer.inventory.getFirstEmptyStack() == -1 && !fullCheck) {
                LogUtils.sendDebug("[AutoSell] Started inventory full watch");
                fullCheck = true;
                checkTimer.schedule(TimeUnit.SECONDS.toMillis(FarmHelper.config.inventoryFullTime));
                fullCount = 1;
                totalCount = 1;
            } else if (fullCheck && !checkTimer.passed()) {
                if (mc.thePlayer.inventory.getFirstEmptyStack() == -1) {
                    fullCount++;
                }
                totalCount++;
            } else if (fullCheck && checkTimer.passed()) {
                fullCheck = false;
                if (((float) fullCount / totalCount) >= (FarmHelper.config.inventoryFullRatio / 100.0)) {
                    MacroHandler.disableCurrentMacro();
                    enableMacro(marketType.BZ, true);
                }
            }
        }
    }
}
