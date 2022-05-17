package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;

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

    public abstract void onRender();

    public abstract void onChatMessageReceived(String msg);
}
