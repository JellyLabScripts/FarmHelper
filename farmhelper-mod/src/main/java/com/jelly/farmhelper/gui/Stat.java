package com.jelly.farmhelper.gui;

import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.state.State;

import java.awt.*;

public class Stat extends UIBlock {
    private final UIComponent displayString;

    public Stat(String iconURL) {
        this.setY(new SiblingConstraint())
                .setHeight(new ChildBasedSizeConstraint())
                .setWidth(new RelativeConstraint(1f))
                .setColor(new Color(36, 37, 39, 85));

        String path = "/assets/farmhelper/textures/gui/";

        UIImage.ofResource(path + iconURL)
                .setX(new PixelConstraint(5))
                .setY(new CenterConstraint())
                .setHeight(new PixelConstraint(15))
                .setWidth(new PixelConstraint(15))
                .setChildOf(this);

        displayString = new UIText("").setTextScale(new PixelConstraint(0.9f))
                .setColor(new Color(249, 249, 249))
                .setX(new SiblingConstraint(5))
                .setY(new CenterConstraint())
                .setChildOf(this);
    }

    public Stat(UIComponent image) {
        this.setY(new SiblingConstraint())
                .setHeight(new ChildBasedSizeConstraint())
                .setWidth(new RelativeConstraint(1f))
                .setColor(new Color(36, 37, 39, 85));

        image
                .setX(new PixelConstraint(5))
                .setY(new CenterConstraint())
                .setHeight(new PixelConstraint(15))
                .setWidth(new PixelConstraint(15))
                .setChildOf(this);

        displayString = new UIText("").setTextScale(new PixelConstraint(0.9f))
                .setColor(new Color(249, 249, 249))
                .setX(new SiblingConstraint(5))
                .setY(new CenterConstraint())
                .setChildOf(this);
    }

    public UIComponent bind(State<String> state) {
        ((UIText) displayString).bindText(state);
        return this;
    }
}
