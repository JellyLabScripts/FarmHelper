package com.jelly.farmhelperv2.mixin.block;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRendererDispatcher.class)
public class MixinBlockRendererDispatcher {
    private Minecraft mc = Minecraft.getMinecraft();
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer worldRendererIn, CallbackInfoReturnable<Boolean> cir) {
        if (FarmHelperConfig.performanceMode && (MacroHandler.getInstance().isMacroToggled())) {
            BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            if (playerPos.distanceSq(pos) > 25) {
                cir.setReturnValue(false);
            }
        }
    }
}
