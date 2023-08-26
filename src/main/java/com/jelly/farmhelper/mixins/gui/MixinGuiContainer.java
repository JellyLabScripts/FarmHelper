package com.jelly.farmhelper.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Shadow public Container inventorySlots;

    /**
     * @author
     */
    @Overwrite
    public void onGuiClosed() {
        try {
            if (Minecraft.getMinecraft().thePlayer != null) {
                this.inventorySlots.onContainerClosed(Minecraft.getMinecraft().thePlayer);
            }
        } catch (Exception e ) {
            e.printStackTrace();
            onGuiClosed();
        }

    }
}
