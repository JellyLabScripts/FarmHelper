package com.jelly.farmhelper.mixins.gui;

import com.jelly.farmhelper.mixins.render.MixinInventoryEffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory extends MixinInventoryEffectRenderer {
    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * @author
     */
    @Final
    @Overwrite
    public void updateScreen() {
        try {
            if (this.mc.theWorld == null || this.mc.thePlayer == null || this.mc.playerController == null) return;
            if (this.mc.playerController.isInCreativeMode()) {
                this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
            }
            this.updateActivePotionEffects();
        } catch (Exception e) {
            e.printStackTrace();
            updateScreen();
        }

    }
}
