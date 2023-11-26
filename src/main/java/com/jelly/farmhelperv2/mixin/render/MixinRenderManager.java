package com.jelly.farmhelperv2.mixin.render;

import com.jelly.farmhelperv2.feature.impl.Freelock;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {
    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float modifyPitch(Entity entity) {
        return Freelock.getInstance().getPitch(entity.rotationPitch);
    }

    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float modifyYaw(Entity entity) {
        return Freelock.getInstance().getYaw(entity.rotationYaw);
    }

    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"))
    private float modifyPrevPitch(Entity entity) {
        return Freelock.getInstance().getPrevPitch(entity.prevRotationPitch);
    }

    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"))
    private float modifyPrevYaw(Entity entity) {
        return Freelock.getInstance().getPrevYaw(entity.prevRotationYaw);
    }
}
