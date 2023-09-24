package com.github.may2beez.farmhelperv2;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "farmhelper", useMetadata=true)
public class FarmHelper {
    public static FarmHelperConfig config;
    public static final String VERSION = "%%VERSION%%";
    public static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        initializeFields();
        initializeListeners();
    }

    private void initializeListeners() {
        MinecraftForge.EVENT_BUS.register(GameStateHandler.getInstance());
        MinecraftForge.EVENT_BUS.register(MacroHandler.getInstance());
    }

    private void initializeFields() {
        config = new FarmHelperConfig();
    }
}
