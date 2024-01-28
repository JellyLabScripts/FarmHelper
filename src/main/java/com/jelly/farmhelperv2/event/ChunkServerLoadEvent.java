package com.jelly.farmhelperv2.event;

import lombok.Getter;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class ChunkServerLoadEvent extends Event {
    public int x;
    public int z;
    public Chunk chunk;

    public ChunkServerLoadEvent(int x, int z, Chunk c) {
        this.x = x;
        this.z = z;
        this.chunk = c;
    }
}
