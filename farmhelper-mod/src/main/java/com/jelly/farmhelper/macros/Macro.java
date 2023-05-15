package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.events.ReceivePacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;
    public boolean savedLastState = false;


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
            if (savedLastState) {
                savedLastState = false;
                restoreState();
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

    public void saveLastStateBeforeDisable() {
        savedLastState = true;
        new Thread(() -> {
            try {
                Thread.sleep((long) (1500 + Math.random() * 2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (MacroHandler.isMacroing)
                toggle();
        }).start();
    }

    public void restoreState() {}

    public void triggerTpCooldown() {}
}
