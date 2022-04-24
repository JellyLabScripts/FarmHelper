package com.jelly.FarmHelper.gui.components;

import com.jelly.FarmHelper.config.FarmHelperConfig;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.state.BasicState;

import java.awt.*;

public class Slider extends UIBlock {
    private UIComponent slider;
    private UIComponent sliderValue;
    private boolean dragging = false;
    private double currentValue = 0;
    private double maxValue = 0;
    private double step = 0;
    private String configName;
    private BasicState<String> sliderTextState = new BasicState<String>("");

    public Slider(String optionName, double maxValue, double step, String configName) {
        // Get current value from config
        this.currentValue = (Double) FarmHelperConfig.get(configName);
        this.configName = configName;
        this.maxValue = maxValue;
        this.step = step;
        sliderTextState.set(String.valueOf(this.currentValue));

        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(20)))
            .setColor(new Color(36, 37, 39));

        new UIText(optionName, false)
            .setX(new PixelConstraint(10))
            .setY(new PixelConstraint(5))
            .setTextScale(new PixelConstraint(1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(this);

        slider = new UIBlock(new Color(30, 31, 32))
            .setX(new CenterConstraint())
            .setY(new AdditiveConstraint(new SiblingConstraint(), new PixelConstraint(5)))
            .setHeight(new PixelConstraint(15))
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1f), new PixelConstraint(20)))
            .onMouseClick((component, uiClickEvent) -> {
                dragging = true;
                updateValue(uiClickEvent.getRelativeX());
                return null;
            })
            .onMouseLeave(component -> {
                dragging = false;
                return null;
            })
            .onMouseDrag((component, aFloat, aFloat2, integer) -> {
                if (!dragging) return null;
                updateValue(aFloat);
                return null;
            })
            .setChildOf(this);

        // slider value
        sliderValue = new UIBlock(new Color(175, 36, 36))
            .setX(new PixelConstraint(0))
            .setY(new PixelConstraint(0))
            .setHeight(new RelativeConstraint(1))
            .setWidth(new ScaleConstraint(new RelativeConstraint(1f), (float) (currentValue / this.maxValue)))
            .setChildOf(slider);

        new UIText(String.valueOf(currentValue), false)
            .bindText(sliderTextState)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(0.9f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(slider);
    }

    public void updateValue(float relPosX) {
        this.currentValue = Math.round(Math.min(Math.max(maxValue * (relPosX / slider.getWidth()), 0), maxValue) * (1/step)) / (1/step);
        sliderValue.setWidth(new ScaleConstraint(new RelativeConstraint(1f), (float) (currentValue / maxValue)));
        sliderTextState.set(String.valueOf(currentValue));
        FarmHelperConfig.set(configName, currentValue);
    }
}