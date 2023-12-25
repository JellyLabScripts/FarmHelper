package com.jelly.farmhelperv2.failsafe.impl;

import com.jelly.farmhelperv2.config.page.FailsafeNotificationsPage;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.Failsafe;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.PlayerUtils;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;

public class ItemChangeFailsafe extends Failsafe {
    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public FailsafeManager.EmergencyType getType() {
        return FailsafeManager.EmergencyType.ITEM_CHANGE_CHECK;
    }

    @Override
    public boolean shouldSendNotification() {
        return FailsafeNotificationsPage.notifyOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldPlaySound() {
        return FailsafeNotificationsPage.alertOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldTagEveryone() {
        return FailsafeNotificationsPage.tagEveryoneOnItemChangeFailsafe;
    }

    @Override
    public boolean shouldAltTab() {
        return FailsafeNotificationsPage.autoAltTabOnItemChangeFailsafe;
    }

    @Override
    public void onReceivedPacketDetection(ReceivePacketEvent event) {
        if (MacroHandler.getInstance().isTeleporting()) return;

        if (!(event.packet instanceof S2FPacketSetSlot)) return;

        S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
        int slot = packet.func_149173_d();
        int farmingToolSlot = -1;
        try {
            farmingToolSlot = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (farmingToolSlot == -1) return;
        ItemStack farmingTool = mc.thePlayer.inventory.getStackInSlot(farmingToolSlot);
        if (slot == 36 + farmingToolSlot &&
                farmingTool != null &&
                !(farmingTool.getItem() instanceof ItemHoe) &&
                !(farmingTool.getItem() instanceof ItemAxe)) {
            LogUtils.sendDebug("[Failsafe] No farming tool in hand! Slot: " + slot);
            FailsafeManager.getInstance().possibleDetection(this);
        }
    }

    @Override
    public void duringFailsafeTrigger() {

    }

    @Override
    public void endOfFailsafeTrigger() {

    }

    enum ItemChangeState {
        NONE,
        WAIT_BEFORE_START,
        LOOK_AROUND,
        SWAP_BACK_ITEM,
        END
    }
}
