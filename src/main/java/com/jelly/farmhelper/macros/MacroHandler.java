package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.utils.InventoryUtils;
import com.jelly.farmhelper.utils.KeyBindUtils;
import com.jelly.farmhelper.utils.LogUtils;
import com.jelly.farmhelper.utils.UngrabUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.concurrent.TimeUnit;

public class MacroHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean macroEnabled;
    public static Macros currentMacro = Macros.CROP_MACRO;
    public static int startCounter = 0;
    public static long startTime = 0;

    enum Macros {
        CROP_MACRO
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
            mc.thePlayer.closeScreen();
            macroEnabled = !macroEnabled;
            if (macroEnabled) {
                startTime = System.currentTimeMillis();
                startCounter = InventoryUtils.getCounter();
                LogUtils.scriptLog("Starting script");
                UngrabUtils.ungrabMouse();
                enableCurrentMacro();
            } else {
                LogUtils.scriptLog("Stopping script");
                UngrabUtils.regrabMouse();
                disableCurrentMacro();
            }
        }
    }

    public static void enableCurrentMacro() {
        if (macroEnabled && !isMacroEnabled()) {
            switch (currentMacro) {
                case CROP_MACRO:
                    CropMacro.enable();
            }
        }
    }

    public static void disableCurrentMacro() {
        if (isMacroEnabled()) {
            switch (currentMacro) {
                case CROP_MACRO:
                    CropMacro.disable();
            }
        }
    }

    public static boolean isMacroEnabled() {
        switch (currentMacro) {
            case CROP_MACRO:
                return CropMacro.isEnabled();
        }
        return true;
    }
}