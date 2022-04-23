package com.jelly.FarmHelper.gui;

import com.jelly.FarmHelper.utils.Utils;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;
import gg.essential.elementa.state.BasicState;
import javafx.util.Pair;

import javax.swing.plaf.nimbus.State;
import java.awt.*;

public class MenuGUI extends WindowScreen {
    public UIComponent menu;
    public MenuGUI() {
        menu = new UIContainer()
            .setX(new PixelConstraint(100))
            .setY(new PixelConstraint(100))
            .setWidth(new ChildBasedSizeConstraint())
            .setHeight(new ChildBasedSizeConstraint())
            .setChildOf(getWindow());

        new List(0, 0, "Settings").setChildOf(getWindow());
    }

}

class List extends UIBlock {
    private boolean isDragging = false;
    private Pair<Float, Float> dragOffset = new Pair<>(0f, 0f);

    public List(int x, int y, String title) {
        this.setX(new PixelConstraint(x))
            .setY(new PixelConstraint(y))
            .setWidth(new PixelConstraint(150))
            .setHeight(new ChildBasedSizeConstraint())
            .setColor(new Color(36, 37, 39))
            .onMouseClick((component, uiClickEvent) -> {
                parent.removeChild(this);
                parent.addChild(this);
                return null;
            });

        UIComponent topBar = new UIBlock(new Color(18, 19, 20))
            .setX(new PixelConstraint(0))
            .setY(new PixelConstraint(0))
            .setWidth(new RelativeConstraint(1f))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(10)))
            .onMouseClick((component, uiClickEvent) -> {
                isDragging = true;
                dragOffset = new Pair<>(uiClickEvent.getAbsoluteX(), uiClickEvent.getAbsoluteY());
                return null;
            })
            .onMouseRelease(component -> {
                isDragging = false;
                return null;
            })
            .onMouseDrag((component, aFloat, aFloat2, integer) -> {
                if (!isDragging) return null;
                float absoluteX = aFloat + getLeft();
                float absoluteY = aFloat2 + getTop();

                float deltaX = absoluteX - dragOffset.getKey();
                float deltaY = absoluteY - dragOffset.getValue();

                dragOffset = new Pair<>(absoluteX, absoluteY);
                float newX = this.getLeft() + deltaX;
                float newY = this.getTop() + deltaY;
                this.setX(new PixelConstraint(newX));
                this.setY(new PixelConstraint(newY));
                return null;
            })
            .setChildOf(this);

        new UIText(title, false)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1.1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(topBar);

        new ToggleOption("Jacob Failsafe").setChildOf(this);
        new ToggleOption("Auto Resync").setChildOf(this);
        new ToggleOption("Webhook Logs").setChildOf(this);
        new Slider("Status time", 10.0f, 1.0f).setChildOf(this);
    }
}

class ToggleOption extends UIBlock {
    private UIComponent toggle;
    private boolean toggleValue = false;

    public ToggleOption(String optionName) {
        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(10)))
            .setColor(new Color(36, 37, 39));

        new UIText(optionName, false)
            .setX(new PixelConstraint(10))
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(this);

        toggle = new UIBlock(new Color(30, 31, 32))
            .setX(new PixelConstraint(10, true))
            .setY(new CenterConstraint())
            .setHeight(new PixelConstraint(14))
            .setWidth(new PixelConstraint(14))
            .onMouseClick((component, uiClickEvent) -> {
                AnimatingConstraints anim;
                toggleValue = !toggleValue;
                if (!toggleValue) {
                    anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(30, 31, 32)));
                } else {
                    anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(175, 36, 36)));
                }
                component.animateTo(anim);
                return null;
            })
            .setChildOf(this);
    }
}

class Slider extends UIBlock {
    private UIComponent slider;
    private UIComponent sliderValue;
    private UIComponent sliderText;
    private boolean dragging = false;
    private float currentValue = 0;
    private float maxValue = 0;
    private BasicState<String> sliderTextState = new BasicState<String>("");

    public Slider(String optionName, float maxValue, float value) {
        this.maxValue = maxValue;
        this.currentValue = value;

        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedMaxSizeConstraint(), new PixelConstraint(15)))
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
            .setHeight(new PixelConstraint(14))
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
            .setWidth(new ScaleConstraint(new RelativeConstraint(1f), (currentValue / this.maxValue)))
            .setChildOf(slider);

        sliderText = new UIText(String.valueOf(currentValue), false)
            .bindText(sliderTextState)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(0.9f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(slider);
    }

    public void updateValue(float relPosX) {
        this.currentValue = (float) (Math.round(Math.min(Math.max(maxValue * (relPosX / slider.getWidth()), 0), maxValue) * 100.0) / 100.0);
        sliderValue.setWidth(new ScaleConstraint(new RelativeConstraint(1f), (currentValue / maxValue)));
        sliderTextState.set(String.valueOf(currentValue));
    }
}
