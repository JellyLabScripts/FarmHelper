package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class RewardClaimer {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static RewardClaimer instance;

    public static RewardClaimer getInstance() {
        if (instance == null) {
            instance = new RewardClaimer();
        }
        return instance;
    }

    @Getter
    private final Clock delayClock = new Clock();

    @SubscribeEvent
    public void onTickEnabled(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!FarmHelperConfig.rewardClaimer) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;
        if (InventoryUtils.getInventoryName() == null || !InventoryUtils.getInventoryName().contains("Your rewards"))
            return;

        for (Slot slot : mc.thePlayer.openContainer.inventorySlots) {
            if (!slot.getHasStack()) continue;

            List<String> lore = InventoryUtils.getItemLore(slot.getStack());
            for (String line : lore) {
                if (line.contains("Click to claim reward!")) {
                    InventoryUtils.clickContainerSlot(slot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                    delayClock.schedule(Math.random() * 300 + 700);
                }
            }
        }
    }
}
