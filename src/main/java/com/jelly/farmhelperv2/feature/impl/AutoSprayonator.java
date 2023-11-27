package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.FarmHelperConfig.SPRAYONATOR_ITEM;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoSprayonator implements IFeature {
    @Getter
    private static final AutoSprayonator instance = new AutoSprayonator();

    @Getter
    private final HashMap<Integer, PlotData> sprayonatorPlotStates = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean running;

    @Getter
    private SPRAYONATOR_ITEM sprayItem = SPRAYONATOR_ITEM.values()[FarmHelperConfig.sprayonatorType];
    @Getter
    private AUTO_SPRAYONATOR_STATE sprayState = AUTO_SPRAYONATOR_STATE.NONE;
    @Getter
    private CHECK_PLOT_STATE checkPlotState = CHECK_PLOT_STATE.NONE;
    @Getter
    private SKYMART_PURCHASE_STATE skymartPurchaseState = SKYMART_PURCHASE_STATE.NONE;
    @Getter
    private BAZAAR_PURCHASE_STATE bazaarPurchaseState = BAZAAR_PURCHASE_STATE.NONE;
    @Getter
    private CURRENT_GUI_STATE currentGuiState = CURRENT_GUI_STATE.NONE;
    private Pattern sprayTimerPattern = Pattern.compile("(\\w+)\\s(\\d+)m\\s(\\d+)s");
    private final String skymartItemName = "Sprayonator";
    private boolean hasCopper = true;
    private String bazaarItemName;

    @Getter
    private final Clock sprayonatorDelay = new Clock();

    @Override
    public String getName() {
        return "Auto Sprayonator";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return true;
    }

    @Override
    public void start() {
        if (running) return;
        if (MacroHandler.getInstance().isMacroToggled())
            MacroHandler.getInstance().pauseMacro();
        running = true;

        sprayItem = SPRAYONATOR_ITEM.values()[FarmHelperConfig.sprayonatorType];
        if (sprayonatorPlotStates.isEmpty()) {
            sprayState = AUTO_SPRAYONATOR_STATE.CHECK_PLOTS;
        } else {
            sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
        }
        sprayonatorDelay.schedule(100);
    }

    @Override
    public void stop() {
        if (!running) return;
        resetStatesAfterMacroDisabled();
        hasCopper = true;
        if (mc.currentScreen != null)
            mc.thePlayer.closeScreen();
        if (MacroHandler.getInstance().isMacroToggled())
            Multithreading.schedule(() -> {
                if (MacroHandler.getInstance().isMacroToggled()) {
                    MacroHandler.getInstance().resumeMacro();
                }
            }, 1_500, TimeUnit.MILLISECONDS);
        running = false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        sprayState = AUTO_SPRAYONATOR_STATE.NONE;
        if (!sprayonatorPlotStates.isEmpty()) {
            sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
        }
        checkPlotState = CHECK_PLOT_STATE.NONE;
        skymartPurchaseState = SKYMART_PURCHASE_STATE.NONE;
        bazaarPurchaseState = BAZAAR_PURCHASE_STATE.NONE;
        currentGuiState = CURRENT_GUI_STATE.NONE;
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.enableSprayonator;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    private boolean hasSprayonator() {
        return InventoryUtils.hasItemInInventory("Sprayonator");
    }
    private int getChosenSprayonatorSlot() {
        return FarmHelperConfig.sprayonatorSlot-1;
    }
    private int getCurrentSprayonatorSlot() {
        return InventoryUtils.getSlotIdOfItemInInventory("Sprayonator");
    }
    private boolean hasSprayItem() {
        return InventoryUtils.hasItemInInventory(sprayItem.getItemName());
    }
    private boolean shouldBuySprayItem() {
        return !FarmHelperConfig.sprayonatorItemInventoryOnly && !hasSprayItem();
    }

    private boolean sprayonatorInHotbar() {
        return InventoryUtils.hasItemInHotbar("Sprayonator");
    }

    private void moveSprayonator() {
        if (getCurrentSprayonatorSlot() == getChosenSprayonatorSlot()) return;
        if (getCurrentSprayonatorSlot() == -1) return;
        if (mc.currentScreen == null) return;
        if (mc.thePlayer.inventoryContainer.getSlot(getCurrentSprayonatorSlot()).getHasStack()) {
            GuiInventory gui = (GuiInventory) mc.currentScreen;
            mc.playerController.windowClick(gui.inventorySlots.windowId, getCurrentSprayonatorSlot(), getChosenSprayonatorSlot(), 2, this.mc.thePlayer);
            Multithreading.schedule(mc.thePlayer::closeScreen, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
            sprayState = AUTO_SPRAYONATOR_STATE.SET_SPRAYONATOR;
        }
    }

    public void resetPlots() {
        sprayonatorPlotStates.clear();
        if (sprayState == AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT) {
            sprayState = AUTO_SPRAYONATOR_STATE.NONE;
        }
    }

    enum AUTO_SPRAYONATOR_STATE {
        CHECK_PLOTS, // save states of all plots
        WAITING_FOR_PLOT, // waiting to enter a non sprayed plot
        CHECK_SPRAYONATOR, // check if sprayonator in inventory
        SKYMART_PURCHASE, // buy sprayonator if not in inventory
        HOLD_SPRAYONATOR, // move sprayonator into inventory slot
        SET_SPRAYONATOR, // set sprayonator item to correct item
        CHECK_ITEM, // check if item is in inventory
        BAZAAR_PURCHASE, // buy item if not in inventory
        USE_SPRAYONATOR, // use sprayonator on current plot
        NONE,
    }

    enum CHECK_PLOT_STATE {
        OPEN_DESK,
        OPEN_PLOTS,
        END,
        NONE
    }

    enum SKYMART_PURCHASE_STATE {
        OPEN_DESK,
        OPEN_SKYMART,
        PURCHASE_ITEM,
        END,
        NONE
    }

    enum BAZAAR_PURCHASE_STATE {
        OPEN_BAZAAR,
        CLICK_ITEM,
        BUY_ITEM,
        NONE
    }

    enum CURRENT_GUI_STATE {
        AWAIT_OPEN,
        CLOSING,
        NONE
    }

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (running) return;
        if (!isToggled()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;

        LogUtils.sendError("[Auto Sprayonator] Attempting Enable");

        if (!hasSprayonator() && GameStateHandler.getInstance().getCopper() < 25) {
            LogUtils.sendError("[Auto Sprayonator] Disabling due to no sprayonator");
            return;
        }

        if (sprayonatorPlotStates.isEmpty() || !sprayonatorPlotStates.get(GameStateHandler.getInstance().getCurrentPlot()).sprayed) {
            LogUtils.sendWarning("[Auto Sprayonator] Activating!");
            KeyBindUtils.stopMovement();
            start();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (sprayState != AUTO_SPRAYONATOR_STATE.NONE && sprayState != AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT && !MacroHandler.getInstance().isCurrentMacroPaused()) {
            MacroHandler.getInstance().pauseMacro();
            return;
        }
        if (currentGuiState != CURRENT_GUI_STATE.NONE) return;
        if (!sprayonatorDelay.passed()) return;
        switch (sprayState) {
            case CHECK_PLOTS:
                switch (checkPlotState) {
                    case OPEN_DESK:
                        long delay = 100 + (long)(Math.random() * 100);
                        sprayonatorDelay.schedule(500 + delay);
                        if (mc.currentScreen != null && mc.currentScreen instanceof GuiChest) {
                            // close gui and wait
                            Multithreading.schedule(() -> {
                                mc.thePlayer.closeScreen();
                            }, delay, TimeUnit.MILLISECONDS);
                            return;
                        }
                        currentGuiState = CURRENT_GUI_STATE.AWAIT_OPEN;
                        Multithreading.schedule(() -> {
                            mc.thePlayer.sendChatMessage("/desk");
                        }, delay/2, TimeUnit.MILLISECONDS);
                        break;
                    case END:
                        checkPlotState = CHECK_PLOT_STATE.NONE;
                        sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
                        break;
                    case NONE:
                        checkPlotState = CHECK_PLOT_STATE.OPEN_DESK;
                        break;
                    default:
                        break;
                }
                break;
            case WAITING_FOR_PLOT:
                if (sprayonatorPlotStates.isEmpty()) {
                    sprayState = AUTO_SPRAYONATOR_STATE.CHECK_PLOTS;
                    return;
                }
                PlotData data = sprayonatorPlotStates.get(GameStateHandler.getInstance().getCurrentPlot());
                if (!data.sprayed || !data.spray_item.equals(sprayItem.getItemName())) {
                    sprayState = AUTO_SPRAYONATOR_STATE.CHECK_SPRAYONATOR;
                    return;
                }
                break;
            case CHECK_SPRAYONATOR:
                if (hasSprayonator()) {
                    sprayState = AUTO_SPRAYONATOR_STATE.HOLD_SPRAYONATOR;
                } else {
                    sprayState = AUTO_SPRAYONATOR_STATE.SKYMART_PURCHASE;
                }
                return;
            case SKYMART_PURCHASE:
                switch (skymartPurchaseState) {
                    case OPEN_DESK:
                        break;
                    case OPEN_SKYMART:
                        break;
                    case PURCHASE_ITEM:
                        break;
                    case END:
                        break;
                    case NONE:
                        break;
                }
                break;
            case HOLD_SPRAYONATOR:
                sprayonatorDelay.schedule(2000);
                if (sprayonatorInHotbar() && InventoryUtils.getSlotIdOfItemInHotbar("Sprayonator") == getChosenSprayonatorSlot()) {
                    sprayState = AUTO_SPRAYONATOR_STATE.SET_SPRAYONATOR;
                } else {
                    if (mc.currentScreen != null) {
                        Multithreading.schedule(() -> {
                            mc.thePlayer.closeScreen();
                        }, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                        return;
                    }
                    InventoryUtils.openInventory();
                    Multithreading.schedule(this::moveSprayonator, 400 + (long)(Math.random() * 100), TimeUnit.MILLISECONDS);
                }
                break;
            case SET_SPRAYONATOR:
                if (mc.currentScreen != null) {
                    Multithreading.schedule(() -> {
                        mc.thePlayer.closeScreen();
                    }, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                    return;
                }
                sprayonatorDelay.schedule(1000);
                if (mc.thePlayer.inventory.currentItem != getChosenSprayonatorSlot())
                    mc.thePlayer.inventory.currentItem = getChosenSprayonatorSlot();
                List<String> itemLore = InventoryUtils.getItemLore(mc.thePlayer.inventory.getStackInSlot(getChosenSprayonatorSlot()));
                if (itemLore.isEmpty()) return;
                if (itemLore.size() < 19) return;
                String sprayTypeLine = itemLore.get(18);
                String sprayType = StringUtils.stripControlCodes(sprayTypeLine.split(": ")[1]);
                if (sprayType.equals(sprayItem.getItemName())) {
                    sprayState = AUTO_SPRAYONATOR_STATE.CHECK_ITEM;
                } else {
                    Multithreading.schedule(KeyBindUtils::leftClick, 100 + (long)(Math.random() * 100), TimeUnit.MILLISECONDS);
                }
                break;
            case CHECK_ITEM:
                break;
            case BAZAAR_PURCHASE:
                switch (bazaarPurchaseState) {
                    case OPEN_BAZAAR:
                        break;
                    case CLICK_ITEM:
                        break;
                    case BUY_ITEM:
                        break;
                    case NONE:
                        break;
                }
                break;
            case USE_SPRAYONATOR:
                break;
            case NONE:
                break;
        }
    }

    @SubscribeEvent
    public void onGuiOpen(DrawScreenAfterEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!sprayonatorDelay.passed()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!running) return;
        if (!(event.guiScreen instanceof GuiChest)) return;
        String guiName = InventoryUtils.getInventoryName();
        if (guiName == null) return;
        ContainerChest guiChest = (ContainerChest) ((GuiChest) event.guiScreen).inventorySlots;
        if (currentGuiState != CURRENT_GUI_STATE.AWAIT_OPEN) return;
        sprayonatorDelay.schedule(1000);

        switch (sprayState) {
            case CHECK_PLOTS:
                switch (checkPlotState) {
                    case OPEN_DESK:
                        if (!guiName.equals("Desk")) {
                            currentGuiState = CURRENT_GUI_STATE.CLOSING;
                            Multithreading.schedule(() -> {
                                mc.thePlayer.closeScreen();
                                currentGuiState = CURRENT_GUI_STATE.NONE;
                            }, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                            return;
                        }
                        checkPlotState = CHECK_PLOT_STATE.OPEN_PLOTS;
                        Multithreading.schedule(() -> {
                            InventoryUtils.clickContainerSlot(InventoryUtils.getSlotIdOfItemInContainer("Configure Plots"), InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        }, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                        break;
                    case OPEN_PLOTS:
                        if (!guiName.equals("Configure Plots")) {
                            currentGuiState = CURRENT_GUI_STATE.CLOSING;
                            Multithreading.schedule(() -> {
                                mc.thePlayer.closeScreen();
                                currentGuiState = CURRENT_GUI_STATE.NONE;
                            }, 100 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                            return;
                        }
                        loadSprayonatorData(guiChest);
                        currentGuiState = CURRENT_GUI_STATE.CLOSING;
                        checkPlotState = CHECK_PLOT_STATE.END;
                        sprayState = AUTO_SPRAYONATOR_STATE.NONE;
                        Multithreading.schedule(() -> {
                            mc.thePlayer.closeScreen();
                            sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
                            checkPlotState = CHECK_PLOT_STATE.NONE;
                            currentGuiState = CURRENT_GUI_STATE.NONE;
                        }, 150 + (long)(Math.random() * 50), TimeUnit.MILLISECONDS);
                }
                break;
            default:
                break;
        }
    }

    private void loadSprayonatorData(ContainerChest guiChest) {
        int plotCounter = 0;
        for (int i = 0; i < guiChest.inventorySlots.size(); i++) {
            Slot slot = guiChest.inventorySlots.get(i);
            if (slot == null || !slot.getHasStack()) continue;
            if (slot.getStack().getDisplayName().contains("Plot")) {
                int plotNumber = PlotUtils.getPLOT_NUMBERS().get(plotCounter);
                List<String> lore = InventoryUtils.getItemLore(slot.getStack());
                boolean foundSpray = false;
                for (String line : lore) {
                    if (line.contains("Sprayed with")) {
                        Matcher matcher = sprayTimerPattern.matcher(line);
                        if (matcher.find()) {
                            String type = matcher.group(1);
                            int minutes = Integer.parseInt(matcher.group(2));
                            int seconds = Integer.parseInt(matcher.group(3));

                            PlotData data = new PlotData(plotNumber, type,(minutes * 60L + seconds) * 1000);
                            sprayonatorPlotStates.put(plotNumber, data);
                            foundSpray = true;
                            break;
                        }
                    }
                }
                if (!foundSpray) {
                    PlotData data = new PlotData(plotNumber);
                    sprayonatorPlotStates.put(plotNumber, data);
                }
                plotCounter++;
            } else if (slot.getStack().getDisplayName().contains("Barn")) {
                plotCounter++;
            }
        }
        sprayonatorPlotStates.forEach((k, v) -> {
            LogUtils.sendDebug("Plot " + k + " : " + v.toString());
        });
    }

    private static class PlotData {
        @Getter @Setter
        private boolean sprayed = false;
        @Setter
        private String spray_item = "none";
        @Setter
        private long spray_time;
        @Getter
        private final int plot_number;
        @Getter
        private final Clock sprayClock = new Clock();

        private PlotData(int plot_number) {
            this.plot_number = plot_number;
        }

        private PlotData(int plot_number, String spray_item, long time) {
            this.plot_number = plot_number;
            this.spray_item = spray_item;
            this.spray_time = time;
            this.sprayed = true;
            sprayClock.schedule(spray_time);
        }

        @Override
        public String toString() {
            return "{" + "sprayed=" + sprayed + ", sprayItem='" + spray_item + '\'' + ", spray_time=" + spray_time + ", plot_number=" + plot_number + '}';
        }
    }
}
