package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;
import net.minecraft.client.Minecraft;

public class MiscMenu extends UIContainer {
    public MiscMenu() {
        new Toggle("Auto GodPot", "autoGodPot").setChildOf(this);
        new Toggle("Auto Cookie", "autoCookie").setChildOf(this);
        new Toggle("Ungrab Mouse", "ungrab").setChildOf(this);
        new Toggle("Debug Mode", "debugMode").setChildOf(this);
        new Toggle("Xray", "xray").setChildOf(this);
        new Toggle("Mute Game", "muteGame").setChildOf(this);
        new Toggle("Visitors Macro", "visitorsMacro").setChildOf(this);
        new Toggle("Visitors Accept Only Profit (BETA)", "visitorsAcceptOnlyProfit").setChildOf(this);
        new Slider("Visitors Macro Money Threshold", 20, 1, "visitorsMacroMoneyThreshold").setChildOf(this);
    }
}
