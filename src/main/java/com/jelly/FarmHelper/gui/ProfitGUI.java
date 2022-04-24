package com.jelly.FarmHelper.gui;

import com.jelly.FarmHelper.config.interfaces.FarmConfig;
import com.jelly.FarmHelper.utils.ProfitUtils;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.Window;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.state.State;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
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

        UIComponent topBar = new UIBlock(new Color(18, 19, 20, 40))
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

        stats.add(new Stat("https://i.ibb.co/HrWKbsw/profit.png").bind(ProfitUtils.profit).setChildOf(this));
        stats.add(new Stat("https://i.ibb.co/BP29c8v/profithr.png").bind(ProfitUtils.profitHr).setChildOf(this));
        stats.add(new Stat("https://i.ibb.co/8dv1fR0/mnw.png").bind(ProfitUtils.cropCount).setChildOf(this));
        stats.add(new Stat("https://static.wikia.nocookie.net/hypixel-skyblock/images/7/70/Enchanted_Carrot.png").bind(ProfitUtils.cropCount).setChildOf(this));
        stats.add(new Stat("https://static.wikia.nocookie.net/hypixel-skyblock/images/8/82/Enchanted_Baked_Potato.png").bind(ProfitUtils.cropCount).setChildOf(this));
        stats.add(new Stat("https://static.wikia.nocookie.net/hypixel-skyblock/images/7/7c/Enchanted_Hay_Bale").bind(ProfitUtils.cropCount).setChildOf(this));
        stats.add(new Stat("https://static.wikia.nocookie.net/hypixel-skyblock/images/e/e5/Enchanted_Red_Mushroom.png").bind(ProfitUtils.redMushroomCount).setChildOf(this));
        stats.add(new Stat("https://static.wikia.nocookie.net/hypixel-skyblock/images/6/6a/Enchanted_Brown_Mushroom.png").bind(ProfitUtils.brownMushroomCount).setChildOf(this));
        stats.add(new Stat("https://i.ibb.co/0YCYCnX/newton-nether-warts-hoe-tier-2.png").bind(ProfitUtils.counter).setChildOf(this));
        stats.add(new Stat("https://i.ibb.co/t3L9FHw/runtime.png").bind(ProfitUtils.runtime).setChildOf(this));
    }
}

class Stat extends UIBlock {
    private UIComponent displayString;
    public Stat(String iconURL) {
        this.setY(new SiblingConstraint())
            .setHeight(new ChildBasedSizeConstraint())
            .setWidth(new RelativeConstraint(1f))
            .setColor(new Color(36, 37, 39, 40));

        try {
            UIImage.ofURL(new URL(iconURL))
                .setX(new PixelConstraint(5))
                .setY(new CenterConstraint())
                .setHeight(new PixelConstraint(15))
                .setWidth(new PixelConstraint(15))
                .setChildOf(this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

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
