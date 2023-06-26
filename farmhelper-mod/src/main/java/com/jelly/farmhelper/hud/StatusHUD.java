package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.utils.StatusUtils;

import java.util.*;

public class StatusHUD extends BasicHud {
    protected transient List<String> lines = new ArrayList<>();
    protected transient float width = 0;
    public StatusHUD() {
        super(true, -5, -5, 5, true, true, 1, 0, 0, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 127));
        addLines(1.0f, true);
    }
    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        if (lines == null | lines.size() == 0) return;

        NanoVGHelper.INSTANCE.setupAndDraw(true, (vg) -> {
            float textX = position.getX() + 1 * scale;
            float textY = position.getY() + 2.5f * scale;

            addLines(scale, example);
    
            for (String line : lines) {
                drawLine(vg ,line, textX, textY, scale);
                textY += 3 * scale;
            }
        });
    }

    protected void drawLine(long vg, String text, float x, float y, float scale) {
        if (text != null) {
            NanoVGHelper.INSTANCE.drawText(vg, text, (x  + 2) + paddingX, y, -1, 2 * scale, Fonts.SEMIBOLD);
        }
    }

    protected float getLineWidth() {
        String longestLine = "";
        int maxLength = 0;

        for (String line: lines) {
            if(line.length() > maxLength) {
                longestLine = line;
                maxLength = line.length();
            }
        }
        return Platform.getGLPlatform().getStringWidth(longestLine);
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0;
        float currentWidth = (getLineWidth() / 3) * (scale / 1.5f) + (scale / 2) + (paddingX / 2);
        return Math.max(currentWidth, width);
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (lines == null) return 0;
        return (((4.5f - (lines.size() * 0.1f)) * scale) * lines.size() - scale);
    }

    private String centerText(String text, float scale, boolean example) {
        float maxTextLength = ((float) Platform.getGLPlatform().getStringWidth(text) / 3 * (scale / 1.5f)) + paddingX;
        float maxLongestLine = getWidth(scale, example);
        int difference = (int) ((int)((maxLongestLine - maxTextLength) / 2.5f) / (scale / 1.5));

       return (difference > 0) ? new String(new char[difference]).replace("\0", " ") + text : text;
    }

    public void addLines(float scale , boolean example) {
        lines.clear();

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