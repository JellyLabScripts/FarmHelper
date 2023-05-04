package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.gui.components.List;
import com.jelly.farmhelper.gui.menus.*;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.constraints.PixelConstraint;

public class MenuGUI extends WindowScreen {
    public UIComponent menu;

    public MenuGUI() {
//        menu = new UIContainer()
//            .setX(new PixelConstraint(0))
//            .setY(new PixelConstraint(0))
//            .setWidth(new FillConstraint())
//            .setHeight(new FillConstraint())
//            .setChildOf(getWindow());

        new List(new PixelConstraint(10), new PixelConstraint(10), "Farm Helper", new FarmMenu(), 150).setChildOf(getWindow());

        new List(new PixelConstraint(170), new PixelConstraint(10), "Remote Control", new RemoteControlMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(170), new PixelConstraint(284), "Auto Sell", new AutoSellMenu()).setChildOf(getWindow());

        new List(new PixelConstraint(360), new PixelConstraint(10), "Jacob", new JacobMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(360), new PixelConstraint(336), "Scheduler", new SchedulerMenu()).setChildOf(getWindow());

        new List(new PixelConstraint(550), new PixelConstraint(216), "Profit Calculator", new ProfitCalculatorMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(550), new PixelConstraint(10), "Miscellaneous", new MiscMenu()).setChildOf(getWindow());

        new List(new PixelConstraint(740), new PixelConstraint(10), "Failsafes", new FailsafeMenu()).setChildOf(getWindow());
    }
}