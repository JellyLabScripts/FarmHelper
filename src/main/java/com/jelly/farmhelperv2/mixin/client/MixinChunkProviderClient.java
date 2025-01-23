package com.jelly.farmhelperv2.mixin.client;

import com.jelly.farmhelperv2.util.IChunkProviderClient;
import java.util.List;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkProviderClient.class)
public class MixinChunkProviderClient implements IChunkProviderClient {

    @Shadow
    private LongHashMap<Chunk> chunkMapping;

    @Shadow
    private List<Chunk> chunkListing;

    @Override
    public LongHashMap<Chunk> chunkMapping() {
        return chunkMapping;
    }

    @Override
    public List<Chunk> chunkListing() {
        return chunkListing;
    }

}