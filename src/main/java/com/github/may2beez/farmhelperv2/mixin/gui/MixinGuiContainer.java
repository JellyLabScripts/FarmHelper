package com.github.may2beez.farmhelperv2.mixin.gui;

import com.github.may2beez.farmhelperv2.event.DrawScreenAfterEvent;
import com.github.may2beez.farmhelperv2.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen_after(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String name = InventoryUtils.getInventoryName();
        MinecraftForge.EVENT_BUS.post(new DrawScreenAfterEvent(Minecraft.getMinecraft().currentScreen));
    }
}
