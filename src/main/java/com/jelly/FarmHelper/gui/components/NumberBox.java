package com.jelly.FarmHelper.gui.components;

import com.jelly.FarmHelper.config.FarmHelperConfig;
import com.jelly.FarmHelper.utils.LogUtils;
import com.jelly.FarmHelper.utils.Utils;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.input.UITextInput;
import gg.essential.elementa.constraints.*;

import java.awt.*;
import java.util.Arrays;

public class NumberBox extends UIBlock {
    private UIComponent inputWrapper;
    private UITextInput numberInput;
    private Long numberValue;
    private Integer[] codes = {14, 28, 203, 205};
    private Integer digits;

    public NumberBox(String optionName, Integer digits, String configName) {
        // Get value from config
        this.numberValue = (Long) FarmHelperConfig.get(configName);
        this.digits = digits;

        this.setX(new PixelConstraint(0))
            .setY(new SiblingConstraint())
            .setWidth(new RelativeConstraint(1))
            .setHeight(new AdditiveConstraint(new ChildBasedRangeConstraint(), new PixelConstraint(20)))
            .setColor(new Color(36, 37, 39));

        new UIText(optionName, false)
            .setX(new PixelConstraint(10))
            .setY(new PixelConstraint(10))
            .setTextScale(new PixelConstraint(1f))
            .setColor(new Color(249, 249, 249))
            .setChildOf(this);

        inputWrapper = new UIBlock(new Color(30, 31, 32))
            .setX(new CenterConstraint())
            .setY(new AdditiveConstraint(new SiblingConstraint(), new PixelConstraint(5)))
            .setHeight(new PixelConstraint(15))
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1f), new PixelConstraint(20)))
            .setChildOf(this)
            .onMouseClick((component, uiClickEvent) -> {
                numberInput.grabWindowFocus();
                setDefaultText();
                return null;
            });

        numberInput = (UITextInput) new UITextInput(String.valueOf(numberValue))
            .setX(new PixelConstraint(2))
            .setY(new CenterConstraint())
            .setWidth(new SubtractiveConstraint(new RelativeConstraint(1), new PixelConstraint(6)))
            .setChildOf(inputWrapper)
            .onKeyType((component, character, integer) -> {
                // LogUtils.debugLog("Typed: " + character + ", " + integer);
                if ((Character.isDigit(character) || Arrays.asList(codes).contains(integer)) && numberInput.getText().length() <= this.digits) {
                    numberValue = !numberInput.getText().equals("") ? Long.parseLong(numberInput.getText()) : 0;
                    FarmHelperConfig.set(configName, numberValue);
                } else {
                    numberInput.setText(String.valueOf(numberValue));
                }
                return null;
            });
    }

    public void setDefaultText() {
        numberInput.setText(String.valueOf(numberValue));
    }
}
