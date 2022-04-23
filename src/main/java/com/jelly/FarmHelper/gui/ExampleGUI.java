package com.jelly.FarmHelper.gui;

import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.input.UITextInput;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;
import gg.essential.elementa.constraints.animation.ColorAnimationComponent;
import gg.essential.elementa.effects.ScissorEffect;
import javafx.util.Pair;
import javafx.util.converter.PercentageStringConverter;

import java.awt.*;

public class ExampleGUI extends WindowScreen {
    public ExampleGUI() {
        UIComponent createNoteButton = new UIBlock(new Color(207, 207, 196))
            .setX(new PixelConstraint(2))
            .setY(new PixelConstraint(2))
            .setWidth(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(4)))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(), new PixelConstraint(4)))
            .onMouseClick((component, uiClickEvent) -> {
                new StickyNote().setChildOf(getWindow());
                return null;
            })
            .onMouseEnter(component -> {
                AnimatingConstraints anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 5f, new ConstantColorConstraint(new Color(120, 120, 100)));
                component.animateTo(anim);
                return null;
            })
            .onMouseLeave(component -> {
                AnimatingConstraints anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 5f, new ConstantColorConstraint(new Color(207, 207, 196)));
                component.animateTo(anim);
                return null;
            })
            .setChildOf(getWindow());
        new UIText("Create notes!", false)
            .setX(new PixelConstraint(2))
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(2))
            .setColor(Color.GREEN.darker())
            .setChildOf(createNoteButton);
    }
}

class StickyNote extends UIBlock {
    private boolean isDragging = false;
    private Pair<Float, Float> dragOffset = new Pair<>(0f, 0f);
    private UITextInput textArea;
    public StickyNote() {
        this.setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setWidth(new PixelConstraint(150))
            .setHeight(new PixelConstraint(150))
            .onMouseClick((component, uiClickEvent) -> {
                parent.removeChild(this);
                parent.addChild(this);
                return null;
            });
        UIComponent topBar = new UIBlock(Color.YELLOW)
            .setX(new PixelConstraint(1))
            .setY(new PixelConstraint(1))
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1f), new PixelConstraint(2)))
            .setHeight(new PixelConstraint(24))
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
                Float absoluteX = aFloat + getLeft();
                Float absoluteY = aFloat2 + getTop();

                Float deltaX = absoluteX - dragOffset.getKey();
                Float deltaY = absoluteY - dragOffset.getValue();

                dragOffset = new Pair<>(absoluteX, absoluteY);
                Float newX = this.getLeft() + deltaX;
                Float newY = this.getTop() + deltaY;
                this.setX(new PixelConstraint(newX));
                this.setY(new PixelConstraint(newY));
                return null;
            })
            .setChildOf(this);
        new UIText("X", false).setX(new PixelConstraint(4, true))
            .setY(new CenterConstraint())
            .setColor(Color.BLACK)
            .setTextScale(new PixelConstraint(2))
            .onMouseEnter(component -> {
                AnimatingConstraints anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 5f, new ConstantColorConstraint(Color.RED));
                component.animateTo(anim);
                return null;
            })
            .onMouseLeave(component -> {
                AnimatingConstraints anim = component.makeAnimation().setColorAnimation(Animations.OUT_EXP, 5f, new ConstantColorConstraint(Color.BLACK));
                component.animateTo(anim);
                return null;
            })
            .onMouseClick((component, uiClickEvent) -> {
                this.parent.removeChild(this);
                uiClickEvent.stopPropagation();
                return null;
            })
            .setChildOf(topBar);
        UIComponent textHolder = new UIBlock(new Color(80, 80, 80))
            .setX(new PixelConstraint(1))
            .setY(new SiblingConstraint())
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1f), new PixelConstraint(2)))
            .setHeight(new FillConstraint())
            .setChildOf(this)
            .enableEffect(new ScissorEffect());

        textArea = (UITextInput) new UITextInput("Enter your note...")
            .setX(new PixelConstraint(2))
            .setY(new PixelConstraint(2))
            .setHeight(new SubtractiveConstraint(new FillConstraint(), new PixelConstraint(2)))
            .onMouseClick((component, uiClickEvent) -> {
                grabWindowFocus();
                return null;
            })
            .setChildOf(textHolder);
    }
}
