package com.github.may2beez.farmhelperv2.mixin.client;

import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method = "getEventDWheel()I", at = @At("RETURN"), cancellable = true, remap = false)
    private static void getEventDWheel(CallbackInfoReturnable<Integer> cir) {
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().isMacroToggled()) {
            cir.setReturnValue(0);
        }
    }
}
