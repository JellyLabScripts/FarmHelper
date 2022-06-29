package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.Slider;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;

public class SchedulerMenu extends UIContainer {
    public SchedulerMenu() {
        new Toggle("Enabled", "scheduler").setChildOf(this);
        new Toggle("Status GUI", "statusGUI").setChildOf(this);
        new Slider("Farm Time (min)", 500.0, 1.0, "farmTime").setChildOf(this);
        new Slider("Break Time (min)", 120.0, 1.0, "breakTime").setChildOf(this);
    }
}
