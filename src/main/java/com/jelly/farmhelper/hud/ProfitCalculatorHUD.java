package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.utils.LocationUtils;
import net.minecraft.util.Tuple;

import java.awt.*;
import java.util.*;

public class ProfitCalculatorHUD extends BasicHud {
    protected transient ArrayList<Tuple<String, String>> lines = new ArrayList<>();
    protected transient float width = 0;
    public ProfitCalculatorHUD() {
        super(true, 1f, 1f, 3.5f, true, true, 1, 0, 0, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 240));
        addLines();
    }
    @Switch(
            name = "Rainbow Text",category = "HUD", subcategory = "ProfitCalculatorHUD",
            description = "Rainbow text for the status HUD"
    )
    public static boolean rainbowProfitText = false;
    @Switch(
            name = "Don't show Profit Calculator HUD while on the island", category = "HUD", subcategory = "ProfitCalculatorHUD",
            description = "Rainbow text for the status HUD"
    )
    public static boolean dontShowProfitWhileOnTheIsland = true;

    @Exclude
    protected OneColor color = new OneColor(0, 0, 0, 255);

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        if (lines == null || lines.isEmpty()) return;
        if (LocationUtils.currentIsland == LocationUtils.Island.PRIVATE_ISLAND) {
            if (dontShowProfitWhileOnTheIsland)
                return;
        } else if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;

        if (rainbowProfitText) {
            Color chroma = Color.getHSBColor((float) ((System.currentTimeMillis() / 10) % 500) / 500, 1, 1);
            this.color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
        } else {
            Color chroma = Color.WHITE;
            color.setFromOneColor(new OneColor(chroma.getRed(), chroma.getGreen(), chroma.getBlue(), 255));
        }

        NanoVGHelper.INSTANCE.setupAndDraw(true, (vg) -> {
            float textX = position.getX() + 1 * scale;
            float textY = position.getY() + 1 * scale;

            addLines();

            for (Tuple<String, String> line : lines) {
                drawLine(vg ,line.getFirst(), line.getSecond(), textX, textY, scale);
                textY += 5 * scale;
            }
        });
    }

    @Override
    protected boolean shouldShow() {
        return (LocationUtils.currentIsland == LocationUtils.Island.PRIVATE_ISLAND || LocationUtils.currentIsland == LocationUtils.Island.GARDEN);
    }

    protected void drawLine(long vg, String text, String iconPath, float x, float y, float scale) {
        float iconWidth = 4 * scale;
        float iconHeight = 4 * scale;
        if (iconPath != null) {
            NanoVGHelper.INSTANCE.drawImage(vg, iconPath, x + scale - 0.9f * scale, y, iconWidth, iconHeight);
        }
        // draw text
        if (text != null) {
            NanoVGHelper.INSTANCE.drawText(vg, text, (x + iconWidth + 2 * scale - 0.9f * scale), (y + (iconHeight / 2)), rainbowProfitText ? this.color.getRGB() : -1, 2 * scale, Fonts.SEMIBOLD); // made with pain by tama & yuro <3
        }
    }
    protected float getLineWidth() {
        String longestLine = "";
        int maxLength = 0;

        for (Tuple<String, String> entry : lines) {
            String lineText = entry.getFirst();
            if(lineText.length() > maxLength) {
                longestLine = lineText;
                maxLength = lineText.length();
            }
        }
        return Platform.getGLPlatform().getStringWidth(longestLine);
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0;
        float currentWidth = (getLineWidth() / 2 * (scale / 1.5f)) + (scale * lines.size()) + paddingX / 2;
        return Math.max(width, currentWidth);
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (lines == null) return 0;
        return (((6f - (lines.size() * 0.1f)) * scale) * lines.size() - scale + paddingY / 2);
    }

    public void addLines() {
        lines.clear();

        lines.add(new Tuple<>(ProfitCalculator.profit, "/assets/farmhelper/textures/gui/profit.png"));
        lines.add(new Tuple<>(ProfitCalculator.profitHr, "/assets/farmhelper/textures/gui/profithr.png"));
        lines.add(new Tuple<>(ProfitCalculator.blocksPerSecond, "/assets/farmhelper/textures/gui/bps.png"));
        lines.add(new Tuple<>(ProfitCalculator.runtime, "/assets/farmhelper/textures/gui/runtime.png"));
        HashMap<String, ProfitCalculator.BazaarItem> linesCopy = new HashMap<>(ProfitCalculator.ListCropsToShow);
        LinkedHashMap<String, ProfitCalculator.BazaarItem> sorted = new LinkedHashMap<>();
        linesCopy.entrySet()
                .stream()
                .sorted(Comparator.comparingDouble(e -> -e.getValue().currentAmount))
                .forEachOrdered(x -> sorted.put(x.getKey(), x.getValue()));
        for (Map.Entry<String, ProfitCalculator.BazaarItem> entry : sorted.entrySet()) {
            lines.add(new Tuple<>(String.format("%,.2f", entry.getValue().currentAmount), entry.getValue().imageURL));
        }
    }
}