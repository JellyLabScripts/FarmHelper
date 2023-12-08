package com.jelly.farmhelperv2.mixin.render;

import com.jelly.farmhelperv2.feature.impl.Freelook;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityRenderer.class, priority = Integer.MAX_VALUE)
public class MixinEntityRenderer {
    @Shadow
    private float thirdPersonDistanceTemp;

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float modifyPitch(Entity entity) {
        if (entity == null) return 0F;
        return Freelook.getInstance().getPitch(entity.rotationPitch);
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float modifyYaw(Entity entity) {
        if (entity == null) return 0F;
        return Freelook.getInstance().getYaw(entity.rotationYaw);
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"))
    private float modifyPrevPitch(Entity entity) {
        if (entity == null) return 0F;
        return Freelook.getInstance().getPrevPitch(entity.prevRotationPitch);
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"))
    private float modifyPrevYaw(Entity entity) {
        if (entity == null) return 0F;
        return Freelook.getInstance().getPrevYaw(entity.prevRotationYaw);
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    public double onCamera(Vec3 instance, Vec3 vec) {
        if (Freelook.getInstance().isRunning()) return 999D;
        return instance.distanceTo(vec);
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistanceTemp:F"))
    private float modifyDistance(EntityRenderer entityRenderer) {
        if (Freelook.getInstance().isRunning()) return Freelook.getInstance().getDistance();
        return thirdPersonDistanceTemp;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistance:F"))
    private float modifyDistance2(EntityRenderer entityRenderer) {
        if (Freelook.getInstance().isRunning()) return Freelook.getInstance().getDistance();
        return thirdPersonDistanceTemp;
    }

}
