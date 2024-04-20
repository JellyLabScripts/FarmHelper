package com.jelly.farmhelperv2.event;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PlayerDestroyBlockEvent extends Event {
    public final BlockPos pos;
    public final EnumFacing facing;
    public final Block block;

    public PlayerDestroyBlockEvent(BlockPos pos, EnumFacing facing, Block block) {
        this.pos = pos;
        this.facing = facing;
        this.block = block;
    }
}
