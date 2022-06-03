package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.utils.StatusUtils;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.Window;
import gg.essential.elementa.constraints.AdditiveConstraint;
import gg.essential.elementa.constraints.CenterConstraint;
import gg.essential.elementa.constraints.ChildBasedSizeConstraint;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.elementa.state.State;

import java.awt.*;

public class StatusGUI extends UIBlock {
    UIComponent displayString;
    public StatusGUI(Window window) {
        this.setColor(new Color(36, 37, 39, 85))
            .setX(new PixelConstraint(10, true))
            .setY(new PixelConstraint(10, true))
            .setWidth(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(20)))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(10)))
            .setChildOf(window);

        displayString = new UIText("Farming for 25m 35s", false)
            .bindText(StatusUtils.status)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setChildOf(this);
    }
}
