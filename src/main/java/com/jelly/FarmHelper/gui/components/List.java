package com.jelly.FarmHelper.gui.components;

import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIContainer;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.constraints.*;


import java.awt.*;

public class List extends UIBlock {
    private boolean isDragging = false;
    float xDragOffset;
    float yDragOffset;
    public List(XConstraint x, YConstraint y, String title, UIContainer content) {
        this.setX(x)
            .setY(y)
            .setWidth(new PixelConstraint(150))
            .setHeight(new ChildBasedRangeConstraint())
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

        new UIText(title, false)
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setTextScale(new PixelConstraint(1.1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(topBar);

        content.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setHeight(new ChildBasedRangeConstraint())
            .setWidth(new RelativeConstraint(1)).setChildOf(this);
    }
}
