package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.gui.ProxyManagerGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer extends GuiScreen {

    @Inject(method = "initGui", at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        this.buttonList.add(new GuiButton(2137, 8, 8, 130, 20, "FH - Proxy"));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    public void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 2137) {
            this.mc.displayGuiScreen(new ProxyManagerGUI(this));
            ci.cancel();
        }
    }

    @Inject(method = "connectToServer", at = @At(value = "HEAD"))
    public void connectToServer(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getNetHandler() != null) {
            minecraft.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText(""));
        }
    }
}
