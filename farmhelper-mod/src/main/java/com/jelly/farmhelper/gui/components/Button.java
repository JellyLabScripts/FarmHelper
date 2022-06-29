package com.jelly.farmhelper.gui.components;

import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.events.UIClickEvent;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.awt.*;

public class Button extends UIBlock {
    private UIComponent button;
    public Button(String text) {
        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(10)))
            .setColor(new Color(36, 37, 39));

        button = new UIBlock(new Color(30, 31, 32))
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setHeight(new PixelConstraint(20))
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1f), new PixelConstraint(20)))
            .setChildOf(this);

        new UIText(text).setColor(new Color(249, 249, 249))
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1f))
            .setChildOf(button);
    }

    public void setOnClick(Function2<UIComponent, UIClickEvent, Unit> method) {
        button.onMouseClick(method);
    }
}
