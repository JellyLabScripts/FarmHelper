package com.jelly.farmhelperv2.mixin.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiEditSign.class)
public interface AccessorGuiEditSign {
    @Accessor("tileSign")
    TileEntitySign getTileSign();

    @Invoker("actionPerformed")
    void invokeActionPerformed(GuiButton button);
}
