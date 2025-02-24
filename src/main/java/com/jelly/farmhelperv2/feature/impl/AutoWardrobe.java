package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.InventoryUtils.ClickMode;
import com.jelly.farmhelperv2.util.InventoryUtils.ClickType;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import java.util.List;
import java.util.ArrayList;

public class AutoWardrobe implements IFeature {

    public static AutoWardrobe instance = new AutoWardrobe();
    public static int activeSlot = -1;
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private int swapTo = -1;
    private int invStart = 54;
    private int invEnd = 54;
    private List<String> equipmentsToSwapTo = new ArrayList<>();
    private State state = State.STARTING;
    private Clock timer = new Clock();

    @Override
    public String getName() {
        return "AutoWardrobe";
    }

    @Override
    public boolean isRunning() {
        return enabled;
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
    public void resetStatesAfterMacroDisabled() {
        // activeSlot = -1;
    }

    @Override
    public boolean isToggled() {
        return true;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    public void swapTo(int slot) {
        if (slot < 1 || slot > 18) {
            return;
        }

        swapTo = slot;
        enabled = true;
        LogUtils.sendSuccess("[AutoWardrobe] Starting. Swapping to slot " + slot);
    }

    public void swapTo(int slot, List<String> equipments) {
        if (slot< 1 || slot > 18) {
            return;   
        }
        swapTo = slot;
        equipmentsToSwapTo = new ArrayList(equipments);
        enabled = true;
        LogUtils.sendSuccess("[AutoWardrobe] Starting. Swapping to slot " + slot + ", and equipments: " + equipments);
    }

    @Override
    public void stop() {
        if (!enabled) {
            return;
        }
        enabled = false;
        swapTo = -1;
        invStart = 54;
        invEnd = 54;
        equipmentsToSwapTo.clear();
        state = State.STARTING;
        timer.reset();

        LogUtils.sendSuccess("[AutoWardrobe] Stopping.");
    }

    public void setState(State state, long time) {
        this.state = state;
        timer.schedule(time);
        if (time == 0) {
            timer.reset();
        }
    }

    public boolean isTimerRunning() {
        return timer.isScheduled() && !timer.passed();
    }

    public boolean hasTimerEnded() {
        return !timer.isScheduled() || timer.passed();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (!enabled || mc.thePlayer == null || event.phase != Phase.START) {
            return;
        }

        switch (state) {
            case STARTING:
                setState(State.OPENING_WD, FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case OPENING_WD:
                if (isTimerRunning()) {
                    return;
                }

                mc.thePlayer.sendChatMessage("/wd");
                setState(State.WD_VERIFY, 2000);
                break;
            case WD_VERIFY:
                if (hasTimerEnded()) {
                    LogUtils.sendError("Could not open wardrobe in under 2 seconds. Stopping");
                    setState(State.WAITING, 0);
                    return;
                }

                if (inventoryName().startsWith("Wardrobe") && InventoryUtils.isInventoryLoaded()) {
                    setState(State.NAVIGATING, FarmHelperConfig.getRandomGUIMacroDelay());
                }
                break;
            case NAVIGATING:
                if (isTimerRunning()) {
                    return;
                }
                if (swapTo < 9) {
                    setState(State.CLICKING_SLOT, 0);
                    return;
                }
                InventoryUtils.clickContainerSlot(InventoryUtils.getSlotIdOfItemInContainer("Next Page"), ClickType.LEFT, ClickMode.PICKUP);
                setState(State.NAVIGATION_VERIFY, 2000);
                break;
            case NAVIGATION_VERIFY:
                if (hasTimerEnded()) {
                    LogUtils.sendError("Could not switch to next page in under 2 seconds. Stopping");
                    setState(State.WAITING, 0);
                    return;
                }

                if (inventoryName().endsWith("2)")) {
                    setState(State.CLICKING_SLOT, FarmHelperConfig.getRandomGUIMacroDelay());
                }
                break;
            case CLICKING_SLOT:
                if (isTimerRunning()) {
                    return;
                }
                int slotId = 35 + (swapTo - 1) % 9 + 1;
                Slot slot = InventoryUtils.getSlotOfIdInContainer(slotId);
                if (slot != null && slot.getHasStack()) {
                    ItemStack stack = slot.getStack();
                    // remove this to make it unequip armor (click the same slot)
                    if (stack.hasDisplayName() && !stack.getDisplayName().contains("Equipped") && !stack.getDisplayName().contains("Locked")) {
                        InventoryUtils.clickContainerSlot(35 + (swapTo - 1) % 9 + 1, ClickType.LEFT, ClickMode.PICKUP);
                    }
                }
                activeSlot = swapTo;
                setState(State.WAITING, FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            // this is just here to give a bit extra pause before it stops
            case WAITING:
                if (isTimerRunning()) {
                    return;
                }
                if (equipmentsToSwapTo.isEmpty()) {
                    PlayerUtils.closeScreen();
                    setState(State.ENDING, FarmHelperConfig.getRandomGUIMacroDelay());
                } else {
                    setState(State.OPENING_EQ, 0);
                }
                break;
            case OPENING_EQ:
                if (isTimerRunning()) {
                    return;
                }
                mc.thePlayer.sendChatMessage("/eq");
                setState(State.EQ_VERIFY, 2000);
                break;
            case EQ_VERIFY:
                if (hasTimerEnded()) {
                    LogUtils.sendError("Could not open eq in under 2 seconds. Stopping");
                    setState(State.WAITING, 0);
                    return;
                }

                if (inventoryName().startsWith("Your Equipment") && InventoryUtils.isInventoryLoaded()) {
                    setState(State.SWAPPING_EQUIPMENT, FarmHelperConfig.getRandomGUIMacroDelay());
                    invStart = 54;
                    invEnd = mc.thePlayer.openContainer.inventorySlots.size();
                }
                break;
            case SWAPPING_EQUIPMENT:
                if (isTimerRunning()) {
                    return;
                }

                for (; invStart < invEnd; invStart++) {
                    slot = mc.thePlayer.openContainer.getSlot(invStart);
                    if (slot.getHasStack()) {
                        ItemStack stack = slot.getStack();
                        if (stack.hasDisplayName() && equipmentsToSwapTo.removeIf(it -> stack.getDisplayName().contains(it.trim()))) {
                            InventoryUtils.clickContainerSlot(invStart, ClickType.LEFT, ClickMode.PICKUP);
                            timer.schedule(FarmHelperConfig.pestFarmerEquipmentClickDelay);
                            return;
                        }
                    }
                }
                equipmentsToSwapTo.clear();
                setState(State.WAITING, FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case ENDING:
              if (isTimerRunning()) {
                return;
              }
              stop();
              break;
        }
    }

    private String inventoryName() {
        try {
            if (mc.currentScreen instanceof GuiChest) {
                final ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) {
                    return "";
                }
                final IInventory inv = chest.getLowerChestInventory();
                return inv.hasCustomName() ? inv.getName() : "";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }


    enum State {
        STARTING, OPENING_WD, WD_VERIFY, NAVIGATING, NAVIGATION_VERIFY, CLICKING_SLOT, WAITING, OPENING_EQ, EQ_VERIFY, SWAPPING_EQUIPMENT, ENDING
    }
}
