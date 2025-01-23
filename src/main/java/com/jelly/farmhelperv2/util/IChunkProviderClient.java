package com.jelly.farmhelperv2.util;

import java.util.List;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;

public interface IChunkProviderClient {

    LongHashMap<Chunk> chunkMapping();
    List<Chunk> chunkListing();

}