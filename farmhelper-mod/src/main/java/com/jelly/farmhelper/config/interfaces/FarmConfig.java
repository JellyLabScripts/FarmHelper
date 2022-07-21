package com.jelly.farmhelper.config.interfaces;

import com.jelly.farmhelper.config.ConfigHandler;
import com.jelly.farmhelper.config.annotations.Config;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;

public class FarmConfig {
    @Config(key = "cropType")
    public static CropEnum cropType;
    @Config(key = "farmType")
    public static FarmEnum farmType;
}
