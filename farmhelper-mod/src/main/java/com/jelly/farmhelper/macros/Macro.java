package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.config.enums.MacroEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.features.VisitorsMacro;
import com.jelly.farmhelper.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;
    public boolean disabledByFailsafe = false;


    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
            if (disabledByFailsafe) {
                disabledByFailsafe = false;
                restoreStateAfterFailsafe();
            }
        } else {
            VisitorsMacro.stopMacro();
            onDisable();
        }
        Failsafe.restartAfterFailsafeCooldown.reset();
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public void onLastRender() {}

    public void onChatMessageReceived(String msg) {}

    public void onOverlayRender(RenderGameOverlayEvent event) {}

    public void onPacketReceived(ReceivePacketEvent event) {}

    public void failsafeDisable() {
        disabledByFailsafe = true;
        toggle();
    }

    public void restoreStateAfterFailsafe() {}

    public void getTool() {
        getTool(true);
    }

    public void getTool(boolean blatant) {
        // Sometimes if staff changed your slot, you might not have the tool in your hand after the swap, so it won't be obvious that you're using a macro
        if (!blatant) {
            ItemStack tool = mc.thePlayer.getHeldItem();
            if (tool != null && !(tool.getItem() instanceof ItemHoe) && !(tool.getItem() instanceof ItemAxe)) {
                return;
            }
        }
        if (FarmConfig.cropType != MacroEnum.PUMPKIN_MELON)
            mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(MacroHandler.crop);
        else
            mc.thePlayer.inventory.currentItem = PlayerUtils.getAxeSlot();
    }
}
