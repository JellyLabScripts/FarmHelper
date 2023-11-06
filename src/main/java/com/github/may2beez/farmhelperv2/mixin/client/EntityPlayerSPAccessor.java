package com.github.may2beez.farmhelperv2.mixin.client;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerSP.class)
public interface EntityPlayerSPAccessor {
    @Accessor
    float getLastReportedYaw();

    @Accessor
    void setLastReportedYaw(float lastReportedYaw);

    @Accessor
    void setLastReportedPitch(float lastReportedPitch);
    @Accessor
    float getLastReportedPitch();
}
