package com.jelly.farmhelperv2.mixin.block;


import com.google.common.collect.Sets;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;


@Mixin(BlockRendererDispatcher.class)
public class MixinBlockRendererDispatcher {
    private Minecraft mc = Minecraft.getMinecraft();
    @Unique
    private static final Set<Block> cropBlocks = Sets.newHashSet(
            Blocks.cocoa,
            Blocks.reeds,
            Blocks.nether_wart,
            Blocks.wheat,
            Blocks.carrots,
            Blocks.potatoes,
            Blocks.pumpkin,
            Blocks.pumpkin_stem,
            Blocks.melon_block,
            Blocks.melon_stem,
            Blocks.brown_mushroom,
            Blocks.red_mushroom,
            Blocks.cactus
    );

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, WorldRenderer worldRendererIn, CallbackInfoReturnable<Boolean> cir) {
        if (FarmHelperConfig.performanceMode && (MacroHandler.getInstance().isMacroToggled())) {
            if (FarmHelperConfig.fastRender) {
                BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                if (playerPos.distanceSq(pos) > 25 && FarmHelperConfig.fastRender) {
                    cir.setReturnValue(false);
                }
            } else if (cropBlocks.contains(state.getBlock())) {
                cir.setReturnValue(false);
            }
        }
    }
}
