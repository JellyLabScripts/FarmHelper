package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.event.MotionUpdateEvent;
import com.jelly.farmhelperv2.feature.impl.Freelook;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityPlayerSP.class, priority = Integer.MAX_VALUE)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Override
    public void setAngles(float yaw, float pitch) {
        if (Freelook.getInstance().isRunning()) {
            Freelook.getInstance().setCameraPrevYaw(Freelook.getInstance().getCameraYaw());
            Freelook.getInstance().setCameraPrevPitch(Freelook.getInstance().getCameraPitch());
            Freelook.getInstance().setCameraYaw((float) (Freelook.getInstance().getCameraYaw() + (yaw * 0.15)));
            Freelook.getInstance().setCameraPitch((float) (Freelook.getInstance().getCameraPitch() + (pitch * -0.15)));
            Freelook.getInstance().setCameraPitch(MathHelper.clamp_float(Freelook.getInstance().getCameraPitch(), -90.0F, 90.0F));
        } else {
            super.setAngles(yaw, pitch);
        }
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    public void onUpdateWalkingPlayer(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new MotionUpdateEvent.Pre(this.rotationYaw, this.rotationPitch));
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    public void onUpdateWalkingPlayerReturn(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new MotionUpdateEvent.Post(this.rotationYaw, this.rotationPitch));
    }
}
