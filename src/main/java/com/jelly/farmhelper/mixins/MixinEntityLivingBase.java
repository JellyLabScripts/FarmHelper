package com.jelly.farmhelper.mixins;

import com.jelly.farmhelper.config.interfaces.MiscConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

    @Inject(method={"swingItem"}, at={@At(value="HEAD")}, cancellable=true)
    public void swingItem(CallbackInfo ci) {
        if (MiscConfig.fastbreak) ci.cancel();
    }
}
