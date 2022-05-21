package com.jelly.farmhelper.macros;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public abstract class Macro {
    public Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled;

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public abstract void onTick();

    public abstract void onLastRender();

    public abstract void onChatMessageReceived(String msg);

    public abstract void onOverlayRender(RenderGameOverlayEvent event);


}
