package com.jelly.farmhelper.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.TextRenderer;
import com.jelly.farmhelper.features.ProfitCalculator;
import com.jelly.farmhelper.utils.LocationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;

import java.util.*;

// made with pain by tama & yuro <3 (and even more pain fixing it by May2Bee)
public class ProfitCalculatorHUD extends BasicHud {
    protected transient ArrayList<Tuple<String, String>> lines = new ArrayList<>();
    private final float iconWidth = 12 * scale;
    private final float iconHeight = 12 * scale;
    public ProfitCalculatorHUD() {
        super(true, 1f, 1f, 1, true, true, 2.5f, 2.5f, 2.5f, new OneColor(0, 0, 0, 150), false, 2, new OneColor(0, 0, 0, 240));
        addLines();
    }

    @cc.polyfrost.oneconfig.config.annotations.Color(
            name = "Text Color"
    )
    protected OneColor color = new OneColor(255, 255, 255);

    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    protected int textType = 0;

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        if (lines == null || lines.isEmpty()) return;
        if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN && !example) return;

        float textX = position.getX() + 1 * scale;
        addLines();
        NanoVGHelper.INSTANCE.setupAndDraw(true, (vg) -> {
            float textY = position.getY() + 1 * scale;
            for (Tuple<String, String> line : lines) {
                drawImage(vg , line.getSecond(), textX, textY, scale);
                textY += 15 * scale;
            }
        });
        float textY = position.getY() + 1 * scale;
        for (Tuple<String, String> line : lines) {
            drawLine(line.getFirst(), textX, textY, scale);
            textY += 15 * scale;
        }
    }

    @Override
    protected boolean shouldShow() {
        return LocationUtils.currentIsland == LocationUtils.Island.GARDEN;
    }

    protected void drawImage(long vg, String iconPath, float x, float y, float scale) {
        if (iconPath != null) {
            NanoVGHelper.INSTANCE.drawImage(vg, iconPath, x + paddingX * scale, y + paddingY * scale, iconWidth * scale, iconHeight * scale, this.getClass());
        }
    }

    protected void drawLine(String text, float x, float y, float scale) {
        if (text != null) {
            TextRenderer.drawScaledString(text, x + iconWidth * scale + (3.5f * scale) + paddingX * scale, y + paddingY * scale + iconHeight * scale / 2 - getLineHeight(scale) / 2.5f, color.getRGB(), TextRenderer.TextType.toType(textType), scale);
        }
    }
    protected float getLineWidth(String line, float scale) {
        return Platform.getGLPlatform().getStringWidth(line) * scale;
    }

    protected float getLineHeight(float scale) {
        return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * scale;
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0;
        float width = 0;
        for (Tuple<String, String> line : lines) {
            width = Math.max(width, getLineWidth(line.getFirst(), scale));
        }
        return width + iconWidth * scale + 10 * scale + 1 * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        if (lines == null) return 0;
        return lines.size() * 15 * scale;
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