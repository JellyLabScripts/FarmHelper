package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;

import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.utils.Clock;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProfitCalculatorHUD extends BasicHud {
    protected transient LinkedHashMap<String, String> lines = new LinkedHashMap<String, String>();
    protected transient final Clock updateClock = new Clock();
    public ProfitCalculatorHUD() {
        super(true, 0, 0, 0.8f, true, true, 3, 4, 6, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 240));
        addLines();
    }
    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        if (lines == null || lines.size() == 0) return;
        NanoVGHelper.INSTANCE.setupAndDraw(true, (vg) -> {
            float textY = position.getY() + 2 * scale;
            addLines();

            for (HashMap.Entry<String, String> line : lines.entrySet()) {
                drawLine(vg ,line.getKey(), line.getValue(), position.getX(), textY, scale);
                textY += 5 * scale;
            }
        });
    }

    protected void drawLine(long vg, String text, String iconPath, float x, float y, float scale) {
        float iconWidth = 4 * scale;
        float iconHeight = 4 * scale;
        if (iconPath != null) {
            NanoVGHelper.INSTANCE.drawImage(vg, iconPath, x + 4, y, iconWidth, iconHeight);
        }
        // draw text
        if (text != null) {
            NanoVGHelper.INSTANCE.drawText(vg, text, (x + iconWidth + 2) + 4, (y + (iconHeight / 2)), -1, 2 * scale, Fonts.BOLD);
        }
    }

    protected float getLineWidth(String line) {
        return Platform.getGLPlatform().getStringWidth(line);
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0;
        float width = 0;
        for (HashMap.Entry<String, String> line : lines.entrySet()) {
            width = Math.max(width, getLineWidth(line.getKey()));
        }
        return width + paddingX;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (lines == null) return 0;
        return (float) ((lines.size() * 3.5 + ((lines.size() < 5) ? -5 : (lines.size() > 6) ? + 3 : 0)) * scale) + paddingY;
    }

    public void addLines() {
        lines.clear();

        lines.put(ProfitCalculator.profit, "/assets/farmhelper/textures/gui/profit.png");
        lines.put(ProfitCalculator.profitHr, "/assets/farmhelper/textures/gui/profithr.png");
        lines.put(ProfitCalculator.blocksPerSecond, "/assets/farmhelper/textures/gui/bps.png");
        lines.put(ProfitCalculator.runtime, "/assets/farmhelper/textures/gui/runtime.png");
        for (Map.Entry<String, ProfitCalculator.BazaarItem> entry : ProfitCalculator.ListCropsToShow.entrySet()) {
            if (lines.get(entry.getValue().currentAmount + "") == null) lines.put(entry.getValue().currentAmount + "", entry.getValue().imageURL);
            else lines.put(entry.getValue().currentAmount + " ", entry.getValue().imageURL); // this is very tricky...
        }
    }
}