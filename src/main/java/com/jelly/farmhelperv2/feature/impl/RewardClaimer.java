package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.InventoryUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class RewardClaimer implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static RewardClaimer instance;

    public static RewardClaimer getInstance() {
        if (instance == null) {
            instance = new RewardClaimer();
        }
        return instance;
    }


    private static final RotationHandler rotation = RotationHandler.getInstance();
    @Getter
    private final Clock delayClock = new Clock();
    private Entity jacob = null;

    @Nullable
    private Entity getJacob() {
        return mc.theWorld.getLoadedEntityList().
                stream().
                filter(entity -> entity.hasCustomName() && entity.getCustomNameTag().contains(StringUtils.stripControlCodes("Jacob")))
                .filter(entity -> entity.getDistanceSqToCenter(mc.thePlayer.getPosition()) <= 8)
                .min(Comparator.comparingDouble(entity -> entity.getDistanceSqToCenter(mc.thePlayer.getPosition()))).orElse(null);
    }

    @SubscribeEvent
    public void onTickEnabled(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (!isRunning()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (InventoryUtils.getInventoryName() == null && VisitorsMacro.getInstance().isInBarn()) {
            jacob = getJacob();
            if (jacob == null || jacob.getDistanceSqToEntity(mc.thePlayer) > 4 * 4) {
                return;
            }
            if (rotation.isRotating())
                return;
            rotation.easeTo(
                    new RotationConfiguration(
                            new Target(jacob),
                            FarmHelperConfig.getRandomRotationTime(),
                            () -> {
                                KeyBindUtils.leftClick();
                                RotationHandler.getInstance().reset();
                                delayClock.schedule(3_000L);
                            }
                    ).easeOutBack(true)
            );
        }

        if (InventoryUtils.getInventoryName() == null) return;
        if (InventoryUtils.getInventoryName().contains("Jacob's Farming Contest")) {
            int slot = InventoryUtils.getSlotIdOfItemInContainer("Claim your rewards!");
            if (slot == -1) return;
            InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
            delayClock.schedule(Math.random() * 300 + 700);
            return;
        }
        if (InventoryUtils.getInventoryName().contains("Your Contests")) {
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

    @Override
    public String getName() {
        return "Reward Claimer";
    }

    @Override
    public boolean isRunning() {
        return InventoryUtils.getInventoryName() != null && InventoryUtils.getInventoryName().contains("Your Contests");
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

    }

    @Override
    public void stop() {

    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        delayClock.reset();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.rewardClaimer;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }
}
