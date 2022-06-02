package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.ProfitCalculatorConfig;
import com.jelly.farmhelper.world.GameState;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.components.Window;
import gg.essential.universal.UGraphics;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Render {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Window window;
    private final ProfitGUI profitGUI;
    public Render() {
        window = new Window();
        profitGUI = new ProfitGUI(window);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void render(RenderGameOverlayEvent event) {
        if (mc.theWorld != null && mc.thePlayer != null && ProfitCalculatorConfig.profitCalculator && event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            profitGUI.stats.forEach(e -> e.parent.removeChild(e));
            if (ProfitCalculatorConfig.runtime) profitGUI.addChild(profitGUI.stats.get(10));
            if (ProfitCalculatorConfig.counter) profitGUI.addChild(profitGUI.stats.get(9));
            if (ProfitCalculatorConfig.mushroomCount) {
                profitGUI.addChild(profitGUI.stats.get(8));
                profitGUI.addChild(profitGUI.stats.get(7));
            }
            if (ProfitCalculatorConfig.itemCount) {
                switch (FarmConfig.cropType) {
                    case NETHERWART:
                        profitGUI.addChild(profitGUI.stats.get(2));
                        break;
                    case CARROT:
                        profitGUI.addChild(profitGUI.stats.get(3));
                        break;
                    case POTATO:
                        profitGUI.addChild(profitGUI.stats.get(4));
                        break;
                    case WHEAT:
                        profitGUI.addChild(profitGUI.stats.get(5));
                        break;
                    case SUGARCANE:
                        profitGUI.addChild(profitGUI.stats.get(6));
                        break;
                }
            }
            if (ProfitCalculatorConfig.profitHour) profitGUI.addChild(profitGUI.stats.get(1));
            if (ProfitCalculatorConfig.totalProfit) profitGUI.addChild(profitGUI.stats.get(0));

            UGraphics.enableAlpha();
            window.draw();
            UGraphics.disableAlpha();
        }
    }
}