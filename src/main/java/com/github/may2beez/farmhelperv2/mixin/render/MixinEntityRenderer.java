package com.github.may2beez.farmhelperv2.mixin.render;

import com.github.may2beez.farmhelperv2.feature.impl.Freelock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Shadow private float thirdPersonDistance;

    @Shadow private float thirdPersonDistanceTemp;

    @Unique
    private void farmHelperV2$setPerspectiveAngles(float x, float y, int invert) {
        if (!Freelock.getInstance().isRunning()) return;
        Freelock.getInstance().setCameraPitch(Freelock.getInstance().getCameraPitch() + (y / 8f));
        Freelock.getInstance().setCameraYaw(Freelock.getInstance().getCameraYaw() + (x / 8f));
        if (Math.abs(Freelock.getInstance().getCameraPitch()) > 90)
            Freelock.getInstance().setCameraPitch(Freelock.getInstance().getCameraPitch() > 0 ? 90 : -90);
    }

    @Inject(method = "updateCameraAndRender", at = {@At(value = "INVOKE", target = "net/minecraft/client/entity/EntityPlayerSP.setAngles(FF)V", ordinal = 0)}, locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void perspectiveCameraUpdatingSmooth(float partialTicks, long time, CallbackInfo info, boolean flag, float sens, float adjustedSens, float x, float y, int invert, float delta) {
        farmHelperV2$setPerspectiveAngles(x, y, invert);
    }

    @Inject(method = {"updateCameraAndRender"}, at = {@At(value = "INVOKE", target = "net/minecraft/client/entity/EntityPlayerSP.setAngles(FF)V", ordinal = 1)}, locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void perspectiveCameraUpdatingNormal(float partialTicks, long time, CallbackInfo info, boolean flag, float sens, float adjustedSens, float x, float y, int invert) {
        farmHelperV2$setPerspectiveAngles(x, y, invert);
    }

    @Redirect(method = {"updateCameraAndRender"}, at = @At(value = "INVOKE", target = "net/minecraft/client/entity/EntityPlayerSP.setAngles(FF)V"))
    private void perspectivePreventMovement(EntityPlayerSP player, float x, float y) {
        if (Freelock.getInstance().isRunning()) return;

        player.setAngles(x, y);
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistance:F"))
    public float tweakThirdPersonDistance(EntityRenderer instance) {
        if (Freelock.getInstance().isRunning()) return 4.0f;
        return this.thirdPersonDistance;
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistanceTemp:F"))
    public float tweakThirdPersonDistanceTemp(EntityRenderer instance) {
        if (Freelock.getInstance().isRunning()) return 4.0f;
        return this.thirdPersonDistanceTemp;
    }

    @Redirect(method = {"orientCamera"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    public double cameraDistance(Vec3 instance, Vec3 vec) {
        if (Freelock.getInstance().isRunning()) return 4.0d;
        return instance.distanceTo(vec);
    }
}
