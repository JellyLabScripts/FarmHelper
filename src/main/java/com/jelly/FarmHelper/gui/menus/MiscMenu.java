package com.jelly.FarmHelper.gui.menus;

import com.jelly.FarmHelper.config.FarmHelperConfig;
import com.jelly.FarmHelper.gui.components.Button;
import com.jelly.FarmHelper.gui.components.Toggle;
import com.jelly.FarmHelper.utils.Utils;
import gg.essential.elementa.components.UIContainer;
import me.acattoXD.WartMacro;
import net.minecraft.client.Minecraft;

import java.util.concurrent.TimeUnit;

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
            WartMacro.openedGUI = false;
            mc.thePlayer.closeScreen();
            WartMacro.buying = true;
            WartMacro.timeoutStart = System.currentTimeMillis();
            Utils.ScheduleRunnable(WartMacro.buyCookie, 1, TimeUnit.SECONDS);
            return null;
        });
        ((Button) new Button("Buy God Potion").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            WartMacro.openedGUI = false;
            mc.thePlayer.closeScreen();
            WartMacro.buying = true;
            WartMacro.timeoutStart = System.currentTimeMillis();
            Utils.ScheduleRunnable(WartMacro.buyGodPot, 1, TimeUnit.SECONDS);
            return null;
        });
        ((Button) new Button("Save Keybinds").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            FarmHelperConfig.set("openGUIKeybind", WartMacro.customKeyBinds[0].getKeyCode());
            FarmHelperConfig.set("startScriptKeybind", WartMacro.customKeyBinds[1].getKeyCode());
            return null;
        });
    }
}
