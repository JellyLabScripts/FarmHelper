package com.jelly.farmhelperv2.config.page;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;

public class AutoSellNPCItemsPage {
    @Switch(name = "Runes")
    public static boolean autoSellRunes = true;

    @Switch(name = "Dead Bush")
    public static boolean autoSellDeadBush = true;

    @Switch(name = "Iron Hoe")
    public static boolean autoSellIronHoe = true;

    @Text(
            name = "Custom Items",
            description = "Add custom items to AutoSell here. Use | to split the messages.",
            placeholder = "Custom items to auto sell. Use | to split the messages.",
            size = 2
    )
    public static String autoSellCustomItems = "";
}
