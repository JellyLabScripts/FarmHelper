package com.jelly.farmhelperv2.pathfinder;

import com.google.common.collect.Lists;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.MotionUpdateEvent;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.handler.GameStateHandler;
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
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class WorldCache {
    private static WorldCache instance;
    private final ExecutorService executorService = Executors.newScheduledThreadPool(10);
    private final HashMap<Coordinate, Chunk> chunkCache = new HashMap<>();
    private final List<Class<? extends Block>> directionDependentBlocks = Lists.newArrayList(BlockDoor.class, BlockTrapDoor.class, BlockStairs.class);

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

    private final HashMap<BlockPos, CacheEntry> worldCache = new HashMap<>();
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
    public void onWorldUnload(WorldEvent.Unload event) {
        worldCache.clear();
        chunkCache.clear();
    }

    @SubscribeEvent
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.theWorld == null) return;
        if (!FarmHelperConfig.useCachingInFlyPathfinder) return;
        if (!GameStateHandler.getInstance().getLocation().equals(GameStateHandler.Location.TELEPORTING) && !GameStateHandler.getInstance().inGarden())
            return;


        if (event.packet instanceof S21PacketChunkData) {
            S21PacketChunkData packet = (S21PacketChunkData) event.packet;
            if (packet.getExtractedSize() == 0) return;
            Chunk c = new Chunk(mc.theWorld, packet.getChunkX(), packet.getChunkZ());
            c.fillChunk(packet.getExtractedDataBytes(), packet.getExtractedSize(), packet.func_149274_i());
            Coordinate coordinate = new Coordinate(packet.getChunkX(), packet.getChunkZ());
            cacheChunk(c, coordinate);
        } else if (event.packet instanceof S26PacketMapChunkBulk) {
            S26PacketMapChunkBulk packet = (S26PacketMapChunkBulk) event.packet;
            for (int i = 0; i < packet.getChunkCount(); i++) {
                Chunk c = new Chunk(mc.theWorld, packet.getChunkX(i), packet.getChunkZ(i));
                c.fillChunk(packet.getChunkBytes(i), packet.getChunkSize(i), true);
                Coordinate coordinate = new Coordinate(packet.getChunkX(i), packet.getChunkZ(i));
                cacheChunk(c, coordinate);
            }
        }
    }

    private void cacheChunk(Chunk c, Coordinate coordinate) {
        if (chunkCache.containsKey(coordinate)) return;
        for (int x = 0; x < 16; x++)
            for (int y = 66; y < 100; y++)
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x + coordinate.x * 16, y, z + coordinate.z * 16);
                    BlockPos chunkPos = new BlockPos(x, y, z);
                    IBlockState chunkState = c.getBlockState(chunkPos);
                    if (!BlockUtils.blockHasCollision(pos, chunkState, chunkState.getBlock())) {
                        worldCache.put(pos, new CacheEntry(chunkState.getBlock(), pos, BlockUtils.PathNodeType.OPEN));
                    } else {
                        worldCache.put(pos, new CacheEntry(chunkState.getBlock(), pos, BlockUtils.PathNodeType.BLOCKED));
                    }
                }
        chunkCache.put(coordinate, c);
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
}
