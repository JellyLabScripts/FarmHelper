package com.jelly.farmhelper.mixins.client;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ PlayerControllerMP.class })
public class MixinPlayerControllerMP {
    private final Clock checkTimer = new Clock();

    private int blocksChecked = 0;

    @Inject(method={"clickBlock"}, at={@At(value="HEAD")})
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        if (FarmHelper.config.checkDesync &&
                MacroHandler.isMacroing && MacroHandler.currentMacro != null
                && MacroHandler.currentMacro.enabled && loc != null
                && Minecraft.getMinecraft().theWorld.getBlockState(loc) != null) {


            if (checkTimer.passed()) {
                if(Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockReed
                        || Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock().equals(Blocks.cactus)) {
                    blocksChecked ++;
                    Resync.update(loc.up()); // check 1 block above sugarcane or cactus
                }
                else if((Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockBush ||
                                Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockMelon ||
                                    Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockPumpkin)
                                && !(Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockStem)) {
                    // don't check stems

                    blocksChecked ++;
                    Resync.update(loc);
                }


                if(blocksChecked >= 4) {
                    blocksChecked = 0;
                    checkTimer.schedule(3000);
                }
            }

        }
    }
}