package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(value = KeyBinding.class, priority = Integer.MAX_VALUE)
public class MixinKeyBinding {
    @Shadow
    private int keyCode;

    @Shadow
    private int pressTime;

    @Shadow
    private boolean pressed;

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        if (!MacroHandler.getInstance().isMacroToggled() || MacroHandler.getInstance().isCurrentMacroPaused()) return;

        if (Arrays.stream(Minecraft.getMinecraft().gameSettings.keyBindsHotbar).anyMatch(keyBinding -> keyBinding.getKeyCode() == this.keyCode) || keyCode == Minecraft.getMinecraft().gameSettings.keyBindDrop.getKeyCode()) {
            cir.setReturnValue(false);
            pressTime = 0;
            pressed = false;
        }
    }
}
