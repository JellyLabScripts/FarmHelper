package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.gui.components.List;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.UngrabUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean macroEnabled;
    public static Macros currentMacro = Macros.CROP_MACRO;

    enum Macros {
        CROP_MACRO,
        NONE
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
            mc.thePlayer.closeScreen();
            macroEnabled = !macroEnabled;
            if (macroEnabled) {
                //UngrabUtils.ungrabMouse();
                enableCurrentMacro();
            } else {
                //UngrabUtils.regrabMouse();
                disableCurrentMacro();
            }
        }
    }

    public static void enableCurrentMacro() {
        if (macroEnabled) {
            switch (currentMacro) {
                case CROP_MACRO:
                    CropMacro.enable();
                case NONE:
                    LogUtils.debugLog("No Macro Selected");
            }
        }
    }

    public static void disableCurrentMacro() {
        if (CropMacro.isEnabled()) CropMacro.disable();
    }

    public static boolean isMacroEnabled() {
        switch (currentMacro) {
            case CROP_MACRO:
                return CropMacro.isEnabled();
        }
        return true;
    }
}