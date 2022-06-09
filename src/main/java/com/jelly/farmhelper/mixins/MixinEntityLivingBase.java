package com.jelly.farmhelper.mixins;

import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

    @Inject(method={"swingItem"}, at={@At(value="HEAD")}, cancellable=true)
    public void swingItem(CallbackInfo ci) {
            //if (MiscConfig.fastbreak && MacroHandler.isMacroing && MacroHandler.currentMacro.enabled) ci.cancel();
    }
}
