package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.hud.TextHud;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.utils.StatusUtils;

import java.util.*;
import java.awt.Color;

public class StatusHUD extends TextHud {
    public StatusHUD() {
        super(true, 1f, 1f, 1, true, true, 1, 5, 5, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
    }

    @Switch(
        name = "Rainbow Text",category = "HUD", subcategory = "StatusHUD",
        description = "Rainbow text for the status HUD"
    )
    public static boolean rainbowStatusText = false;

    private String centerText(String text, float scale, boolean example) {
        float maxTextLength = getLineWidth(text, scale);
        float maxLongestLine = getWidth(scale, example);

        int difference = (int)(((maxLongestLine - maxTextLength) / 3.5f) / (2 * scale)) - 1; // :troll:
       return (difference > 0) ? new String(new char[difference]).replace("\0", " ") + text : text;
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {

        if (rainbowStatusText) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
        }

        if (example) {
            lines.add("Idling");
            lines.add("Farming for 25m 35s");
            lines.add("Staff ban in Last 15 Minutes: 999");
            lines.add("AutoCookie Fails: 0");
            lines.add("AutoPot Fails: 0");
            lines.set(0, (centerText("Idling", scale, true)));
        } else {
            lines.add(StatusUtils.status);

            if (!BanwaveChecker.staffBan.isEmpty() && FarmHelper.config.banwaveCheckerEnabled)
                lines.add(BanwaveChecker.staffBan);

            if (FarmHelper.config.autoCookie)
                lines.add(StatusUtils.cookieFail);

            if (FarmHelper.config.autoGodPot)
                lines.add(StatusUtils.potFail);

            if (FarmHelper.config.enableRemoteControl)
                lines.add(StatusUtils.connecting);

            lines.set(0, (centerText(StatusUtils.status, scale, example)));
        }
    }
}