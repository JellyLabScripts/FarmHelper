package com.jelly.farmhelper.gui.menus;

import com.jelly.farmhelper.gui.components.Toggle;
import gg.essential.elementa.components.UIContainer;

public class ProfitCalculatorMenu extends UIContainer {
    public ProfitCalculatorMenu() {
        new Toggle("Enabled", "profitCalculator").setChildOf(this);
        new Toggle("Total Profit", "totalProfit").setChildOf(this);
        new Toggle("Profit per Hour", "profitHour").setChildOf(this);
        new Toggle("Item Count", "itemCount").setChildOf(this);
        new Toggle("Count RNG to $/Hr", "countRNGtoHourly").setChildOf(this);
        new Toggle("Blocks per second", "blocksPerSecond").setChildOf(this);
        new Toggle("Runtime", "runtime").setChildOf(this);
    }
}
