package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.utils.ProfitCalculator;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.Window;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.state.State;

import java.awt.*;
import java.util.ArrayList;

public class ProfitGUI extends UIBlock {
    private boolean isDragging = false;
    public ArrayList<UIComponent> stats = new ArrayList<>();
    float xDragOffset;
    float yDragOffset;

    public ProfitGUI(Window window) {
        this.setX(new PixelConstraint(5)).setY(new PixelConstraint(5))
            .setWidth(new PixelConstraint(100))
            .setHeight(new ChildBasedRangeConstraint())
            .setColor(new Color(36, 37, 39, 0))
            .onMouseClick((component, uiClickEvent) -> {
                parent.removeChild(this);
                parent.addChild(this);
                return null;
            })
            .setChildOf(window);

        UIComponent topBar = new UIBlock(new Color(18, 19, 20, 100))
            .setX(new PixelConstraint(0))
            .setY(new PixelConstraint(0))
            .setWidth(new RelativeConstraint(1f))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(10)))
            .onMouseClick((component, uiClickEvent) -> {
                isDragging = true;
                xDragOffset = uiClickEvent.getAbsoluteX();
                yDragOffset = uiClickEvent.getAbsoluteY();
                return null;
            })
            .onMouseRelease(component -> {
                isDragging = false;
                return null;
            })
            .onMouseDrag((component, aFloat, aFloat2, integer) -> {
                if (!isDragging) return null;
                // To displace menu, add menu coords to absolute calc
                float absoluteX = aFloat + getLeft();
                float absoluteY = aFloat2 + getTop();

                float deltaX = absoluteX - xDragOffset;
                float deltaY = absoluteY - yDragOffset;

                xDragOffset = absoluteX;
                yDragOffset = absoluteY;
                float newX = this.getLeft() + deltaX;
                float newY = this.getTop() + deltaY;
                this.setX(new PixelConstraint(newX));
                this.setY(new PixelConstraint(newY));
                return null;
            })
            .setChildOf(this);

        new UIText("Profit Calculator", false)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1.1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(topBar);

        // the order must be followed as in Render.java
        stats.add(new Stat("profit.png").bind(ProfitCalculator.profit).setChildOf(this));
        stats.add(new Stat("profithr.png").bind(ProfitCalculator.profitHr).setChildOf(this));
        stats.add(new Stat("mnw.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("ecarrot.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("epotato.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("ehaybale.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("ecane.png").bind(ProfitCalculator.brownMushroomCount).setChildOf(this));
        stats.add(new Stat("ecocoabeans.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("emelon.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("epumpkin.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));
        stats.add(new Stat("ecactus.png").bind(ProfitCalculator.enchantedCropCount).setChildOf(this));


        stats.add(new Stat("hoe.png").bind(ProfitCalculator.counter).setChildOf(this));
        stats.add(new Stat("clock_00.png").bind(ProfitCalculator.runtime).setChildOf(this));
    }
}

class Stat extends UIBlock {
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

    public UIComponent bind(State<String> state) {
        ((UIText) displayString).bindText(state);
        return this;
    }
}
