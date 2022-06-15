package com.jelly.farmhelper.mixins;

import com.jelly.farmhelper.features.BanwaveChecker;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.macros.MacroHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ GuiDisconnected.class })
public class MixinGuiDisconnected {
    @Inject(method={"drawScreen"}, at={@At(value="TAIL")})
    public void drawScreen(CallbackInfo ci) {
        if(MacroHandler.isMacroing) {
            if (BanwaveChecker.banwaveOn)
                Minecraft.getMinecraft().fontRendererObj.drawString("There is a banwave! " + BanwaveChecker.getBanDisplay(), 5, 5, -1);
            if (!Failsafe.jacobWait.passed())
                Minecraft.getMinecraft().fontRendererObj.drawString("In Jacob Failsafe", 5, 20, -1);
        }

    }

}
