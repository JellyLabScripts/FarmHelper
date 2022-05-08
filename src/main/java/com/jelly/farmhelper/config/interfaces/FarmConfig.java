package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;
import com.jelly.farmhelper.config.FarmHelperConfig;

public class FarmConfig {
    public static CropEnum cropType;
    public static FarmEnum farmType;

    public static void update() {
        cropType = CropEnum.values()[((Long) FarmHelperConfig.get("cropType")).intValue()];
        farmType = FarmEnum.values()[((Long) FarmHelperConfig.get("farmType")).intValue()];
    }
}
