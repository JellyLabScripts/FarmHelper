package com.jelly.farmhelperv2.event;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class ClickedBlockEvent extends Event {
    private final BlockPos pos;
    private final EnumFacing facing;
    private final Block block;

    public ClickedBlockEvent(BlockPos pos, EnumFacing facing, Block block) {
        this.pos = pos;
        this.facing = facing;
        this.block = block;
    }
}
