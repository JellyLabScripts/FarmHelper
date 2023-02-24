package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.config.ConfigHandler;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;

import java.awt.*;
import java.util.ArrayList;

public class FarmMenu extends UIContainer {
    private final UIComponent selector;
    private final ArrayList<UIComponent> cropList = new ArrayList<>();
    private final ArrayList<UIComponent> farmList = new ArrayList<>();

    public FarmMenu() {
        selector = new UIBlock((new Color(36, 37, 39)))
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(20)))
            .setChildOf(this);


        cropList.add(new ImageBox(new PixelConstraint(10), new PixelConstraint(10), 60f, "carrot.png", cropList, 0L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new PixelConstraint(10), 60f, "netherwart.png", cropList, 1L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "potato.png", cropList, 2L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), 60f, "wheat.png", cropList, 3L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "cane.png", cropList, 4L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), 60f, "melonandpumpkin.png", cropList, 5L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "cocoabeans.png", cropList, 6L, "cropType").setChildOf(selector));
        //cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 130f, ".png", cropList, 4L, "cropType").setChildOf(selector));

        farmList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "layered.png", farmList, 0L, "farmType").setChildOf(selector));
        farmList.add(new ImageBox(new PixelConstraint(80), new CramSiblingConstraint(10), 60f, "vertical.png", farmList, 1L, "farmType").setChildOf(selector));

        new UIText("Layered")
            .setX(new CenterConstraint())
            .setY(new PixelConstraint(5, true))
            .setTextScale(new PixelConstraint(0.8f))
            .setChildOf(farmList.get(0));

        new UIText("Vertical")
            .setX(new CenterConstraint())
            .setY(new PixelConstraint(5, true))
            .setTextScale(new PixelConstraint(0.8f))
            .setChildOf(farmList.get(1));
    }
}

class ImageBox extends UIBlock {
    private boolean selected;
    private final Color fillColor;
    private final ArrayList<UIComponent> components;
    private final String configName;
    public ImageBox(XConstraint x, YConstraint y, float width ,String ImageURL, ArrayList<UIComponent> components, Long value, String configName) {
        // Check if already marked
        selected = (long) ConfigHandler.get(configName) == value;
        this.components = components;
        this.configName = configName;
        fillColor = selected ? new Color(175, 36, 36) : new Color(30, 31, 32);
        this.setX(x)
            .setY(y)
            .setWidth(new PixelConstraint(width))
            .setHeight(new PixelConstraint(60))
            .setColor(fillColor)
            .onMouseClick((component, uiClickEvent) -> {
                // if (value == 1 && configName == "farmType") return null; // Temporary disable for vertical button
                components.forEach(c -> ((ImageBox) c).reset());
                selected = true;
                ConfigHandler.set(configName, value);
                AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(175, 36, 36)));
                this.animateTo(anim);
                return null;
            });

        new UIBlock((new Color(30, 31, 32)))
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setHeight(new RelativeConstraint(0.95f))
            .setWidth(new PixelConstraint(width - 3))
            .setChildOf(this);

        String path = "/assets/farmhelper/textures/gui/";

        UIImage.ofResource(path + ImageURL)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setHeight(new RelativeConstraint(0.5f))
            .setWidth(new AspectConstraint())
            .setChildOf(this);
    }


    public void reset() {
        selected = false;
        AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(30, 31, 32)));
        this.animateTo(anim);
    }
}
