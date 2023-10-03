package com.github.may2beez.farmhelperv2.config.page;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;

public class AutoSellNPCItemsPage {
    @Switch(name = "Mysterious Crop")
    public static boolean autoSellMysteriousCrop = true;
    @Switch(name = "Runes")
    public static boolean autoSellRunes = true;

    @Switch(name = "Velvet Top Hat")
    public static boolean autoSellVelvetTopHat = true;
    @Switch(name = "Cashmere Jacket")
    public static boolean autoSellCashmereJacket = true;
    @Switch(name = "Satin Trousers")
    public static boolean autoSellSatinTrousers = true;
    @Switch(name = "Oxford Shoes")
    public static boolean autoSellOxfordShoes = true;

    @Text(
            name = "Custom Items",
            description = "Add custom items to AutoSell here. Use | to split the messages.",
            placeholder = "Custom items to auto sell. Use | to split the messages.",
            size = 2
    )
    public static String autoSellCustomItems = "";
}
