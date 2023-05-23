package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.MacroEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;

public class FarmConfig {
    @Config()
    public static MacroEnum cropType = MacroEnum.CARROT_NW_WHEAT_POTATO;
    @Config()
    public static FarmEnum farmType = FarmEnum.LAYERED;
    @Config
    public static boolean warpBackToStart = false;
}
