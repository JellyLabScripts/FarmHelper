package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.FarmHelperConfig;
import com.jelly.farmhelper.features.AutoCookie;
import com.jelly.farmhelper.features.AutoPot;
import com.jelly.farmhelper.gui.components.Button;
import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import com.jelly.farmhelper.utils.KeyBindUtils;
import gg.essential.elementa.components.UIContainer;
import net.minecraft.client.Minecraft;

public class MiscMenu extends UIContainer {
    private Minecraft mc = Minecraft.getMinecraft();
    public MiscMenu() {
        new Toggle("Resync", "resync").setChildOf(this);
        new Toggle("Auto GodPot", "autoGodPot").setChildOf(this);
        new Toggle("Auto Cookie", "autoCookie").setChildOf(this);
        new Toggle("Drop Stone", "dropStone").setChildOf(this);
        new Toggle("Ungrab Mouse", "ungrab").setChildOf(this);
        new Toggle("Debug Mode", "debugMode").setChildOf(this);
        new Toggle("Fastbreak", "fastbreak").setChildOf(this);
        ((Button) new Button("Buy Cookie").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            AutoCookie.enable();
            return null;
        });
        ((Button) new Button("Buy God Potion").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            AutoPot.enable();
            return null;
        });
        ((Button) new Button("Save Keybinds").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            FarmHelperConfig.set("openGUIKeybind", KeyBindUtils.customKeyBinds[0].getKeyCode());
            FarmHelperConfig.set("startScriptKeybind", KeyBindUtils.customKeyBinds[1].getKeyCode());
            return null;
        });
        new Toggle("Leave on banwave", "banwaveDisconnect").setChildOf(this);
        new Slider("Ban threshold (15 mins)", 40, 1, "banThreshold").setChildOf(this);
        new Slider("Delay before reconnect (s)", 20, 1, "reconnectDelay").setChildOf(this);

    }
}
