package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.NumberBox;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;

public class JacobMenu extends UIContainer {
    public JacobMenu() {
        new Toggle("Jacob Failsafe", "jacobFailsafe").setChildOf(this);
        new NumberBox("Nether Wart Cap", 7, "netherWartCap").setChildOf(this);
        new NumberBox("Mushroom Cap", 7, "mushroomCap").setChildOf(this);
        new NumberBox("Carrot Cap", 7, "carrotCap").setChildOf(this);
        new NumberBox("Potato Cap", 7, "potatoCap").setChildOf(this);
        new NumberBox("Wheat Cap", 7, "wheatCap").setChildOf(this);
        new NumberBox("Sugar Cane Cap", 7, "sugarcaneCap").setChildOf(this);
    }
}
