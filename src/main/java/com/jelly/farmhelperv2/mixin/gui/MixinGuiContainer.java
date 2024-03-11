package com.jelly.farmhelperv2.mixin.gui;

import com.jelly.farmhelperv2.event.DrawScreenAfterEvent;
import com.jelly.farmhelperv2.event.InventoryInputEvent;
import com.jelly.farmhelperv2.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen_after(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String name = InventoryUtils.getInventoryName();
        MinecraftForge.EVENT_BUS.post(new DrawScreenAfterEvent(Minecraft.getMinecraft().currentScreen));
    }

    @Inject(method = "keyTyped", at = @At("RETURN"), cancellable = true)
    public void keyTyped_after(char typedChar, int keyCode, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new InventoryInputEvent(keyCode, typedChar))) {
            ci.cancel();
        }
    }
}
