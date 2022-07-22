package com.jelly.farmhelper.gui.components;

import com.jelly.farmhelper.config.ConfigHandler;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;

import java.awt.*;

public class Toggle extends UIBlock {
    private boolean toggleValue;
    private Color fillColor;

    public Toggle(String optionName, String configName) {
        // Get value from config
        toggleValue = (boolean) ConfigHandler.get(configName);
        fillColor = toggleValue ? new Color(175, 36, 36) : new Color(30, 31, 32);

        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedMaxSizeConstraint(), new PixelConstraint(10)))
            .setColor(new Color(36, 37, 39));

        new UIText(optionName, false)
            .setX(new PixelConstraint(10))
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(this);

        new UIBlock(fillColor)
            .setX(new PixelConstraint(10, true))
            .setY(new CenterConstraint())
            .setHeight(new PixelConstraint(14))
            .setWidth(new PixelConstraint(14))
            .onMouseClick((component, uiClickEvent) -> {
                toggleValue = !toggleValue;
                ConfigHandler.set(configName, toggleValue);
                fillColor = toggleValue ? new Color(175, 36, 36) : new Color(30, 31, 32);

                AnimatingConstraints anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(fillColor));
                component.animateTo(anim);
                return null;
            })
            .setChildOf(this);
    }
}