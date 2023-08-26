package com.jelly.farmhelper.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.jetbrains.annotations.NotNull;

public class BlockChangeEvent extends Event {
    public BlockPos pos;
    public IBlockState old;
    public IBlockState update;

    public BlockChangeEvent(@NotNull BlockPos pos, @NotNull IBlockState old, @NotNull IBlockState update) {
        this.pos = pos;
        this.old = old;
        this.update = update;
    }
}
