package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.features.Autosell;
import com.jelly.farmhelper.gui.components.Button;
import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;
import net.minecraft.client.Minecraft;

public class AutoSellMenu extends UIContainer {
    private final Minecraft mc = Minecraft.getMinecraft();
    public AutoSellMenu() {
        new Toggle("Enabled", "autoSell").setChildOf(this);
        new Toggle("Sell to NPC", "sellToNPC").setChildOf(this);
        new Slider("Inventory Full Time", 20.0, 1.0, "fullTime").setChildOf(this);
        new Slider("Inventory Full Ratio", 100.0, 1.0, "fullRatio").setChildOf(this);
        ((Button) new Button("Sell Inventory").setChildOf(this)).setOnClick((component, uiClickEvent) -> {
            mc.thePlayer.closeScreen();
            Autosell.enable();
            return null;
        });
    }
}
