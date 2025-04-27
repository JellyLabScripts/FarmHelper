package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.event.InventoryInputEvent;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class VerifyFarmingEquipment implements IFeature {
    private enum EquipState {NONE, SENT_COMMAND, WAITING_FOR_GUI, VERIFYING_EQUIPMENT, VERIFYING_ARMOUR, VERIFIED}
    private enum FarmingArmour {Rabbit, Farm, Melon, Cropie, Squash, Fermento}
//    private enum FarmingPet {Rabbit, Mooshroom, Elephant}
    private static boolean equipmentOK = false;
    private boolean armourOK = false;
    private boolean hasRunBefore = false;

    private static VerifyFarmingEquipment instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Clock guiClock = new Clock();
    private EquipState state = EquipState.NONE;

    public static VerifyFarmingEquipment getInstance() {
        if (instance == null) instance = new VerifyFarmingEquipment();
        return instance;
    }

    @Override
    public void start() {
        if (!armourOK && hasRunBefore) { checkArmourStealth(); }
        if (LotusEquipListener.hasEquippedLotus()) { equipmentOK = true; }
        if (hasRunBefore) {
            statusMessages();
            return;
        }

        mc.thePlayer.sendChatMessage("/equipment");

        state = EquipState.SENT_COMMAND;
        guiClock.schedule(1_000);
        IFeature.super.start();
    }

    @Override
    public void stop() {
        state = EquipState.NONE;
        PlayerUtils.closeScreen();
        IFeature.super.stop();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (state == EquipState.SENT_COMMAND && guiClock.passed()) {
            state = EquipState.WAITING_FOR_GUI;
        }

        switch (state) {
            case WAITING_FOR_GUI: {
                LogUtils.sendDebug("Waiting for GUI");
                if (mc.currentScreen instanceof GuiChest
                        && InventoryUtils.isInventoryLoaded()
                        && InventoryUtils.getInventoryName() != null
                        && InventoryUtils.getInventoryName().contains("Equipment")) {
                    LogUtils.sendDebug("GUI Loaded");
                    state = EquipState.VERIFYING_EQUIPMENT;
                }
            }
            case VERIFYING_EQUIPMENT: {
                LogUtils.sendDebug("Checking Farming Equipment");
                int[] equipmentSlotsToCheck = {10, 19, 28, 37};
                for (int slotId : equipmentSlotsToCheck) {
                    Slot slot = InventoryUtils.getSlotOfIdInContainer(slotId);
                    if (slot != null && slot.getHasStack()) {
                        String name = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                        if (name.contains("Lotus")) {
                            LogUtils.sendDebug("Farming Equipment " + name + " found in slot id: " + slotId);
                            equipmentOK = true;
                        }
                    }
                }
                LogUtils.sendDebug("Farming Equipment Checked");
                state = EquipState.VERIFYING_ARMOUR;
            }
            case VERIFYING_ARMOUR: {
                LogUtils.sendDebug("Checking Farming Armour");
                int[] armourSlotsToCheck = {11, 20, 29, 38};
                for (int slotId : armourSlotsToCheck) {
                    Slot slot = InventoryUtils.getSlotOfIdInContainer(slotId);
                    if (slot != null && slot.getHasStack()) {
                        String name = StringUtils.stripControlCodes(slot.getStack().getDisplayName());
                        for (FarmingArmour fa : FarmingArmour.values()) {
                            if (name.contains(fa.name())) {
                                LogUtils.sendDebug("Farming Armour " + fa.name() + " found in slot id: " + slotId);
                                armourOK = true;
                            }
                        }
                    }
                }
                LogUtils.sendDebug("Farming Armour Checked");
                state = EquipState.VERIFIED;
            }
            case VERIFIED: {
                LogUtils.sendDebug("Verified Farming Equipment");
                statusMessages();
                hasRunBefore = true;
                stop();
            }
        }
    }

    private void checkArmourStealth() {
        ItemStack[] armor = InventoryUtils.getEquippedArmor();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getItem() != null) {
                String name = StringUtils.stripControlCodes(piece.getDisplayName());
                for (FarmingArmour fa : FarmingArmour.values()) {
                    if (name.contains(fa.name())) {
                        armourOK = true;
                        return;
                    }
                }
            }
        }
    }

    private void statusMessages() {
        if (equipmentOK) {
            LogUtils.sendSuccess("Farming Equipment Equipped!");
        } else {
            LogUtils.sendError("No Farming Equipment is Equipped!");
        }
        if (armourOK) {
            LogUtils.sendSuccess("Farming Armour Equipped!");
        } else {
            LogUtils.sendError("No Farming Armour is Equipped!");
        }
    }

    private void resetVariables() {
        equipmentOK = false;
        armourOK = false;
        hasRunBefore = false;
    }

    @SubscribeEvent
    public void onKeypress(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) return;

        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            LogUtils.sendWarning("[Equipment Verifier] Stopping manually");
            resetVariables();
            stop();
        }
    }

    @SubscribeEvent
    public void onInventoryKeyPress(InventoryInputEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) return;

        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            LogUtils.sendWarning("[Equipment Verifier] Stopping manually");
            resetVariables();
            stop();
        }
    }

    @Override public String getName() { return "Equipment Scanner"; }
    @Override public boolean isRunning() { return state != EquipState.NONE; }
    @Override public boolean isToggled() { return true; }
    @Override public boolean shouldPauseMacroExecution() { return true; }
    @Override public boolean shouldStartAtMacroStart() { return true; }
    @Override public void resetStatesAfterMacroDisabled() { stop(); }
    @Override public boolean shouldCheckForFailsafes() { return true; }
}












