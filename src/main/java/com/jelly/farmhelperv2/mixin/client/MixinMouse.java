package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Mouse.class, priority = Integer.MAX_VALUE, remap = false)
public class MixinMouse {
    @Inject(method = "getEventDWheel()I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void getEventDWheel(CallbackInfoReturnable<Integer> cir) {
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().isMacroToggled() && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "setCursorPosition", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/InputImplementation;setCursorPosition(II)V"), cancellable = true)
    private static void setCursorPosition(int new_x, int new_y, CallbackInfo ci) {
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().isMacroToggled() && !(Minecraft.getMinecraft().currentScreen instanceof GuiChat) && FarmHelperConfig.autoUngrabMouse) {
            ci.cancel();
        }
    }
}
