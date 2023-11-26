package com.jelly.farmhelperv2.mixin.render;

import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void addBlockDestroyEffects(BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (MacroHandler.getInstance().isMacroToggled()) {
            ci.cancel();
        }
    }
}
