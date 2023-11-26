package com.jelly.farmhelperv2.mixin.gui;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiPlayerTabOverlay.class)
public interface IGuiPlayerTabOverlayAccessor {
    @Accessor
    IChatComponent getFooter();

    @Accessor
    IChatComponent getHeader();
}
