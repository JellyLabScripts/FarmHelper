package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.gui.components.List;
import com.jelly.farmhelper.utils.KeyBindUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

public class MacroHandler {
    public static boolean macroEnabled;
    private static Macro selectedMacro = new CropMacro();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final Resync resync = new Resync();

    public void registerEvent() {
        MinecraftForge.EVENT_BUS.register(selectedMacro);
        MinecraftForge.EVENT_BUS.register(resync);
    }

    @SubscribeEvent
    public void OnKeyPress(InputEvent.KeyInputEvent event) {
        if (KeyBindUtils.customKeyBinds[1].isPressed()) {
            mc.thePlayer.closeScreen();
            macroEnabled = !macroEnabled;
            if (macroEnabled) {
                selectedMacro.enable();
            } else {
                selectedMacro.disable();
            }
        }
    }
}
