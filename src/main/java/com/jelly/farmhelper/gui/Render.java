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
//    private final Minecraft mc = Minecraft.getMinecraft();
//    private final Window window;
//    private final ProfitGUI profitGUI;
//    public Render() {
//        window = new Window();
//        profitGUI = new ProfitGUI(window);
//    }
//
//    @SideOnly(Side.CLIENT)
//    @SubscribeEvent
//    public void render(RenderGameOverlayEvent event) {
//        if (mc.theWorld != null && mc.thePlayer != null && ProfitCalculatorConfig.profitCalculator && event.type == RenderGameOverlayEvent.ElementType.TEXT) {
//            profitGUI.stats.forEach(UIComponent::hide);
//            if (ProfitCalculatorConfig.runtime) profitGUI.stats.get(10).unhide(true);
//            if (ProfitCalculatorConfig.counter) profitGUI.stats.get(9).unhide(true);
//            if (ProfitCalculatorConfig.mushroomCount) {
//                profitGUI.stats.get(8).unhide(true);
//                profitGUI.stats.get(7).unhide(true);
//            }
//            if (ProfitCalculatorConfig.itemCount) {
//                switch (FarmConfig.cropType) {
//                    case NETHERWART:
//                        profitGUI.stats.get(2).unhide(true);
//                        break;
//                    case CARROT:
//                        profitGUI.stats.get(3).unhide(true);
//                        break;
//                    case POTATO:
//                        profitGUI.stats.get(4).unhide(true);
//                        break;
//                    case WHEAT:
//                        profitGUI.stats.get(5).unhide(true);
//                        break;
//                    case SUGARCANE:
//                        profitGUI.stats.get(6).unhide(true);
//                        break;
//                }
//            }
//            if (ProfitCalculatorConfig.profitHour) profitGUI.stats.get(1).unhide(true);
//            if (ProfitCalculatorConfig.totalProfit) profitGUI.stats.get(0).unhide(true);
//
//            UGraphics.enableAlpha();
//            window.draw();
//            UGraphics.disableAlpha();
//        }
//    }
}