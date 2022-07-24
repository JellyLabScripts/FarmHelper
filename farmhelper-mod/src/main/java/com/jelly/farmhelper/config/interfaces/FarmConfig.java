package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;

public class FarmConfig {
    @Config()
    public static CropEnum cropType = CropEnum.SUGARCANE;
    @Config()
    public static FarmEnum farmType = FarmEnum.LAYERED;
}
