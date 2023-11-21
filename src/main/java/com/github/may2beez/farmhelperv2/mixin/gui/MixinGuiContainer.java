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
    @Shadow
    public Container inventorySlots;

    @Unique
    private String farmHelperV2$previousName = "";

    @Unique
    private List<Slot> farmHelperV2$previousSlots;

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void drawScreen_after(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        String name = InventoryUtils.getInventoryName();
        boolean isFullyLoaded = Objects.equals(name, farmHelperV2$previousName) && Objects.requireNonNull(inventorySlots).inventorySlots.equals(farmHelperV2$previousSlots);
        MinecraftForge.EVENT_BUS.post(new DrawScreenAfterEvent(Minecraft.getMinecraft().currentScreen, isFullyLoaded));

        farmHelperV2$previousName = name;
        farmHelperV2$previousSlots = Objects.requireNonNull(inventorySlots).inventorySlots;
    }
}
