package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "getEventDWheel()I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void getEventDWheel(CallbackInfoReturnable<Integer> cir) {
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().isMacroToggled() && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            cir.setReturnValue(0);
        }
    }
}
