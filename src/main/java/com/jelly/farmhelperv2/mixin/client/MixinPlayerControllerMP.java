package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ClickedBlockEvent;
import com.jelly.farmhelperv2.event.PlayerDestroyBlockEvent;
import com.jelly.farmhelperv2.handler.MacroHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerControllerMP.class})
public class MixinPlayerControllerMP {
    @Shadow
    private float curBlockDamageMP;

    @Shadow
    private float stepSoundTickCounter;

    @Shadow
    private int blockHitDelay;

    @Shadow
    @Final
    private Minecraft mc;

    @Inject(method = {"clickBlock"}, at = {@At(value = "HEAD")})
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        Block block = this.mc.theWorld.getBlockState(loc).getBlock();
        if (!FarmHelperConfig.pinglessCactus || !MacroHandler.getInstance().getCrop().equals(FarmHelperConfig.CropEnum.CACTUS)) {
            ClickedBlockEvent event = new ClickedBlockEvent(loc, face, block);
            MinecraftForge.EVENT_BUS.post(event);
        }
        if (!(block instanceof BlockCactus)) return;
        ItemStack currentItem = this.mc.thePlayer.inventory.getCurrentItem();
        if (currentItem != null && currentItem.getDisplayName().contains("Cactus Knife") && block.equals(Blocks.cactus)) {
            PlayerDestroyBlockEvent event = new PlayerDestroyBlockEvent(loc, face, block);
            MinecraftForge.EVENT_BUS.post(event);
            if (FarmHelperConfig.pinglessCactus) {
                this.mc.theWorld.setBlockToAir(loc);
//                this.curBlockDamageMP = 0.0F;
//                this.stepSoundTickCounter = 0.0F;
                ClickedBlockEvent event2 = new ClickedBlockEvent(loc, face, Blocks.cactus);
                MinecraftForge.EVENT_BUS.post(event2);
            }
        }
    }

    @Inject(method = {"onPlayerDestroyBlock"}, at = {@At(value = "HEAD")})
    public void onPlayerDestroyBlock(BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        Block block = this.mc.theWorld.getBlockState(pos).getBlock();
        PlayerDestroyBlockEvent event = new PlayerDestroyBlockEvent(pos, side, block);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
