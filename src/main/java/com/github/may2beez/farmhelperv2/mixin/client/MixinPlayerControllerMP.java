package com.github.may2beez.farmhelperv2.mixin.client;

import com.github.may2beez.farmhelperv2.event.ClickedBlockEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerControllerMP.class})
public class MixinPlayerControllerMP {
    @Inject(method = {"clickBlock"}, at = {@At(value = "HEAD")})
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        Block block = Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock();
        ClickedBlockEvent event = new ClickedBlockEvent(loc, face, block);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
