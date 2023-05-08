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


    public void toggle(boolean pause) {
        if (pause) {
            enabled = false;
            onDisable();
        } else {
            toggle();
        }
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
            if (disabledByFailsafe) {
                disabledByFailsafe = false;
                restoreStateAfterFailsafe();
            }
        } else {
            onDisable();
        }
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

    public void triggerTpCooldown() {}
}
