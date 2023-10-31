package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.util.InventoryUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.SignUtils;
import lombok.Getter;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.util.ArrayList;

public class AutoGodPot implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoGodPot instance;
    public static AutoGodPot getInstance() {
        if (instance == null) {
            instance = new AutoGodPot();
        }
        return instance;
    }

    @Getter
    private final Clock delayClock = new Clock();
    @Getter
    private final Clock stuckClock = new Clock();
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.visitorsMacroGuiDelay + FarmHelperConfig.visitorsMacroGuiDelayRandomness);
    
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return false;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        if (!canEnableMacro()) return;
        if (FarmHelperConfig.autoGodPotFromAH) {
            buyState = BuyState.OPEN_AH;
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        buyState = BuyState.NONE;
        delayClock.reset();
        stuckClock.reset();
    }

    @Override
    public boolean isToggled() {
        return false;
    }

    private BuyState buyState = BuyState.NONE;

    enum BuyState {
        NONE,
        OPEN_AH,
        OPEN_BROWSER,
        CLICK_SEARCH,
        SORT_ITEMS,
        CHECK_IF_BIN,
        SELECT_ITEM,
        BUY_ITEM,
        CONFIRM_PURCHASE,
        WAIT_FOR_CONFIRMATION,
        CLOSE_AH
    }

    private boolean canEnableMacro() {
        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            LogUtils.sendError("[Auto God Pot] Cookie buff is not active, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCurrentPurse() < 1_400_000) {
            LogUtils.sendError("[Auto God Pot] Player's purse is too low, skipping...");
            return false;
        }

        return true;
    }

    ArrayList<Integer> badItems = new ArrayList<>();
    Slot godPotItem;

    private void buyItem() {
        switch (buyState) {
            case NONE:
                break;
            case OPEN_AH:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                mc.thePlayer.sendChatMessage("/ah");
                setBuyState(BuyState.OPEN_BROWSER);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case OPEN_BROWSER:
                if (!checkIfGood("Co-op Auction House")) break;
                Slot ahBrowserItem = InventoryUtils.getSlotOfItemInContainer("Auctions Browser");
                if (ahBrowserItem == null) break;
                InventoryUtils.clickContainerSlot(ahBrowserItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.CLICK_SEARCH);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_SEARCH:
                if (!checkIfGood("Auctions Browser")) break;
                Slot searchItem = InventoryUtils.getSlotOfItemInContainer("Search");
                if (searchItem == null) break;
                InventoryUtils.clickContainerSlot(searchItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.SORT_ITEMS);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                SignUtils.getInstance().setTextToWriteOnString("God Potion");
                break;
            case SORT_ITEMS:
                if (!checkIfGood("Auctions: \"God Potion\"")) break;
                Slot sortItem = InventoryUtils.getSlotOfItemInContainer("Sort");
                if (sortItem == null) break;
                InventoryUtils.clickContainerSlot(sortItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.CHECK_IF_BIN);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CHECK_IF_BIN:
                if (!checkIfGood("Auctions: \"God Potion\"")) break;
                Slot binItem = InventoryUtils.getSlotOfItemInContainer("BIN Filter");
                if (binItem == null) break;
                ItemStack binItemStack = mc.thePlayer.inventoryContainer.getSlot(binItem.slotNumber).getStack();
                if (!InventoryUtils.getItemLore(binItemStack).contains("▶ BIN Only")) {
                    InventoryUtils.clickContainerSlot(binItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    setBuyState(BuyState.CHECK_IF_BIN);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                setBuyState(BuyState.SELECT_ITEM);
                break;
            case SELECT_ITEM:
                if (!checkIfGood("Auctions: \"God Potion\"")) break;
                for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
                    if (slot.getHasStack()) {
                        String itemName = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                        if (itemName.contains("God Potion") && !badItems.contains(slot.slotNumber)) {
                            ItemStack itemLore = mc.thePlayer.inventoryContainer.getSlot(slot.slotNumber).getStack();
                            if (InventoryUtils.getItemLore(itemLore).contains("Status: Sold!")) {
                                badItems.add(slot.slotNumber);
                                break;
                            }
                            godPotItem = slot;
                            break;
                        }
                    } else
                        break;
                }
                InventoryUtils.clickContainerSlot(godPotItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.BUY_ITEM);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case BUY_ITEM:
                if (!checkIfGood("BIN Auction View")) break;
                Slot buyItem = InventoryUtils.getSlotOfItemInContainer("Buy Item Right Now");
                if (buyItem == null) {
                    if (InventoryUtils.getSlotOfItemInContainer("Collect Auction") != null) {
                        badItems.add(godPotItem.slotNumber);
                        Slot goBack = InventoryUtils.getSlotOfItemInContainer("Go Back");
                        if (goBack == null) break;
                        InventoryUtils.clickContainerSlot(goBack.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        setBuyState(BuyState.SELECT_ITEM);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    }
                    break;
                }
                ItemStack buyItemLore = mc.thePlayer.inventoryContainer.getSlot(buyItem.slotNumber).getStack();
                if (InventoryUtils.getItemLore(buyItemLore).contains("Cannot afford bid!")) {
                    LogUtils.sendError("[Auto God Pot] Cannot afford bid!");
                    setBuyState(BuyState.CLOSE_AH);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                InventoryUtils.clickContainerSlot(buyItem.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.CONFIRM_PURCHASE);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CONFIRM_PURCHASE:
                if (!checkIfGood("Confirm Purchase")) break;
                Slot confirmPurchase = InventoryUtils.getSlotOfItemInContainer("Confirm");
                if (confirmPurchase == null) break;
                InventoryUtils.clickContainerSlot(confirmPurchase.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.WAIT_FOR_CONFIRMATION);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case WAIT_FOR_CONFIRMATION:
                break;
            case CLOSE_AH:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                }
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                setBuyState(BuyState.NONE);
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isRunning()) return;
        if (event.type != 0) return;
        if (buyState != BuyState.WAIT_FOR_CONFIRMATION) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message.contains("You claimed God Potion from")) {
            setBuyState(BuyState.CLOSE_AH);
            delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
        } else if (message.contains("NOT_FOUND_OR_ALREADY_CLAIMED")
        || message.contains("There was an error with the auction house!")) {
            badItems.add(godPotItem.slotNumber);
            setBuyState(BuyState.SELECT_ITEM);
            delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
        }
    }


    private void setBuyState(BuyState state) {
        buyState = state;
        LogUtils.sendDebug("[Visitors Macro] Buy state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private boolean checkIfGood(String inventoryNameStartsWith) {
        if (mc.currentScreen == null) {
            setBuyState(BuyState.OPEN_AH);
            delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
            return false;
        }
        if (InventoryUtils.getInventoryName() == null)
            return false;
        if (!InventoryUtils.getInventoryName().startsWith(inventoryNameStartsWith)) {
            LogUtils.sendError("[Auto God Pot] Opened wrong Auction Menu! Restarting...");
            setBuyState(BuyState.OPEN_AH);
            delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
            return false;
        }
        return true;
    }
}
