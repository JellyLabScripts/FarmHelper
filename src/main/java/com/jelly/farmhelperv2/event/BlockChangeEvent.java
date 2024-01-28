package com.jelly.farmhelperv2.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.jetbrains.annotations.NotNull;

public class BlockChangeEvent extends Event {
    public BlockPos pos;
    public IBlockState old;
    public IBlockState update;
    public IBlockAccess world;

    public BlockChangeEvent(@NotNull BlockPos pos, @NotNull IBlockState old, @NotNull IBlockState update, @NotNull IBlockAccess world) {
        this.pos = pos;
        this.old = old;
        this.update = update;
        this.world = world;
    }
}
