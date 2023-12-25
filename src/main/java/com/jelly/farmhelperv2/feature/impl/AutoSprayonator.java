package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoSprayonator implements IFeature {
    private static AutoSprayonator instance = null;

    public static AutoSprayonator getInstance() {
        if (instance == null) {
            instance = new AutoSprayonator();
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final String SPRAYONATOR = "Sprayonator";
    private boolean enabled = false;
    private MainState mainState = MainState.NONE;
    private CheckPlotState checkState = CheckPlotState.STARTING;
    private BuyState buyState = BuyState.STARTING;
    private SprayState sprayState = SprayState.STARTING;
    private final String[] SPRAYONATOR_ITEM = {"Compost", "Honey Jar", "Dung", "Plant Matter", "Tasty Cheese"};
    private Plot[] plots = new Plot[25];
    private final Clock timer = new Clock();
    private final Pattern sprayonatorTimePattern = Pattern.compile("(((\\d+)m\\s)?(\\d+)s)");
    private int counter = 0; // I literally forgot the proper word kill me
    private boolean isRotating = false;
    private boolean changedMaterial = false;
    private boolean pauseSprayonatorForThisSession = false;

    @Override
    public String getName() {
        return "AutoSprayonator";
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
        if (this.enabled) return;

        if (!InventoryUtils.hasItemInInventory(SPRAYONATOR)) {
            LogUtils.sendError("Cannot Find Sprayonator in Inventory. Disabling AutoSprayonator until restart.");
            this.pauseSprayonatorForThisSession = true;
            return;
        }

        this.enabled = true;
        this.mainState = MainState.STARTING;
        MacroHandler.getInstance().pauseMacro();
        log("Activating");
    }

    @Override
    public void stop() {
        if (!this.enabled) return;

        this.enabled = false;
        this.counter = 0;
        this.mainState = MainState.NONE;
        this.checkState = CheckPlotState.STARTING;
        this.buyState = BuyState.STARTING;
        this.sprayState = SprayState.STARTING;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().resumeMacro();
        }

        log("Disabling");
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        this.pauseSprayonatorForThisSession = false;
    }

    @Override
    public boolean isToggled() { // more like should start
        return FarmHelperConfig.autoSprayonatorEnable;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    // Thanks Xai <3
    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (this.enabled || this.pauseSprayonatorForThisSession) return;
        if (!this.isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) return;
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (this.mainState != MainState.NONE) return;

        Plot currentPlot = this.plots[GameStateHandler.getInstance().getCurrentPlot()];
        if (currentPlot == null || currentPlot.canSpray()) this.counter++;
        else this.counter = 0;
        if (this.counter > 150) {
            this.start();
        }
    }

    @SubscribeEvent
    public void onTickMainState(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.enabled) return;
        switch (this.mainState) {
            case NONE:
                break;
            case STARTING:
                // Remove these variables if u need
                Plot currentPlot = this.plots[GameStateHandler.getInstance().getCurrentPlot()];
                boolean shouldCheckPlots = Arrays.stream(this.plots).noneMatch(Objects::nonNull);
                boolean sprayonatorItemNotInInventory = !InventoryUtils.hasItemInInventory(this.SPRAYONATOR_ITEM[FarmHelperConfig.autoSprayonatorType]);
                boolean canSpray = currentPlot != null && currentPlot.canSpray();

                if (shouldCheckPlots) {
                    this.mainState = MainState.CHECK_PLOTS;
                    this.checkState = CheckPlotState.STARTING;
                } else if (sprayonatorItemNotInInventory) {
                    log("Cannot find sprayonator item in inventory.");
                    if (FarmHelperConfig.autoSprayonatorAutoBuyItem) {
                        this.mainState = MainState.BUY_ITEM;
                        this.buyState = BuyState.STARTING;
                    } else {
                        log("Disabling AutoSprayonator until restart.");
                        this.pauseSprayonatorForThisSession = true;
                        this.stop();
                        return;
                    }
                } else if (canSpray) {
                    this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                    this.mainState = MainState.SPRAY_PLOT;
                    this.sprayState = SprayState.STARTING;
                } else {
                    log("Cannot spray/Plot is already sprayed. Disabling");
                    this.stop();
                }
                break;
            case CHECK_PLOTS:
                this.handleCheckPlots();
                break;
            case BUY_ITEM:
                this.handleBuyItem();
                break;
            case SPRAY_PLOT:
                this.handleSprayPlot();
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (event.type != 0 || !this.enabled) return;
        if (event.message == null) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedTextForChat());
        if (message.startsWith("SPRAYONATOR! Your selected material is now ") && this.sprayState == SprayState.SPRAYONATOR_TYPE_VERIFY) {
            log("Sprayonator Material Changed");
            String itemName = SPRAYONATOR_ITEM[FarmHelperConfig.autoSprayonatorType];
            this.changedMaterial = true;
            if (message.contains(itemName)) {
                this.changedMaterial = false;
                this.sprayState = SprayState.STARTING;
            }
        }
        if (message.startsWith("SPRAYONATOR! You sprayed Plot - ") && this.sprayState == SprayState.SPRAY_VERIFY) {
            this.plots[GameStateHandler.getInstance().getCurrentPlot()].setSprayExpireTime(System.currentTimeMillis() + 1800000); // 30 minutes
            this.sprayState = SprayState.DISABLE;
            this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
        }
        if (message.contains("This plot was sprayed with that item recently! Try again soon!")) {
            this.plots = new Plot[25];
            this.mainState = MainState.STARTING;
            this.checkState = CheckPlotState.STARTING;
        }
    }

    private void handleCheckPlots() {
        switch (this.checkState) {
            case STARTING:
                this.checkState = CheckPlotState.OPEN_DESK;
                break;
            case OPEN_DESK:
                mc.thePlayer.sendChatMessage("/desk");
                this.checkState = CheckPlotState.VERIFY_DESK;
                this.timer.schedule(1000);
                break;
            case VERIFY_DESK:
                if (this.timer.isScheduled() && this.timer.passed()) {
                    log("Could not open desk. Disabling");
                    this.stop();
                    return;
                }
                // WHy null? Why not ""? so that i can use contains without null issues??? why man?
                String inventoryName = InventoryUtils.getInventoryName();
                if ((mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest)
                    && (inventoryName != null && inventoryName.contains("Desk"))) {
                    log("Opened Desk.");
                    this.checkState = CheckPlotState.OPEN_PLOTS;
                    this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                }
                break;
            case OPEN_PLOTS:
                if (!this.hasTimerEnded()) return;
                InventoryUtils.clickContainerSlot(InventoryUtils.getSlotIdOfItemInContainer("Configure Plots"), InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                this.checkState = CheckPlotState.VERIFY_PLOTS;
                this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                log("Clicked on Configure Plots Hopefully");
                break;
            case VERIFY_PLOTS:
                if (this.timer.isScheduled() && this.timer.passed()) {
                    log("Could not open plots. Disabling");
                    this.stop();
                    return;
                }
                String inventoryName2 = InventoryUtils.getInventoryName();
                if ((mc.currentScreen instanceof GuiChest || mc.thePlayer.openContainer instanceof ContainerChest)
                    && (inventoryName2 != null && inventoryName2.contains("Configure Plots"))) {
                    log("In Configure Plots Menu");
                    this.checkState = CheckPlotState.SCAN_PLOTS;
                    this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                }
                break;
            case SCAN_PLOTS:
                if (!this.hasTimerEnded()) return;
                int currentPlotIndex = 0;
                log("Scanning Plots");
                for (int i = 0; i < mc.thePlayer.openContainer.inventorySlots.size() - 37; i++) {
                    Slot slot = mc.thePlayer.openContainer.getSlot(i);
                    ItemStack slotStack = slot.getStack();
                    if (slotStack == null) continue;
                    if (slotStack.getDisplayName().contains("The Barn")) {
                        currentPlotIndex++;
                        continue;
                    }
                    if (!slotStack.getDisplayName().contains("Plot")) continue;

                    int actualPlotNumber = PlotUtils.getPLOT_NUMBERS().get(currentPlotIndex);
                    int minute = 0;
                    int second = 0;
                    String lore = String.join(" ", InventoryUtils.getLoreOfItemInContainer(i));
                    Matcher timeMatcher = this.sprayonatorTimePattern.matcher(lore);
                    if (lore.contains("Sprayed with ") && timeMatcher.find()) {
                        second = Integer.parseInt(timeMatcher.group(timeMatcher.groupCount()));
                        String minuteString = timeMatcher.group(timeMatcher.groupCount() - 1);
                        if (minuteString != null) {
                            minute = Integer.parseInt(minuteString);
                        }
                    }
                    long sprayExpiryTime = System.currentTimeMillis() + (minute * 60000L) + (second * 1000L);
                    this.plots[actualPlotNumber] = new Plot(actualPlotNumber, sprayExpiryTime);
                    currentPlotIndex++;
                }
                log(Arrays.toString(this.plots));
                this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                this.checkState = CheckPlotState.DISABLE;
                break;
            case DISABLE:
                if (!this.hasTimerEnded()) return;
                this.checkState = CheckPlotState.STARTING;
                this.mainState = MainState.STARTING;
                PlayerUtils.closeScreen();
                break;
        }
    }

    private void handleBuyItem() {
        switch (this.buyState) {
            case STARTING:
                this.buyState = BuyState.TOGGLE_AUTO_BZ;
                break;
            case TOGGLE_AUTO_BZ:
                AutoBazaar.getInstance().buy(SPRAYONATOR_ITEM[FarmHelperConfig.autoSprayonatorType], FarmHelperConfig.autoSprayonatorAutoBuyAmount);
                this.buyState = BuyState.VERIFY_AUTO_BZ;
                break;
            case VERIFY_AUTO_BZ:
                if (AutoBazaar.getInstance().hasFailed()) {
                    LogUtils.sendError("[AutoSprayonator] Failed to buy item from bazaar. Disabling AutoSprayonator until restart");
                    this.pauseSprayonatorForThisSession = true;
                    this.stop();
                    return;
                }
                if (AutoBazaar.getInstance().hasSucceeded()) {
                    log("Bought item from bazaar.");
                    this.buyState = BuyState.DISABLE;
                }
                break;
            case DISABLE:
                this.buyState = BuyState.STARTING;
                this.mainState = MainState.STARTING;
                break;
        }
    }

    private void handleSprayPlot() {
        switch (sprayState) {
            case STARTING:
                if (!this.hasTimerEnded()) return;

                PlayerUtils.closeScreen();

                this.sprayState = SprayState.USE_SPRAYONATOR;

                // Not necessary but just in case
                if (!InventoryUtils.hasItemInInventory(SPRAYONATOR)) {
                    this.stop();
                    LogUtils.sendError("Sprayonator not in inventory.");
                    return;
                }
                if (!InventoryUtils.hasItemInHotbar(SPRAYONATOR)) {
                    InventoryUtils.openInventory();
                    this.sprayState = SprayState.SWAP_SPRAYONATOR_TO_HOTBAR;
                    this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                    return;
                }
                if (!InventoryUtils.holdItem(SPRAYONATOR)) {
                    this.stop();
                    LogUtils.sendError("Cannot hold sprayonator.");
                    return;
                }
                String sprayonatorLore = String.join(" ", InventoryUtils.getLoreOfItemInContainer(InventoryUtils.getSlotIdOfItemInContainer(SPRAYONATOR)));
                if (!sprayonatorLore.contains(this.SPRAYONATOR_ITEM[FarmHelperConfig.autoSprayonatorType])) {
                    this.sprayState = SprayState.CHANGE_SPRAYONATOR_TYPE;
                    this.isRotating = true;

                    RotationConfiguration rotationConfig = new RotationConfiguration(
                        new Rotation(AngleUtils.getActualYawFrom360(mc.thePlayer.rotationYaw), -60),
                        500,
                        () -> {
                            this.isRotating = false;
                            RotationHandler.getInstance().reset();
                        });
                    RotationHandler.getInstance().easeTo(rotationConfig);

                    return;
                }
                break;
            case CHANGE_SPRAYONATOR_TYPE:
                if (this.isRotating || !this.hasTimerEnded()) return;

                KeyBindUtils.leftClick();
                this.sprayState = SprayState.SPRAYONATOR_TYPE_VERIFY;
                this.timer.schedule(2000);
                break;
            case SPRAYONATOR_TYPE_VERIFY:
                if (this.hasTimerEnded()) {
                    log("Cannot verify sprayonator type");
                    this.stop();
                    return;
                }
                if (!this.changedMaterial) return;

                this.changedMaterial = false;
                this.sprayState = SprayState.CHANGE_SPRAYONATOR_TYPE;
                this.timer.schedule(500);
                break;
            case SWAP_SPRAYONATOR_TO_HOTBAR:
                if (!this.hasTimerEnded()) return;

                InventoryUtils.swapSlots(InventoryUtils.getSlotIdOfItemInInventory(SPRAYONATOR), FarmHelperConfig.autoSprayonatorSlot - 1);
                this.timer.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay);
                this.sprayState = SprayState.STARTING;
                break;
            case USE_SPRAYONATOR:
                KeyBindUtils.rightClick();
                this.sprayState = SprayState.SPRAY_VERIFY;
                this.timer.schedule(2000);
                break;
            case SPRAY_VERIFY:
                if (this.hasTimerEnded()) {
                    LogUtils.sendError("Could not Spray Plot. Disabling.");
                    this.stop();
                    return;
                }
            case DISABLE:
                if (!this.hasTimerEnded()) return;
                log("Disabling spray handler.");
                this.stop();
                break;
        }
    }

    private boolean hasTimerEnded() {
        return (this.timer.isScheduled() && this.timer.passed());
    }

    private void log(String message) {
        LogUtils.sendDebug("[AutoSprayonator] " + message);
    }

    public void resetPlots() {
        this.plots = new Plot[25];
    }

    enum MainState {
        NONE, STARTING, CHECK_PLOTS, BUY_ITEM, SPRAY_PLOT
    }

    enum CheckPlotState {
        STARTING, OPEN_DESK, VERIFY_DESK,
        OPEN_PLOTS, VERIFY_PLOTS,
        SCAN_PLOTS, DISABLE
    }

    enum BuyState {
        STARTING, TOGGLE_AUTO_BZ, VERIFY_AUTO_BZ, DISABLE
    }

    enum SprayState {
        STARTING, CHANGE_SPRAYONATOR_TYPE, SPRAYONATOR_TYPE_VERIFY, SWAP_SPRAYONATOR_TO_HOTBAR, USE_SPRAYONATOR, SPRAY_VERIFY, DISABLE
    }

    @Getter
    @Setter
    static class Plot {
        private final int plotNumber;
        private long sprayExpireTime = 0L;

        public Plot(int plotNumber, long sprayExpireTime) {
            this.plotNumber = plotNumber;
            this.sprayExpireTime = sprayExpireTime;
        }

        public boolean canSpray() {
            return System.currentTimeMillis() > sprayExpireTime;
        }

        @Override
        public String toString() {
            return String.format("Plot[%d, canSpray = %b]", this.plotNumber, this.canSpray());
        }
    }
}