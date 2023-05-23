package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.enums.MacroEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIImage;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FarmMenu extends UIContainer {
    private final UIComponent selector;
    private static final ArrayList<UIComponent> cropList = new ArrayList<>();
    private final ArrayList<UIComponent> farmList = new ArrayList<>();

    public static final Color SELECTED_COLOR = new Color(175, 36, 36);
    public static final Color NOT_SELECTED_COLOR = new Color(30, 31, 32);
    public static final Color DISABLED_COLOR = new Color(1, 1, 1);

    public static final List<Integer> FORBIDDEN_SSHAPE_FARMS = Arrays.asList(2, 5, 6);
    public static final List<Integer> FORBIDDEN_VERTICAL_FARMS = Arrays.asList(1, 3, 4);

    public FarmMenu() {
        selector = new UIBlock((new Color(36, 37, 39)))
                .setWidth(new RelativeConstraint(1))
                .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(20)))
                .setChildOf(this);

        // Make sure to match CropEnum!
        cropList.add(new ImageBox(new PixelConstraint(10), new PixelConstraint(10), 60f, "carrot_nw_wheat_potato.png", cropList, 0L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new PixelConstraint(10), 60f, "cane.png", cropList, 1L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "pumpkin_melon.png", cropList, 2L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), 60f, "cactus.png", cropList, 3L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "cocoabeans.png", cropList, 4L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new SiblingConstraint(10), new CramSiblingConstraint(10), 60f, "brownmushroom.png", cropList, 5L, "cropType").setChildOf(selector));
        cropList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "brownmushroomwithtp.png", cropList, 6L, "cropType").setChildOf(selector));

        farmList.add(new ImageBox(new PixelConstraint(10), new SiblingConstraint(10), 60f, "layered.png", farmList, 0L, "farmType").setChildOf(selector));
        farmList.add(new ImageBox(new PixelConstraint(80), new CramSiblingConstraint(10), 60f, "vertical.png", farmList, 1L, "farmType").setChildOf(selector));

        new UIText("S Shape")
                .setX(new CenterConstraint())
                .setY(new PixelConstraint(5, true))
                .setTextScale(new PixelConstraint(0.8f))
                .setChildOf(farmList.get(0));

        new UIText("Vertical")
                .setX(new CenterConstraint())
                .setY(new PixelConstraint(5, true))
                .setTextScale(new PixelConstraint(0.8f))
                .setChildOf(farmList.get(1));

        new Toggle("Warp Back To Start", "warpBackToStart")
                .setX(new CenterConstraint())
                .setY(new PixelConstraint(25, true))
                .setChildOf(this);

        new Toggle("Rotate After Warp", "rotateAfterTp")
                .setX(new CenterConstraint())
                .setY(new PixelConstraint(5, true))
                .setChildOf(this);

        setDisable(FarmConfig.farmType.ordinal());
    }

    public static void setDisable(int farmType) {
        for (UIComponent c : cropList) {
            c.getChildren().get(0).setColor(NOT_SELECTED_COLOR);
        }

        // Layered
        if (farmType == 0L) {
            for (UIComponent c : cropList) {
                if (FORBIDDEN_SSHAPE_FARMS.contains((int) ((ImageBox) c).value)) {
                    c.getChildren().get(0).setColor(DISABLED_COLOR);
                }
            }
            if (FORBIDDEN_SSHAPE_FARMS.contains(FarmConfig.cropType.ordinal())) {
                FarmConfig.cropType = MacroEnum.CARROT_NW_WHEAT_POTATO;

                // need to add condition since it may throw null pointer if called during first time construction
                if (Minecraft.getMinecraft().thePlayer != null) {
                    cropList.get(0).setColor(SELECTED_COLOR);
                    ((ImageBox) cropList.get(0)).onMouseClick();
                }

            }
        // Vertical
        } else if (farmType == 1L) {
            for (UIComponent c : cropList) {
                if (FORBIDDEN_VERTICAL_FARMS.contains((int) ((ImageBox) c).value)) {
                    c.getChildren().get(0).setColor(DISABLED_COLOR);
                }
            }
            if (FORBIDDEN_VERTICAL_FARMS.contains(FarmConfig.cropType.ordinal())) {
                FarmConfig.cropType = MacroEnum.CARROT_NW_WHEAT_POTATO;

                // need to add condition since it may throw null pointer if called during first time construction
                if (Minecraft.getMinecraft().thePlayer != null) {
                    cropList.get(0).setColor(SELECTED_COLOR);
                    ((ImageBox) cropList.get(0)).onMouseClick();
                }
            }
        }
    }
}

class ImageBox extends UIBlock {
    private boolean selected;
    private final Color fillColor;
    private final ArrayList<UIComponent> components;
    private final String configName;

    public final long value;


    public ImageBox(XConstraint x, YConstraint y, float width, String ImageURL, ArrayList<UIComponent> components, Long value, String configName) {
        // Check if already marked
        selected = (long) ConfigHandler.get(configName) == value;
        this.components = components;
        this.configName = configName;
        this.value = value;

        fillColor = selected ? FarmMenu.SELECTED_COLOR : FarmMenu.NOT_SELECTED_COLOR;

        this.setX(x)
                .setY(y)
                .setWidth(new PixelConstraint(width))
                .setHeight(new PixelConstraint(60))
                .setColor(fillColor)
                .onMouseClick((component, uiClickEvent) -> {
                    if (this.getChildren().get(0).getColor().equals(FarmMenu.DISABLED_COLOR))
                        return null;
                    onMouseClick();
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

    public void onMouseClick() {

        components.forEach(c -> ((ImageBox) c).reset());
        selected = true;
        ConfigHandler.set(configName, value);

        AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 2f, new ConstantColorConstraint(FarmMenu.SELECTED_COLOR));
        this.animateTo(anim);

        if (configName.equals("farmType")) {
            FarmMenu.setDisable(Math.toIntExact(value));
        }
    }


    public void reset() {
        selected = false;
        AnimatingConstraints anim = this.makeAnimation().setColorAnimation(Animations.OUT_EXP, 0.5f, new ConstantColorConstraint(FarmMenu.NOT_SELECTED_COLOR));
        this.animateTo(anim);
    }
}
