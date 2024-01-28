package com.jelly.farmhelperv2.pathfinder;

import com.google.common.collect.Lists;
import com.jelly.farmhelperv2.event.ChunkServerLoadEvent;
import com.jelly.farmhelperv2.event.MotionUpdateEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.util.BlockUtils;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class WorldCache {
    private static WorldCache instance;
    private final List<Class<? extends Block>> directionDependentBlocks = Lists.newArrayList(BlockDoor.class, BlockTrapDoor.class, BlockStairs.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(30);

    @Getter
    private BlockPos currentPos;
    @Getter
    private BlockPos lastPos;

    public static WorldCache getInstance() {
        if (instance == null) {
            instance = new WorldCache();
        }
        return instance;
    }

    private final HashMap<IBlockAccess, WorldCacheEntry> worldCache = new HashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onMotionUpdateEvent(MotionUpdateEvent event) {
        if (currentPos == null) {
            currentPos = new BlockPos(mc.thePlayer);
            lastPos = currentPos;
        } else if (!currentPos.equals(new BlockPos(mc.thePlayer))) {
            lastPos = currentPos;
            currentPos = new BlockPos(mc.thePlayer);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkServerLoadEvent event) {
//        cacheChunk(event.getChunk(), new Coordinate(event.getX(), event.getZ()));
    }

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.theWorld == null) return;
        if ((event.packet instanceof S21PacketChunkData)) {
            S21PacketChunkData packet = (S21PacketChunkData) event.packet;
            Chunk c = mc.theWorld.getChunkFromChunkCoords(packet.getChunkX(), packet.getChunkZ());
            cacheChunk(c, new Coordinate(packet.getChunkX(), packet.getChunkZ()));
        } else if (event.packet instanceof S26PacketMapChunkBulk) {
            S26PacketMapChunkBulk packet = (S26PacketMapChunkBulk) event.packet;
            for (int i = 0; i < packet.getChunkCount(); i++) {
                Chunk c = mc.theWorld.getChunkFromChunkCoords(packet.getChunkX(i), packet.getChunkZ(i));
                cacheChunk(c, new Coordinate(packet.getChunkX(i), packet.getChunkZ(i)));
            }
        }
    }

    public void cacheChunk(Chunk c, Coordinate coordinate) {
        WorldCacheEntry worldCacheEntry = worldCache.get(mc.theWorld);
        if (worldCacheEntry == null) {
            worldCacheEntry = new WorldCacheEntry();
            worldCache.put(mc.theWorld, worldCacheEntry);
        }
        if (worldCacheEntry.getChunksCached().contains(coordinate)) return;
        worldCacheEntry.getChunksCached().add(coordinate);
        for (int x = 0; x < 16; x++)
            for (int y = 66; y < 83; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x + (coordinate.x << 4), y, z + (coordinate.z << 4));
                    IBlockState chunkBlock = mc.theWorld.getBlockState(pos);
                    if (!BlockUtils.blockHasCollision(pos, chunkBlock, chunkBlock.getBlock())) {
                        worldCacheEntry.getCache().put(pos, new CacheEntry(chunkBlock.getBlock(), pos, BlockUtils.PathNodeType.OPEN));
                    } else {
                        worldCacheEntry.getCache().put(pos, new CacheEntry(chunkBlock.getBlock(), pos, BlockUtils.PathNodeType.BLOCKED));
                    }
                }
    }

    public static class Coordinate {
        private final int x;
        private final int z;

        public Coordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Coordinate)) return false;
            Coordinate c = (Coordinate) obj;
            return c.x == x && c.z == z;
        }

        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }

    @Getter
    public static class CacheEntry {
        private final Block block;
        private final BlockPos pos;
        private final BlockUtils.PathNodeType pathNodeType;

        public CacheEntry(Block block, BlockPos pos, BlockUtils.PathNodeType pathNodeType) {
            this.block = block;
            this.pos = pos;
            this.pathNodeType = pathNodeType;
        }
    }

    @Getter
    public static class WorldCacheEntry {
        private final List<Coordinate> chunksCached = Lists.newArrayList();
        private final HashMap<BlockPos, CacheEntry> cache = new HashMap<>();
    }
}
