package com.jelly.farmhelper.mixins;

import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.macros.CropMacro;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCarrot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ PlayerControllerMP.class })
public class MixinPlayerControllerMP {
    @Inject(method={"clickBlock"}, at={@At(value="HEAD")}, cancellable=true)
    public void clickBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        if (MiscConfig.resync && CropMacro.isEnabled() && loc != null && Minecraft.getMinecraft().theWorld.getBlockState(loc) != null && Minecraft.getMinecraft().theWorld.getBlockState(loc).getBlock() instanceof BlockBush) {
            Resync.update(loc);
        }
    }

//    @Inject(method={"clickBlock"}, at={@At(value="HEAD")}, cancellable=true)
//    private void clickBlockHook(BlockPos pos, EnumFacing face, CallbackInfoReturnable<Boolean> info) {
//        // LogUtils.debugFullLog("clickBlockHook - " + pos + Minecraft.getMinecraft().theWorld.getBlockState(pos));
//    }

//    @Inject(method={"onPlayerDamageBlock"}, at={ @At(value="HEAD") }, cancellable=true)
//    private void onPlayerDamageBlockHook(BlockPos pos, EnumFacing face, CallbackInfoReturnable<Boolean> info) {
//
//    }
//
//    @Inject(method={"resetBlockRemoving"}, at={ @At(value="HEAD") }, cancellable=true)
//    private void resetBlock(CallbackInfo info) {
//
//    }
//
//    @Inject(method={"getBlockReachDistance"}, at={@At(value="RETURN")}, cancellable=true)
//    private void getReachDistanceHook(CallbackInfoReturnable<Float> distance) {
//
//    }
}