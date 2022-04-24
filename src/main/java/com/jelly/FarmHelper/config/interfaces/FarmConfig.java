package com.jelly.FarmHelper.config.interfaces;

import com.jelly.FarmHelper.config.enums.CropEnum;
import com.jelly.FarmHelper.config.enums.FarmEnum;
import com.jelly.FarmHelper.config.FarmHelperConfig;

public class FarmConfig {
    public static CropEnum cropType;
    public static FarmEnum farmType;

    public static void update() {
        cropType = CropEnum.values()[((Long) FarmHelperConfig.get("cropType")).intValue()];
        farmType = FarmEnum.values()[((Long) FarmHelperConfig.get("farmType")).intValue()];
    }
}
