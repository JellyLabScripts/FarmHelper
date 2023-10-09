package com.github.may2beez.farmhelperv2.mixin.client;

import com.github.may2beez.farmhelperv2.FarmHelper;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class MixinSoundManager {
    @Inject(method = "getNormalizedVolume", at = @At("RETURN"), cancellable = true)
    private void getNormalizedVolume(ISound sound, SoundPoolEntry soundPoolEntry, SoundCategory category, CallbackInfoReturnable<Float> ci) {
        if (MacroHandler.getInstance().isMacroToggled() && FarmHelperConfig.muteTheGame && !Failsafe.getInstance().isEmergency()) {
            ci.setReturnValue(0.0f);
        }
    }
}
