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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBazaar implements IFeature {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static AutoBazaar instance = null;
    public static final int SELL_INVENTORY = 0;
    public static final int SELL_SACK = 1;

    public static AutoBazaar getInstance() {
        if (instance == null) {
            instance = new AutoBazaar();
        }
        return instance;
    }

    private final Pattern instabuyAmountPattern = Pattern.compile("Amount:\\s(\\d+)x");
    private final Pattern totalCostPattern = Pattern.compile("Price:\\s(\\d+(.\\d+)?)\\scoins");
    public String[] instaSellBtn = {"Sell inventory now", "Sell sacks now"};
    private final Clock timer = new Clock();
    private boolean enabled = false;
    private MainState mainState = MainState.BUY_FROM_BZ;
    private boolean succeeded = false;
    private boolean failed = false;
    public boolean wasManipulated = false;
    public boolean notFoundOnBZ = false;

    // Buy
    private BuyState buyState = BuyState.STARTING;
    private String itemToBuy = null;
    private int buyAmount = 0;
    private float maxSpendLimit = 0;
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

    @Override
    public void start() {
        IFeature.super.start();
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
        this.maxSpendLimit = 0;

        log("Disabling");
        IFeature.super.stop();
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

    public void buy(String itemName, int amount) {
        this.buy(itemName, amount, 0);
    }

    public void buy(String itemName, int amount, float maxSpendLimit) {
        if (this.enabled) return;

        this.enabled = true;
        this.failed = false;
        this.succeeded = false;
        this.wasManipulated = false;
        this.notFoundOnBZ = false;
        this.itemToBuy = itemName;
        this.buyAmount = amount;
        this.buyNowButtonSlot = -1;
        this.buyState = BuyState.STARTING;
        this.mainState = MainState.BUY_FROM_BZ;
        this.maxSpendLimit = maxSpendLimit;

        log("Enabling");
        start();
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
        if (event.message == null) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String boughtMessage = String.format("[Bazaar] Bought %dx %s for", this.buyAmount, this.itemToBuy);

        if (message.startsWith(boughtMessage) && (this.buyState == BuyState.BUY_VERIFY || this.buyState == BuyState.WARNING_PAGE)) {
            this.buyState = BuyState.DISABLE;
            this.timer.schedule(500);
        }

        if (message.startsWith("[Bazaar] Executing instant sell...") && this.sellState == SellState.CONFIRM_INSTASELL_VERIFY) {
            this.sellState = SellState.STARTING;
            this.timer.schedule(500);
        }
    }

    // For anyone wondering i know this might be bad because too many useless checks but you can never be too careful :D - Schizo
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
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiNameStartsWith("Bazaar ➜ \"")) {
                    log("Opened Bz.");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    this.buyState = BuyState.CLICK_ON_PRODUCT;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Cannot open bz");
                }
                break;
            case CLICK_ON_PRODUCT:
                if (!this.hasTimerEnded()) return;

                int itemSlot = InventoryUtils.getSlotIdOfItemInContainer(this.itemToBuy, true);

                // - 37 cuz it might try to click if its in inv (dont know for sure didnt check if that happens or not)
                if (itemSlot == -1 || itemSlot > mc.thePlayer.openContainer.inventorySlots.size() - 37) {
                    this.notFoundOnBZ = true;
                    this.disable("Cannot find item.");
                    return;
                }

                InventoryUtils.clickContainerSlot(itemSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.buyState = BuyState.PRODUCT_VERIFY;
                break;
            case PRODUCT_VERIFY:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiProductNameStartsWith(this.itemToBuy)) {
                    log("Opened item page.");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
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
                InventoryUtils.clickContainerSlot(buyInstantlySlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.buyState = BuyState.BUY_INSTANTLY_VERIFY;
                this.timer.schedule(2000);
                break;
            case BUY_INSTANTLY_VERIFY:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiNameStartsWith(this.itemToBuy + " ➜ Instant Buy")) {
                    log("Opened instant buy page");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());

                    Predicate<Slot> buyPredicate = slot -> slot.getHasStack()
                            && (StringUtils.stripControlCodes(slot.getStack().getDisplayName()).startsWith("Buy") || StringUtils.stripControlCodes(slot.getStack().getDisplayName()).startsWith("Fill"))
                            && slot.slotNumber < mc.thePlayer.openContainer.inventorySlots.size() - 37;
                    List<Slot> buySlots = InventoryUtils.getIndexesOfItemsFromContainer(buyPredicate);

                    if (buySlots.isEmpty()) return;

                    for (Slot slot : buySlots) {
                        String lore = String.join(" ", InventoryUtils.getItemLore(slot.getStack()));
                        Matcher matcher = this.instabuyAmountPattern.matcher(lore);

                        if (matcher.find() && this.buyAmount == Integer.parseInt(matcher.group(1).replace(",", ""))) {
                            this.buyNowButtonSlot = slot.slotNumber;
                            this.buyState = BuyState.CLICK_BUY;
                            System.out.println("Clicking button at slot: " + slot.slotNumber);
                            return;
                        }
                        if (lore.contains("Loading...")) { // Makes more sense down here
                            this.buyState = BuyState.BUY_INSTANTLY_VERIFY;
                            return;
                        }
                    }

                    Slot customAmount = InventoryUtils.getSlotOfItemInContainer("Custom Amount");
                    if (customAmount != null && customAmount.getHasStack()) {
                        this.buyState = BuyState.OPEN_SIGN;
                        this.buyNowButtonSlot = -1;
                        log("Buying custom amount");
                        return;
                    }
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not open buy instantly page.");
                }
                break;
            case OPEN_SIGN:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (!this.hasTimerEnded()) return;

                Slot signSlot = InventoryUtils.getSlotOfItemInContainer("Custom Amount");
                if (signSlot == null || !signSlot.getHasStack()) {
                    this.disable("Could not find sign.");
                    return;
                }

                InventoryUtils.clickContainerSlot(signSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                this.buyState = BuyState.OPEN_SIGN_VERIFY;
                log("In open Sign.");
                break;
            case OPEN_SIGN_VERIFY:
                if (mc.currentScreen instanceof GuiEditSign) {
                    log("Opened sign gui");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    this.buyState = BuyState.EDIT_SIGN;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Could not open sign.");
                }
                break;
            case EDIT_SIGN:
                if (!this.hasTimerEnded()) return;

                SignUtils.setTextToWriteOnString(String.valueOf(this.buyAmount));
                this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                this.buyState = BuyState.CONFIRM_SIGN;
                break;
            case CONFIRM_SIGN:
                if (!this.hasTimerEnded()) return;
                SignUtils.confirmSign();
                this.timer.schedule(2000);
                this.buyState = BuyState.VERIFY_CONFIRM_PAGE;
                break;
            case VERIFY_CONFIRM_PAGE:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiNameContains("Confirm Instant Buy")) {
                    log("Opened confirm buy page");
                    this.buyNowButtonSlot = InventoryUtils.getSlotIdOfItemInContainer("Custom Amount");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    this.buyState = BuyState.CLICK_BUY;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not open confirm page.");
                }
                break;
            case CLICK_BUY:
                if (!this.hasTimerEnded()) return;

                if (buyNowButtonSlot == -1) {
                    Slot customAmount = InventoryUtils.getSlotOfItemInContainer("Custom Amount");
                    if (customAmount != null && customAmount.getHasStack()) {
                        this.buyNowButtonSlot = customAmount.slotNumber;
                    } else {
                        System.out.println("Slot is null 2");
                        return;
                    }
                }

                Slot slot = InventoryUtils.getSlotOfIdInContainer(buyNowButtonSlot);
                if (slot == null || !slot.getHasStack()) {
                    System.out.println("Slot is null");
                    return;
                }

                boolean foundPrice = false;

                String lore = String.join(" ", InventoryUtils.getItemLore(slot.getStack())).replace(",", "");
                Matcher matcher = this.totalCostPattern.matcher(lore);
                System.out.println(lore);

                if (matcher.find()) {
                    float amount = Float.parseFloat(matcher.group(1));
                    System.out.println("Amount: " + amount);
                    System.out.println("Max spend limit: " + this.maxSpendLimit);
                    foundPrice = true;
                    if (this.maxSpendLimit > 0 && amount > this.maxSpendLimit) {
                        LogUtils.sendError("[AutoBazaar] Declining to buy due to exceeding the price limit. Item price: " + amount + ", limit: " + this.maxSpendLimit);
                        log("Attempting to spend more than allowed. Price: " + amount + ", limit: " + this.maxSpendLimit);
                        log("Disabling.");
                        this.wasManipulated = true;
                        this.disable("Spending more than allowed.");
                        return;
                    }
                }
                // For high pong gamers - Might get stuck in an inf loop here if internet is bad
                if (lore.contains("Loading...")) return;

                if (!foundPrice) {
                    this.disable("Could not find price.");
                    return;
                }

                this.buyState = BuyState.BUY_VERIFY;
                InventoryUtils.clickContainerSlot(slot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.timer.schedule(2000);
                break;
            case BUY_VERIFY:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiNameContains("Confirm") && !this.openedChestGuiNameContains("Instant Buy")) {
                    log("Opened warning page.");
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    this.buyState = BuyState.WARNING_PAGE;
                    break;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not buy item. Disabling");
                }
                // Verifying
                break;
            case WARNING_PAGE:
                if (!InventoryUtils.isInventoryLoaded()) return;
                if (this.openedChestGuiNameContains("Confirm") && !this.openedChestGuiNameContains("Instant Buy")) {
                    if (InventoryUtils.getSlotIdOfItemInContainer("WARNING") != -1) {
                        this.timer.schedule(1000);
                    } else if (InventoryUtils.getSlotIdOfItemInContainer("Confirm") != -1) {
                        this.buyState = BuyState.BUY_VERIFY;
                        InventoryUtils.clickContainerSlot(InventoryUtils.getSlotIdOfItemInContainer("Confirm"), InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        this.timer.schedule(2000);
                        log("Clicked confirm.");
                    }
                    break;
                }
                if (this.hasTimerEnded()) {
                    this.disable("Could not open confirm page.");
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
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    this.sellState = SellState.CLICK_INSTASELL;
                }

                if (this.hasTimerEnded()) {
                    this.disable("Cannot open bz");
                }
                break;
            case CLICK_INSTASELL:
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
                    this.timer.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
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

    public boolean hasNotFoundOnBZ() {
        return !this.enabled && this.notFoundOnBZ;
    }

    private boolean openedChestGuiNameContains(String guiName) {
        String openGuiName = InventoryUtils.getInventoryName();
        return (mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest) && (openGuiName != null && openGuiName.contains(guiName));
    }

    private boolean openedChestGuiNameStartsWith(String guiName) {
        String openGuiName = InventoryUtils.getInventoryName();
        System.out.println(openGuiName);
        return (mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest)
                && (openGuiName != null && (openGuiName.startsWith(guiName) || guiName.startsWith(openGuiName)));
    }

    private boolean openedChestGuiProductNameStartsWith(String productName) {
        String openGuiName = InventoryUtils.getInventoryName();
        if (openGuiName == null) return false;
        String product = openGuiName.substring(openGuiName.indexOf("➜ ") + 2);
        return (mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest)
                && (productName.startsWith(product));
    }

    private void disable(String message) {
        log(message);
        this.setSuccessStatus(false);
        this.stop();
        PlayerUtils.closeScreen();
    }

    enum MainState {
        BUY_FROM_BZ, SELL_TO_BZ,
    }

    // Insta Buy
    enum BuyState {
        STARTING, OPEN_BZ, BZ_VERIFY, CLICK_ON_PRODUCT, PRODUCT_VERIFY, CLICK_BUY_INSTANTLY, BUY_INSTANTLY_VERIFY, OPEN_SIGN, OPEN_SIGN_VERIFY, EDIT_SIGN, CONFIRM_SIGN, VERIFY_CONFIRM_PAGE, CLICK_BUY, WARNING_PAGE, BUY_VERIFY, DISABLE
    }

    // Insta Sell
    enum SellState {
        STARTING, OPEN_BZ, BZ_VERIFY, CLICK_INSTASELL, INSTASELL_VERIFY, CLICK_CONFIRM_INSTASELL, CONFIRM_INSTASELL_VERIFY, DISABLE
    }
}