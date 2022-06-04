package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.StatusUtils;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.UIText;
import gg.essential.elementa.components.Window;
import gg.essential.elementa.constraints.*;
import gg.essential.elementa.state.State;

import java.awt.*;

public class StatusGUI extends UIBlock {
    UIComponent displayString;
    UIComponent cookieFail;
    UIComponent potFail;
    boolean cookieFailShown;
    boolean potFailShown;

    public StatusGUI(Window window) {
        this.setColor(new Color(36, 37, 39, 85))
            .setX(new PixelConstraint(10, true))
            .setY(new PixelConstraint(10, true))
            .setWidth(new AdditiveConstraint(new ChildBasedMaxSizeConstraint(), new PixelConstraint(20)))
            .setHeight(new AdditiveConstraint(new ChildBasedSizeConstraint(5), new PixelConstraint(10)))
            .setChildOf(window);

        displayString = new UIText("Farming for 25m 35s", false)
            .bindText(StatusUtils.status)
            .setX(new CenterConstraint())
            .setY(new PixelConstraint(5))
            .setChildOf(this);

        cookieFailShown = true;
        cookieFail = new UIText("AutoCookie Fails: 0", false)
            .bindText(StatusUtils.cookieFail)
            .setX(new CenterConstraint())
            .setY(new AdditiveConstraint(new PixelConstraint(5), new SiblingConstraint()))
            .setChildOf(this);

        potFailShown = true;
        potFail = new UIText("AutoPot Fails: 0", false)
            .bindText(StatusUtils.potFail)
            .setX(new CenterConstraint())
            .setY(new AdditiveConstraint(new PixelConstraint(5), new SiblingConstraint()))
            .setChildOf(this);
    }

    public void updateFails() {
        if (MiscConfig.autoCookie && !cookieFailShown) {
            this.addChild(cookieFail);
            cookieFailShown = true;
        } else if (!MiscConfig.autoCookie && cookieFailShown) {
            this.removeChild(cookieFail);
            cookieFailShown = false;
        }

        if (MiscConfig.autoGodPot && !potFailShown) {
            this.addChild(potFail);
            potFailShown = true;
        } else if (!MiscConfig.autoGodPot && potFailShown) {
            this.removeChild(potFail);
            potFailShown = false;
        }
    }
}
