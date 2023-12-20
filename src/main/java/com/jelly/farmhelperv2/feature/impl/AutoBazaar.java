package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBazaar implements IFeature {
    public static Minecraft mc = Minecraft.getMinecraft();
    private static AutoBazaar instance = null;
    public static int SELL_INVENTORY = 0;
    public static int SELL_SACK = 1;

    public static AutoBazaar getInstance() {
        if (instance == null) {
            instance = new AutoBazaar();
        }
        return instance;
    }

    private final Pattern instabuyAmountPattern = Pattern.compile("Amount:\\s(\\d+)x");
    private final Pattern ppuPattern = Pattern.compile("unit:\\s(\\d+(.\\d+)?)\\scoins");
    public String[] instaSellBtn = {"Sell inventory now", "Sell sacks now"};
    private final Clock timer = new Clock();
    private boolean enabled = false;
    private MainState mainState = MainState.BUY_FROM_BZ;
    private boolean succeeded = false;
    private boolean failed = false;
    public boolean wasManipulated = false;

    // Buy
    private BuyState buyState = BuyState.STARTING;
    private String itemToBuy = null;
    private int buyAmount = 0;
    private boolean checkManipulation = false;
    private int buyNowButtonSlot = -1;

    // Sell
    private final List<Integer> sellTypes = new ArrayList<>();
    private SellState sellState = SellState.STARTING;

    @Override
    public String getName() {
        return "AutoBazaar";
    }

    @Override
    public boolean isRunning() {
        return this.enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    // Cant use this prob idk
    @Override
    public void start() {
    }

    @Override
    public void stop() {
        if (!this.enabled) return;

        this.enabled = false;
        this.resetStatesAfterMacroDisabled();
        this.buyAmount = 0;
        this.itemToBuy = null;
        this.sellTypes.clear();
        this.buyNowButtonSlot = -1;

        log("Disabling");
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        this.mainState = MainState.BUY_FROM_BZ;
        this.buyState = BuyState.STARTING;
        this.sellState = SellState.STARTING;
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public void buy(String itemName, int amount, boolean checkManipulation) {
        if (this.enabled) return;

        this.enabled = true;
        this.failed = false;
        this.succeeded = false;
        this.wasManipulated = false;
        this.itemToBuy = itemName;
        this.buyAmount = amount;
        this.buyNowButtonSlot = -1;
        this.buyState = BuyState.STARTING;
        this.mainState = MainState.BUY_FROM_BZ;
        this.checkManipulation = checkManipulation;

        log("Enabling");
    }

    public void sell(Integer... sellTypes) {
        for (int i : sellTypes) {
            if (i >= this.instaSellBtn.length) continue;
            this.sellTypes.add(i);
        }
        if (this.enabled || this.sellTypes.isEmpty()) return;

        this.enabled = true;
        this.sellState = SellState.STARTING;
        this.mainState = MainState.SELL_TO_BZ;
        this.failed = false;
        this.succeeded = false;

        log("Starting Autosell");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.enabled) return;
        switch (this.mainState) {
            case BUY_FROM_BZ:
                this.handleBuyFromBz();
                break;
            case SELL_TO_BZ:
                this.handleSellToBz();
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (event.type != 0 || !this.enabled) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String boughtMessage = String.format("[Bazaar] Bought %dx %s for", this.buyAmount, this.itemToBuy);

        if (message.startsWith(boughtMessage) && this.buyState == BuyState.BUY_VERIFY) {
            this.buyState = BuyState.DISABLE;
            this.timer.schedule(500);
        }

        if (message.startsWith("[Bazaar] Executing instant sell...") && this.sellState == SellState.CONFIRM_INSTASELL_VERIFY) {
            this.sellState = SellState.STARTING;
            this.timer.schedule(500);
        }
    }

    private void handleBuyFromBz() {
        switch (this.buyState) {
            case STARTING:
                this.buyState = BuyState.OPEN_BZ;
                break;
            case OPEN_BZ:
                mc.thePlayer.sendChatMessage("/bz " + this.itemToBuy);
                this.timer.schedule(2000);
                this.buyState = BuyState.BZ_VERIFY;
                break;
            case BZ_VERIFY:
                if (this.openedChestGuiNameStartsWith("Bazaar ➜ \"")) {
                    log("Opened Bz.");
                    this.timer.schedule(500);
                    this.buyState = BuyState.CLICK_ON_PRODUCT;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Cannot open bz");
                }
                break;
            case CLICK_ON_PRODUCT:
                if (!this.hasTimerEnded()) return;

                int itemSlot = InventoryUtils.getSlotIdOfItemInContainer(this.itemToBuy, true);
                if (itemSlot == -1 || itemSlot > mc.thePlayer.openContainer.inventorySlots.size() - 37) {
                    this.disable("Cannot find item.");
                    return;
                }

                InventoryUtils.clickContainerSlot(itemSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.buyState = BuyState.PRODUCT_VERIFY;
                break;
            case PRODUCT_VERIFY:
                if (this.openedChestGuiNameContains("➜ " + this.itemToBuy)) {
                    log("Opened item page.");
                    this.timer.schedule(500);
                    this.buyState = BuyState.CLICK_BUY_INSTANTLY;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Cannot open item.");
                }
                break;
            case CLICK_BUY_INSTANTLY:
                if (!this.hasTimerEnded()) return;

                int buyInstantlySlot = InventoryUtils.getSlotIdOfItemInContainer("Buy Instantly");
                if (buyInstantlySlot == -1) {
                    this.disable("Cannot find buy instantly button. Disabling");
                    return;
                }

                if (this.checkManipulation) {

                    String buyInstantlyLore = String.join(" ", InventoryUtils.getLoreOfItemInContainer(buyInstantlySlot)).replace(",", "");
                    Matcher buyMatcher = ppuPattern.matcher(buyInstantlyLore);
                    float pricePerUnit = buyMatcher.find() ? Float.parseFloat(buyMatcher.group(1)) : 0;

                    if (pricePerUnit == 0) {
                        this.disable("Cannot find item price. Report this to developer.");
                        return;
                    }

                    ProfitCalculator.BazaarItem bazaarItem = ProfitCalculator.getInstance().getVisitorsItem("_" + this.itemToBuy);
                    if (bazaarItem != null) {
                        if (pricePerUnit > (bazaarItem.npcPrice * FarmHelperConfig.visitorsMacroPriceManipulationMultiplier)) {
                            log("Price Manipulation Detected.");
                            log("Item: " + this.itemToBuy + ", PricePerUnit: " + pricePerUnit + ", NPCPrice: " + bazaarItem.npcPrice);
                            log("NpcPriceAfterManipulation: " + bazaarItem.npcPrice * FarmHelperConfig.visitorsMacroPriceManipulationMultiplier);
                            this.wasManipulated = true;
                            this.disable("Disabling due to price being manipulated.");
                            return;
                        }
                    }
                }

                InventoryUtils.clickContainerSlot(buyInstantlySlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.buyState = BuyState.BUY_INSTANTLY_VERIY;
                this.timer.schedule(2000);
                break;
            case BUY_INSTANTLY_VERIY:
                if (this.openedChestGuiNameStartsWith(this.itemToBuy + " ➜ Instant Buy")) {
                    log("Opened instant buy page");
                    this.timer.schedule(500);
                    this.buyState = BuyState.OPEN_SIGN;

                    Predicate<Slot> buyPredicate = slot -> slot.getHasStack()
                        && StringUtils.stripControlCodes(slot.getStack().getDisplayName()).startsWith("Buy")
                        && slot.slotNumber < mc.thePlayer.openContainer.inventorySlots.size() - 37;
                    List<Slot> buySlots = InventoryUtils.getIndexesOfItemsFromContainer(buyPredicate);

                    log("BuySlotsIsEmpty: " + (buySlots.isEmpty()));

                    if (buySlots.isEmpty()) return;

                    for (Slot slot : buySlots) {
                        String lore = String.join(" ", InventoryUtils.getItemLore(slot.getStack()));

                        if (lore.contains("Loading...")) { // Mainly for high pingers becuz lore doesnt load quick enuf
                            this.buyState = BuyState.BUY_INSTANTLY_VERIY;
                            return;
                        }

                        Matcher matcher = this.instabuyAmountPattern.matcher(lore);

                        if (matcher.find() && this.buyAmount == Integer.parseInt(matcher.group(1))) {
                            this.buyState = BuyState.BUY_NOW_BTN;
                            this.buyNowButtonSlot = slot.slotNumber;
                            return;
                        }
                    }
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not open buy instantly page.");
                }
                break;
            case OPEN_SIGN:
                if (!this.hasTimerEnded()) return;

                int signSlot = InventoryUtils.getSlotIdOfItemInContainer("Custom Amount");
                if (signSlot == -1) {
                    this.disable("Could not find sign.");
                    return;
                }

                InventoryUtils.clickContainerSlot(signSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.buyState = BuyState.OPEN_SIGN_VERIFY;
                log("In open Sign.");
                break;
            case OPEN_SIGN_VERIFY:
                if (mc.currentScreen instanceof GuiEditSign) {
                    log("Opened sign gui");
                    this.timer.schedule(500);
                    this.buyState = BuyState.EDIT_SIGN;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Could not open sign.");
                }
                break;
            case EDIT_SIGN:
                if (!this.hasTimerEnded()) return;

                SignUtils.setTextToWriteOnString(String.valueOf(this.buyAmount));
                this.timer.schedule(2000);
                this.buyState = BuyState.VERIFY_CONFIRM_PAGE;
                break;
            case VERIFY_CONFIRM_PAGE:
                if (this.openedChestGuiNameContains("Confirm Instant Buy")) {
                    log("Opened confirm buy page");
                    this.timer.schedule(500);
                    this.buyState = BuyState.CLICK_CONFIRM;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not open confirm page.");
                }
                break;
            case CLICK_CONFIRM:
                if (!this.hasTimerEnded()) return;

                int customAmountSlot = InventoryUtils.getSlotIdOfItemInContainer("Custom Amount");
                if (customAmountSlot == -1) {
                    this.disable("Could not find slot");
                    return;
                }

                InventoryUtils.clickContainerSlot(customAmountSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.buyState = BuyState.BUY_VERIFY;
                this.timer.schedule(2000);
                break;
            case BUY_NOW_BTN:
                if (!this.hasTimerEnded()) return;

                if (this.buyNowButtonSlot == -1) {
                    log("Cannot Find Buy One Slot. Switching to Sign.");
                    this.buyState = BuyState.OPEN_SIGN;
                    return;
                }
                InventoryUtils.clickContainerSlot(this.buyNowButtonSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.buyState = BuyState.BUY_VERIFY;
                this.timer.schedule(2000);
                break;
            case BUY_VERIFY:
                if (this.hasTimerEnded()) {
                    this.disable("Could not buy item. Disabling");
                }
                // Verifying
                break;
            case DISABLE:
                if (!this.hasTimerEnded()) return;

                PlayerUtils.closeScreen();
                this.setSuccessStatus(true);
                this.stop();
                log("Finished Buying");
                break;
        }
    }

    private void handleSellToBz() {
        switch (this.sellState) {
            case STARTING:
                this.sellState = SellState.OPEN_BZ;

                if (this.sellTypes.isEmpty()) {
                    this.sellState = SellState.DISABLE;
                }
                break;
            case OPEN_BZ:
                mc.thePlayer.sendChatMessage("/bz");
                this.timer.schedule(2000);
                this.sellState = SellState.BZ_VERIFY;
                break;
            case BZ_VERIFY:
                if (this.openedChestGuiNameStartsWith("Bazaar ➜ ")) {
                    log("Opened Bz.");
                    this.timer.schedule(500);
                    this.sellState = SellState.CLICK_INSTASELLL;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Cannot open bz");
                }
                break;
            case CLICK_INSTASELLL:
                if (!this.hasTimerEnded()) return;

                int sellType = this.sellTypes.get(0);
                this.sellTypes.remove(0);

                int itemSlot = InventoryUtils.getSlotIdOfItemInContainer(this.instaSellBtn[sellType], true);
                if (itemSlot == -1) {
                    if (sellType == SELL_SACK) {
                        this.sellState = SellState.STARTING;
                    } else {
                        this.disable("Cannot find instasell btn.");
                        return;
                    }
                }

                String lore = String.join(" ", InventoryUtils.getLoreOfItemInContainer(itemSlot));
                if (lore.contains("You don't have anything to sell!")) {
                    this.sellState = SellState.STARTING;
                    return;
                }

                InventoryUtils.clickContainerSlot(itemSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.sellState = SellState.INSTASELL_VERIFY;
                break;
            case INSTASELL_VERIFY:
                if (this.openedChestGuiNameContains("Are you sure?")) {
                    log("Opened instasell page.");
                    this.timer.schedule(500);
                    this.sellState = SellState.CLICK_CONFIRM_INSTASELL;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Cannot open instasell page.");
                }
                break;
            case CLICK_CONFIRM_INSTASELL:
                if (!this.hasTimerEnded()) return;

                int instaSellConfirmSlot = InventoryUtils.getSlotIdOfItemInContainer("Selling whole inventory");
                if (instaSellConfirmSlot == -1) {
                    this.disable("Cannot find instasell confirm btn.");
                    return;
                }

                InventoryUtils.clickContainerSlot(instaSellConfirmSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.sellState = SellState.CONFIRM_INSTASELL_VERIFY;
                break;
            case CONFIRM_INSTASELL_VERIFY:
                if (this.hasTimerEnded()) {
                    this.disable("Failed to verify instasell");
                }
                break;
            case DISABLE:
                if (!this.hasTimerEnded()) return;

                PlayerUtils.closeScreen();
                this.setSuccessStatus(true);
                this.stop();
                break;
        }
    }

    // Utils
    private void log(String message) {
        LogUtils.sendDebug(String.format("[%s] - %s", this.getName(), message));
    }

    private boolean hasTimerEnded() {
        return this.timer.isScheduled() && this.timer.passed();
    }

    private void setSuccessStatus(boolean succeeded) {
        this.succeeded = succeeded;
        this.failed = !succeeded;
    }

    public boolean hasSucceeded() {
        return !this.enabled && this.succeeded;
    }

    public boolean hasFailed() {
        return !this.enabled && this.failed;
    }

    public boolean wasPriceManipulated() {
        return !this.enabled && this.wasManipulated;
    }

    private boolean openedChestGuiNameContains(String guiName) {
        String openGuiName = InventoryUtils.getInventoryName();
        return (mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest) && (openGuiName != null && openGuiName.contains(guiName));
    }

    private boolean openedChestGuiNameStartsWith(String guiName) {
        String openGuiName = InventoryUtils.getInventoryName();
        return (mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest)
            && (openGuiName != null && openGuiName.startsWith(guiName));
    }

    private void disable(String message) {
        log(message);
        this.setSuccessStatus(false);
        this.stop();
    }

    enum MainState {
        BUY_FROM_BZ, SELL_TO_BZ,
    }

    // Insta Buy
    enum BuyState {
        STARTING, OPEN_BZ, BZ_VERIFY, CLICK_ON_PRODUCT, PRODUCT_VERIFY, CLICK_BUY_INSTANTLY, BUY_INSTANTLY_VERIY, OPEN_SIGN, OPEN_SIGN_VERIFY, EDIT_SIGN, BUY_NOW_BTN, VERIFY_CONFIRM_PAGE, CLICK_CONFIRM, BUY_VERIFY, DISABLE
    }

    // Insta Sell
    enum SellState {
        STARTING, OPEN_BZ, BZ_VERIFY, CLICK_INSTASELLL, INSTASELL_VERIFY, CLICK_CONFIRM_INSTASELL, CONFIRM_INSTASELL_VERIFY, DISABLE
    }
}