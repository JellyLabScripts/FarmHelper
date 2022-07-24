package com.jelly.farmhelper.mixins.block;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Block.class)
public interface IBlockAccessor {
    @Accessor
    void setMaxY(double maxY);
}