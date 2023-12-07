package com.jelly.farmhelperv2.mixin.render;

import com.jelly.farmhelperv2.feature.impl.Freelook;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {
    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;rotationPitch:F"))
    private static float modifyPitch(EntityPlayer player) {
        return Freelook.getInstance().getPitch(player.rotationPitch);
    }

    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;rotationYaw:F"))
    private static float modifyYaw(EntityPlayer player) {
        return Freelook.getInstance().getYaw(player.rotationYaw);
    }
}
