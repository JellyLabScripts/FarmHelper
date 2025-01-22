package com.jelly.farmhelperv2.pathfinder.custom.util;

import com.jelly.farmhelperv2.util.IChunkProviderClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class BlockStateAccessor {

    private final World world;
    private final Long2ObjectMap<Chunk> loadedChunks;
    private Chunk cached;
    public IBlockAccess access;
    public final BlockPos.MutableBlockPos isPassableBlockPos;

    public BlockStateAccessor(World world) {
        this.world = world;
        this.loadedChunks = new Long2ObjectOpenHashMap<>();
        this.cached = null;

        List<Chunk> loadedWorld = ((IChunkProviderClient) this.world.getChunkProvider()).chunkListing();

        for (Chunk chunk : loadedWorld) {
            this.loadedChunks.put(getKey(chunk.xPosition, chunk.zPosition), chunk);
        }

        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this, world);
    }

    public IBlockState get(int x, int y, int z) {
        Chunk current = this.cached;
        if (current != null && current.xPosition == (x >> 4) && current.zPosition == (z >> 4)) {
            return current.getBlockState(new BlockPos(x, y, z));
        }

        current = this.loadedChunks.get(getKey(x >> 4, z >> 4));

        if (current != null && current.isLoaded()) {
            this.cached = current;
            return current.getBlockState(new BlockPos(x, y, z));
        }

        return Blocks.air.getDefaultState();
    }

    public IBlockState get(BlockPos block) {
        return get(block.getX(), block.getY(), block.getZ());
    }

    public boolean isBlockInLoadedChunks(int blockX, int blockZ) {
        return this.loadedChunks.containsKey(getKey(blockX >> 4, blockZ >> 4));
    }

    private long getKey(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }
}
