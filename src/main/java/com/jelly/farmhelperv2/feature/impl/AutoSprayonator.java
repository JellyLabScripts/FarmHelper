package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.config.FarmHelperConfig.SPRAYONATOR_ITEM;
import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlotUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
    private final String skymartItemName = "Sprayonator";
    @Getter
    private final Clock sprayonatorDelay = new Clock();
    @Getter
    private final Clock enableDelay = new Clock();
    private final long sprayonatorEnableDelayTime = 5200L;
    private long sprayonatorEnableDelayTimeRand = 0L;

    {
        updateRandomDelay();
    }

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
    private final Pattern sprayTimerPattern = Pattern.compile("([a-zA-Z\\s]*)\\s?(?:(\\d+)m)?\\s+(\\d+)s");
    private boolean hasCopper = true;
    private String bazaarItemName;

    private float[] prevRotation = new float[]{0f, 0f};
    private int rotationState = 0;

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
        return false;
    }

    @Override
    public void start() {
        if (running) return;
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
        LogUtils.sendWarning("[Auto Sprayonator] Stopping");
        resetStatesAfterMacroDisabled();
        hasCopper = true;
        if (mc.currentScreen != null)
            mc.thePlayer.closeScreen();
        if (MacroHandler.getInstance().isMacroToggled())
            Multithreading.schedule(() -> MacroHandler.getInstance().resumeMacro(), 1_500, TimeUnit.MILLISECONDS);
        running = false;
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        sprayState = AUTO_SPRAYONATOR_STATE.NONE;
        running = false;
        if (!sprayonatorPlotStates.isEmpty()) {
            sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
        }
        checkPlotState = CHECK_PLOT_STATE.NONE;
        skymartPurchaseState = SKYMART_PURCHASE_STATE.NONE;
        bazaarPurchaseState = BAZAAR_PURCHASE_STATE.NONE;
        currentGuiState = CURRENT_GUI_STATE.NONE;
        updateRandomDelay();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoSprayonatorEnable;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    private boolean hasSprayonator() {
        return InventoryUtils.hasItemInInventory("Sprayonator");
    }

    private int getChosenSprayonatorSlot() {
        return FarmHelperConfig.autoSprayonatorSlot - 1;
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

    private boolean isHoldingSprayonator() {
        return mc.thePlayer.inventory.currentItem == getChosenSprayonatorSlot();
    }

    private void moveSprayonator() {
        if (getCurrentSprayonatorSlot() == getChosenSprayonatorSlot()) return;
        if (getCurrentSprayonatorSlot() == -1) return;
        if (mc.currentScreen == null) return;
        if (mc.thePlayer.inventoryContainer.getSlot(getCurrentSprayonatorSlot()).getHasStack()) {
            GuiInventory gui = (GuiInventory) mc.currentScreen;
            mc.playerController.windowClick(gui.inventorySlots.windowId, getCurrentSprayonatorSlot(), getChosenSprayonatorSlot(), 2, this.mc.thePlayer);
            Multithreading.schedule(mc.thePlayer::closeScreen, 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
            sprayState = AUTO_SPRAYONATOR_STATE.SET_SPRAYONATOR;
        }
    }

    public void resetPlots() {
        sprayonatorPlotStates.clear();
        if (sprayState == AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT) {
            sprayState = AUTO_SPRAYONATOR_STATE.NONE;
        }
    }

    private void updateRandomDelay() {
        sprayonatorEnableDelayTimeRand = (long) (Math.random() * 765);
    }

    @SubscribeEvent
    public void onTickShouldEnable(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (running) return;
        if (!isToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) {
            updateRandomDelay();
            enableDelay.schedule(sprayonatorEnableDelayTime + sprayonatorEnableDelayTimeRand);
            return;
        }
        if (!Scheduler.getInstance().isFarming()) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (sprayState != AUTO_SPRAYONATOR_STATE.NONE) return;
        sprayItem = SPRAYONATOR_ITEM.values()[FarmHelperConfig.sprayonatorType];

        System.out.println("Has sprayonator: " + hasSprayonator());
        if (!hasSprayonator()) {
            LogUtils.sendError("[Auto Sprayonator] Disabling due to no sprayonator");
            return;
        }
        if (!enableDelay.passed()) return;
        PlotData data = sprayonatorPlotStates.get(GameStateHandler.getInstance().getCurrentPlot());
        if (sprayonatorPlotStates.isEmpty() || (data != null && !data.isSprayed())) {
            LogUtils.sendWarning("[Auto Sprayonator] Activating!");
            LogUtils.sendDebug("[Auto Sprayonator] Plot Data: " + data);
            KeyBindUtils.stopMovement();
            start();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
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
                        long delay = 100 + (long) (Math.random() * 100);
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
                        }, delay / 2, TimeUnit.MILLISECONDS);
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
                if (!hasSprayItem() && !shouldBuySprayItem()) {
                    stop();
                    LogUtils.sendError("[Auto Sprayonator] Disabling until restart due to no spray item");
                    sprayState = AUTO_SPRAYONATOR_STATE.NONE;
                }
                PlotData data = sprayonatorPlotStates.get(GameStateHandler.getInstance().getCurrentPlot());
                if (data != null && (!data.isSprayed() || !data.sprayItem.equals(sprayItem.getItemName()))) {
                    LogUtils.sendSuccess("[Auto Sprayonator] Spraying plot " + GameStateHandler.getInstance().getCurrentPlot());
                    sprayState = AUTO_SPRAYONATOR_STATE.CHECK_SPRAYONATOR;
                    running = true;
                    return;
                }
                if (running && MacroHandler.getInstance().isCurrentMacroPaused()) {
                    stop();
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
                sprayonatorDelay.schedule(1000);
                if (sprayonatorInHotbar() && InventoryUtils.getSlotIdOfItemInHotbar("Sprayonator") == getChosenSprayonatorSlot()) {
                    sprayState = AUTO_SPRAYONATOR_STATE.SET_SPRAYONATOR;
                } else {
                    if (mc.currentScreen != null) {
                        Multithreading.schedule(() -> {
                            mc.thePlayer.closeScreen();
                        }, 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                        return;
                    }
                    InventoryUtils.openInventory();
                    Multithreading.schedule(this::moveSprayonator, 400 + (long) (Math.random() * 100), TimeUnit.MILLISECONDS);
                }
                break;
            case SET_SPRAYONATOR:
                if (mc.currentScreen != null) {
                    Multithreading.schedule(() -> {
                        mc.thePlayer.closeScreen();
                    }, 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                    return;
                }
                sprayonatorDelay.schedule(1000);
                if (mc.thePlayer.inventory.currentItem != getChosenSprayonatorSlot()) {
                    mc.thePlayer.inventory.currentItem = getChosenSprayonatorSlot();
                }
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(getChosenSprayonatorSlot());
                if (stack == null) return;
                List<String> itemLore = InventoryUtils.getItemLore(stack);
                if (itemLore.isEmpty()) return;
                if (itemLore.size() < 19) return;

                String sprayTypeLine = "Selected Material: Unknown";
                for (String lore : itemLore) {
                    if (lore.contains("Selected Material:")) {
                        sprayTypeLine = lore;
                        break;
                    }
                }
                String sprayType = StringUtils.stripControlCodes(sprayTypeLine.split(": ")[1]);
                if (sprayType.equals(sprayItem.getItemName())) {
                    sprayState = AUTO_SPRAYONATOR_STATE.CHECK_ITEM;
                    if (rotationState == 1) {
                        RotationHandler.getInstance().easeTo(
                                new RotationConfiguration(
                                        new Rotation(prevRotation[0], prevRotation[1]),
                                        FarmHelperConfig.getRandomRotationTime(), () -> {
                                    rotationState = 0;
                                    RotationHandler.getInstance().reset();
                                }
                                ).easeOutBack(true)
                        );
                    }
                } else {
                    switch (rotationState) {
                        case 0: // save rotations and rotate
                            prevRotation = new float[]{
                                    mc.thePlayer.rotationYaw,
                                    mc.thePlayer.rotationPitch
                            };
                            RotationHandler.getInstance().easeTo(
                                    new RotationConfiguration(
                                            new Rotation(mc.thePlayer.rotationYaw, 77),
                                            FarmHelperConfig.getRandomRotationTime(), null
                                    ).easeOutBack(true)
                            );
                            rotationState++;
                            break;
                        case 1:
                            Multithreading.schedule(KeyBindUtils::leftClick, 100 + (long) (Math.random() * 100), TimeUnit.MILLISECONDS);
                            break;
                    }

                }
                break;
            case CHECK_ITEM:
                if (hasSprayItem()) {
                    sprayState = AUTO_SPRAYONATOR_STATE.USE_SPRAYONATOR;
                } else {
                    if (shouldBuySprayItem()) {
                        sprayState = AUTO_SPRAYONATOR_STATE.BAZAAR_PURCHASE;
                        bazaarPurchaseState = BAZAAR_PURCHASE_STATE.OPEN_BAZAAR;
                    } else {
                        stop();
                        LogUtils.sendError("[Auto Sprayonator] Disabling until restart due to no spray item");
                        sprayState = AUTO_SPRAYONATOR_STATE.NONE;
                    }
                }
                break;
            case BAZAAR_PURCHASE:
                long randomDelay = (long) (Math.random() * 250);
                switch (bazaarPurchaseState) {
                    case OPEN_BAZAAR:
                        mc.thePlayer.sendChatMessage("/bz " + sprayItem.getItemName().toLowerCase());
                        bazaarItemName = sprayItem.getItemName();
                        sprayonatorDelay.schedule(1000 + randomDelay);
                        currentGuiState = CURRENT_GUI_STATE.AWAIT_OPEN;
                        break;
                    case CLICK_ITEM: // handled in gui event
                    case BUY_ITEM:
                        break;
                    case NONE:
                        break;
                }
                break;
            case USE_SPRAYONATOR:
                if (isHoldingSprayonator()) {
                    sprayonatorDelay.schedule(1000);
                    Multithreading.schedule(() -> {
                        KeyBindUtils.rightClick();
                        sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
                    }, 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                } else {
                    sprayState = AUTO_SPRAYONATOR_STATE.HOLD_SPRAYONATOR;
                }
                break;
            case NONE:
                break;
        }
    }

    @SubscribeEvent
    public void onTeleport(ReceivePacketEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!running) return;
        if (!isToggled()) return;
        if (e.packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport packet = (S18PacketEntityTeleport) e.packet;
            if (packet.getEntityId() == mc.thePlayer.getEntityId()) {
                LogUtils.sendError("[Auto Sprayonator] Disabling due to teleport.");
                stop();
            }
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
        sprayItem = SPRAYONATOR_ITEM.values()[FarmHelperConfig.sprayonatorType];

        long randomDelay = (long) (Math.random() * 250);

        switch (sprayState) {
            case CHECK_PLOTS:
                sprayonatorDelay.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay + 1000 + randomDelay);
                switch (checkPlotState) {
                    case OPEN_DESK:
                        if (!guiName.equals("Desk")) {
                            currentGuiState = CURRENT_GUI_STATE.CLOSING;
                            Multithreading.schedule(() -> {
                                mc.thePlayer.closeScreen();
                                currentGuiState = CURRENT_GUI_STATE.NONE;
                            }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                            return;
                        }
                        checkPlotState = CHECK_PLOT_STATE.OPEN_PLOTS;
                        Multithreading.schedule(() -> {
                            InventoryUtils.clickContainerSlot(InventoryUtils.getSlotIdOfItemInContainer("Configure Plots"), InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                        }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                        break;
                    case OPEN_PLOTS:
                        if (!guiName.equals("Configure Plots")) {
                            currentGuiState = CURRENT_GUI_STATE.CLOSING;
                            Multithreading.schedule(() -> {
                                mc.thePlayer.closeScreen();
                                currentGuiState = CURRENT_GUI_STATE.NONE;
                            }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
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
                        }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 150 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                }
                break;
            case BAZAAR_PURCHASE:
                sprayonatorDelay.schedule(FarmHelperConfig.autoSprayonatorAdditionalDelay + 2000 + randomDelay * 3);
                if (guiName.contains("Bazaar")) {
                    if (bazaarPurchaseState == BAZAAR_PURCHASE_STATE.OPEN_BAZAAR)
                        bazaarPurchaseState = BAZAAR_PURCHASE_STATE.CLICK_ITEM;
                } else {
                    bazaarPurchaseState = BAZAAR_PURCHASE_STATE.NONE;
                    currentGuiState = CURRENT_GUI_STATE.CLOSING;
                    Multithreading.schedule(() -> {
                        mc.thePlayer.closeScreen();
                        currentGuiState = CURRENT_GUI_STATE.NONE;
                    }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 400 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                    return;
                }
                LogUtils.sendDebug("Bazaar Purchase State: " + bazaarPurchaseState.toString());
                switch (bazaarPurchaseState) {
                    case CLICK_ITEM:
                        Multithreading.schedule(() -> {
                            Slot slot = InventoryUtils.getSlotOfItemInContainer(bazaarItemName, true);
                            LogUtils.sendDebug("Item Slot: " + (slot == null ? "null" : slot.slotNumber));
                            if (slot == null) {
                                currentGuiState = CURRENT_GUI_STATE.CLOSING;
                                Multithreading.schedule(() -> {
                                    mc.thePlayer.closeScreen();
                                    currentGuiState = CURRENT_GUI_STATE.NONE;
                                }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                                return;
                            }
                            InventoryUtils.clickContainerSlot(slot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                            bazaarPurchaseState = BAZAAR_PURCHASE_STATE.BUY_ITEM;
                            sprayonatorDelay.schedule(-1);
                        }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 300 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                        break;
                    case BUY_ITEM:
                        Multithreading.schedule(() -> {
                            LogUtils.sendDebug("Buy Item");
                            if (guiName.contains(bazaarItemName) || guiName.contains(bazaarItemName.toLowerCase())) {
                                Slot slot = InventoryUtils.getSlotOfItemInContainer("Buy Instantly", false);
                                LogUtils.sendDebug("Buy Slot: " + (slot == null ? "null" : slot.slotNumber));
                                if (slot == null) {
                                    currentGuiState = CURRENT_GUI_STATE.CLOSING;
                                    Multithreading.schedule(() -> {
                                        mc.thePlayer.closeScreen();
                                        currentGuiState = CURRENT_GUI_STATE.NONE;
                                    }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 250 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                                    return;
                                }
                                Multithreading.schedule(() -> {
                                    InventoryUtils.clickContainerSlot(slot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                                    Multithreading.schedule(() -> {
                                        InventoryUtils.clickContainerSlot(slot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                                        currentGuiState = CURRENT_GUI_STATE.CLOSING;
                                        Multithreading.schedule(() -> {
                                            mc.thePlayer.closeScreen();
                                            currentGuiState = CURRENT_GUI_STATE.NONE;
                                            sprayState = AUTO_SPRAYONATOR_STATE.CHECK_ITEM;
                                        }, 300 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                                    }, FarmHelperConfig.autoSprayonatorAdditionalDelay / 2 + 300 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                                }, FarmHelperConfig.autoSprayonatorAdditionalDelay / 2 + 300 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                                bazaarPurchaseState = BAZAAR_PURCHASE_STATE.NONE;
                            } else {
                                LogUtils.sendDebug("Invalid GUI: " + guiName);
                                currentGuiState = CURRENT_GUI_STATE.CLOSING;
                                Multithreading.schedule(() -> {
                                    mc.thePlayer.closeScreen();
                                    currentGuiState = CURRENT_GUI_STATE.NONE;
                                }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 100 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                            }
                        }, FarmHelperConfig.autoSprayonatorAdditionalDelay + 300 + (long) (Math.random() * 50), TimeUnit.MILLISECONDS);
                        break;
                    default:
                        break;
                }
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || e.message == null) return;

        String message = StringUtils.stripControlCodes(e.message.getUnformattedText());

        if (message.contains("sprayed with that item recently")) {
            sprayState = AUTO_SPRAYONATOR_STATE.CHECK_PLOTS;
            return; // Avoids additional processing if the condition is met
        }

        if (!message.startsWith("SPRAYONATOR!")) return;

        if (message.contains("sprayed")) {
            String plotNumberString = message.split(" ")[5];
            if (plotNumberString.matches("\\d+")) {
                int plotNumber = Integer.parseInt(plotNumberString);
                PlotData data = new PlotData(plotNumber, sprayItem.getItemName(), TimeUnit.MINUTES.toMillis(30));
                sprayonatorPlotStates.put(plotNumber, data);
                sprayState = AUTO_SPRAYONATOR_STATE.WAITING_FOR_PLOT;
            }
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
                        System.out.println(line);
                        Matcher matcher = sprayTimerPattern.matcher(line.replace("Sprayed with ", ""));
                        if (matcher.find()) {
                            String minutes = matcher.group(2);
                            if (minutes == null) {
                                minutes = "0";
                            }
                            String seconds = matcher.group(3);
                            long time = TimeUnit.MINUTES.toMillis(Integer.parseInt(minutes))
                                    + TimeUnit.SECONDS.toMillis(Integer.parseInt(seconds));
                            PlotData data = new PlotData(plotNumber, matcher.group(1).trim(), time);
                            foundSpray = true;
                            sprayonatorPlotStates.put(plotNumber, data);
                        }
                        break;
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

    public static class PlotData {
        @Getter
        private final int plot_number;
        @Getter
        private final Clock sprayClock = new Clock();

        @Setter
        @Getter
        private String sprayItem = "none";
        @Setter
        private long sprayTime;

        private PlotData(int plot_number) {
            this.plot_number = plot_number;
            sprayClock.schedule(0);
        }

        private PlotData(int plot_number, String spray_item, long time) {
            this.plot_number = plot_number;
            this.sprayItem = spray_item;
            this.sprayTime = time;
            sprayClock.schedule(time);
        }

        public boolean isSprayed() {
            return !sprayClock.passed();
        }

        @Override
        public String toString() {
            return "{" + "sprayed=" + isSprayed() + ", sprayItem='" + sprayItem + '\'' + ", sprayTime=" + sprayTime + ", plot_number=" + plot_number + '}';
        }
    }
}
