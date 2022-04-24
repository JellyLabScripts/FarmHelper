package com.jelly.FarmHelper.gui;

import com.jelly.FarmHelper.gui.components.List;
import com.jelly.FarmHelper.gui.menus.*;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.constraints.*;

public class MenuGUI extends WindowScreen {
    public UIComponent menu;

    public MenuGUI() {
//        menu = new UIContainer()
//            .setX(new PixelConstraint(0))
//            .setY(new PixelConstraint(0))
//            .setWidth(new FillConstraint())
//            .setHeight(new FillConstraint())
//            .setChildOf(getWindow());

        new List(new PixelConstraint(10), new PixelConstraint(10), "Farm Helper", new FarmMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(170), new PixelConstraint(10), "Webhooks", new WebhookMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(330), new PixelConstraint(10), "Jacob", new JacobMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(490), new PixelConstraint(10), "Auto Sell", new AutoSellMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(650), new PixelConstraint(10), "Profit Calculator", new ProfitCalculatorMenu()).setChildOf(getWindow());
        new List(new PixelConstraint(170), new PixelConstraint(200), "Miscellaneous", new MiscMenu()).setChildOf(getWindow());

    }
}