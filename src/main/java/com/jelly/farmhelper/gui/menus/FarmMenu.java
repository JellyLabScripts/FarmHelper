package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.config.FarmHelperConfig;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class FarmMenu extends UIContainer {
    private UIComponent selector;
    private ArrayList<UIComponent> cropList = new ArrayList<>();
    private ArrayList<UIComponent> farmList = new ArrayList<>();

    public FarmMenu() {
        selector = new UIBlock((new Color(36, 37, 39)))
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(20)))
            .setChildOf(this);


        cropList.add(new ImageBox(new PixelConstraint(10), new PixelConstraint(10), "https://static.wikia.nocookie.net/minecraft/images/6/63/Carrot_Updated.png", cropList, 0L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new PixelConstraint(10), "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c0/Nether_Wart_%28item%29_JE1.png", cropList, 1L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c1/Potato_JE3_BE2.png", cropList, 2L, "cropType").setChildOf(selector));
        cropList.add( new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/75/Wheat_JE2_BE2.png", cropList, 3L, "cropType").setChildOf(selector));

        farmList.add(new ImageBox(new PixelConstraint(10), new CramSiblingConstraint(10), "https://i.ibb.co/6nFDfRt/layered.png", farmList, 0L, "farmType").setChildOf(selector));
        farmList.add(new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), "https://i.ibb.co/hLG9g3X/vertical.png", farmList, 1L, "farmType").setChildOf(selector));

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
    private Color fillColor;
    private ArrayList<UIComponent> components;
    private String configName;
    public ImageBox(XConstraint x, YConstraint y, String ImageURL, ArrayList<UIComponent> components, Long value, String configName) {
        // Check if already marked
        selected = (long) FarmHelperConfig.get(configName) == value;
        this.components = components;
        this.configName = configName;
        fillColor = selected ? new Color(175, 36, 36) : new Color(30, 31, 32);
        this.setX(x)
            .setY(y)
            .setWidth(new PixelConstraint(60))
            .setHeight(new PixelConstraint(60))
            .setColor(fillColor)
            .onMouseClick((component, uiClickEvent) -> {
                components.forEach(c -> ((ImageBox) c).reset());
                selected = true;
                FarmHelperConfig.set(configName, value);
                AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(175, 36, 36)));
                this.animateTo(anim);
                return null;
            });

        new UIBlock((new Color(30, 31, 32)))
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setHeight(new RelativeConstraint(0.95f))
            .setWidth(new RelativeConstraint(0.95f))
            .setChildOf(this);

        try {
            UIImage.ofURL(new URL(ImageURL))
                .setX(new CenterConstraint())
                .setY(new CenterConstraint())
                .setHeight(new RelativeConstraint(0.5f))
                .setWidth(new RelativeConstraint(0.5f))
                .setChildOf(this);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    public void reset() {
        selected = false;
        AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(new Color(30, 31, 32)));
        this.animateTo(anim);
    }
}
