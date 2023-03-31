package com.jelly.farmhelper.gui;

import com.jelly.farmhelper.config.interfaces.ProfitCalculatorConfig;
import com.jelly.farmhelper.config.interfaces.SchedulerConfig;
import com.jelly.farmhelper.features.ProfitCalculator;
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
    private ProfitGUI profitGUI;
    private final StatusGUI statusGUI;

    public Render() {
        window = new Window();
        profitGUI = new ProfitGUI(window);
        statusGUI = new StatusGUI(window);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void render(RenderGameOverlayEvent event) {
        if (mc.theWorld != null && mc.thePlayer != null && (ProfitCalculatorConfig.profitCalculator || SchedulerConfig.statusGUI) && event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            window.removeChild(statusGUI);
            if (SchedulerConfig.statusGUI) {
                window.addChild(statusGUI);
                statusGUI.updateFails();
            }

            window.removeChild(profitGUI);
            if (ProfitCalculatorConfig.profitCalculator) {
                window.addChild(profitGUI);
                profitGUI.getChildren().forEach(e -> {
                    if (e.getComponentName().equals("DON'T TOUCH THIS")) return;
                    e.parent.removeChild(e);
                });
//                ProfitCalculatorNew.dropToShow.values().forEach(e -> e.stat.parent.removeChild(e.stat));
                if (ProfitCalculatorConfig.totalProfit) profitGUI.addChild(profitGUI.stats.get(0));
                if (ProfitCalculatorConfig.profitHour) profitGUI.addChild(profitGUI.stats.get(1));
                if (ProfitCalculatorConfig.itemCount) {
//                    for (int i = 2; i < profitGUI.stats.size() - 2; i++) {
//                        profitGUI.addChild(profitGUI.stats.get(i));
//                    }
                    for (ProfitCalculator.GuiItem item : ProfitCalculator.dropToShow.values()) {
                        profitGUI.addChild(item.stat);
                    }
//                    switch (FarmConfig.cropType) {
//                        case NETHERWART:
//                            profitGUI.addChild(profitGUI.stats.get(2));
//                            break;
//                        case CARROT:
//                            profitGUI.addChild(profitGUI.stats.get(3));
//                            break;
//                        case POTATO:
//                            profitGUI.addChild(profitGUI.stats.get(4));
//                            break;
//                        case WHEAT:
//                            profitGUI.addChild(profitGUI.stats.get(5));
//                            break;
//                        case SUGARCANE:
//                            profitGUI.addChild(profitGUI.stats.get(6));
//                            break;
//                        case COCOA_BEANS:
//                            profitGUI.addChild(profitGUI.stats.get(7));
//                            break;
//                        case MELON:
//                            profitGUI.addChild(profitGUI.stats.get(8));
//                            break;
//                        case PUMPKIN:
//                            profitGUI.addChild(profitGUI.stats.get(9));
//                            break;
//                        case CACTUS:
//                            profitGUI.addChild(profitGUI.stats.get(10));
//                            break;
//                    }
                }
                if (ProfitCalculatorConfig.runtime) profitGUI.addChild(profitGUI.stats.get(profitGUI.stats.size() - 1));
            }

            UGraphics.enableAlpha();
            window.draw();
            UGraphics.disableAlpha();
        }
    }
}