package com.jelly.farmhelper.mixins.render;

import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class) // this shit make pojavlaucher user crashs!
public class MixinEffectRenderer {

    @Inject(method = "addBlockDestroyEffects", at = @At("HEAD"), cancellable = true)
    private void addBlockDestroyEffects(BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (MacroHandler.isMacroing | MacroHandler.randomizing) {
            ci.cancel();
        }
    }

    @Inject(method = "addBlockHitEffects(Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void addBlockDestroyEffects(BlockPos pos, EnumFacing side, CallbackInfo ci) {
        if (MacroHandler.isMacroing | MacroHandler.randomizing) {
            ci.cancel();
        }
    }
}
