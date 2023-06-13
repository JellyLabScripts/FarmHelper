package com.jelly.farmhelper.mixins.gui;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.gui.ProxyScreen;
import com.jelly.farmhelper.network.proxy.ConnectionState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer extends MixinGuiScreen {
    GuiButton proxybtn;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo ci) {
        proxybtn = new GuiButton(420, this.width - 140, 15, 120, 20, "Proxy");
        buttonList.add(proxybtn);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ProxyScreen.state == null && !FarmHelper.config.connectAtStartup) {
            proxybtn.displayString = ConnectionState.DISCONNECTED.color + "Proxy - DISCONNECTED";
        } else if (ProxyScreen.state != null) {
            switch (ProxyScreen.state) {
                case INVALID:
                    proxybtn.displayString = ConnectionState.INVALID.color+ "Proxy - INVALID";
                    break;
                case DISCONNECTED:
                    proxybtn.displayString = ConnectionState.DISCONNECTED.color + "Proxy - DISCONNECTED";
                    break;
                case CONNECTED:
                    proxybtn.displayString = ConnectionState.CONNECTED.color + "Proxy - CONNECTED";
                    break;
                case CONNECTING:
                    proxybtn.displayString = ConnectionState.CONNECTING.color + "Proxy - CONNECTING";
                    break;
            }
        }
    }
    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 420) {
            mc.displayGuiScreen(new ProxyScreen((GuiScreen) (Object) this));
            ci.cancel();
        }
    }
}
