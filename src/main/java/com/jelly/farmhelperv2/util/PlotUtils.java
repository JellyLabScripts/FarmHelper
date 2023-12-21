package com.jelly.farmhelperv2.util;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class PlotUtils {
    @Getter
    private static final HashMap<Integer, List<Tuple<Integer, Integer>>> PLOTS = new HashMap<>();

    @Getter
    private static final ArrayList<Integer> PLOT_NUMBERS = new ArrayList<>(Arrays.asList(21, 13, 9, 14, 22, 15, 5, 1, 6, 16, 10, 2, 0, 3, 11, 17, 7, 4, 8, 18, 23, 19, 12, 20, 24));
    private static final Minecraft mc = Minecraft.getMinecraft();

    static {
        for (int i = 0; i <= 24; i++) {
            PLOTS.put(i, new ArrayList<>());
        }

        PLOTS.get(21).addAll(getChunks(-15, -10, -15, -10));
        PLOTS.get(13).addAll(getChunks(-9, -4, -15, -10));
        PLOTS.get(9).addAll(getChunks(-3, 2, -15, -10));
        PLOTS.get(14).addAll(getChunks(3, 8, -15, -10));
        PLOTS.get(22).addAll(getChunks(9, 14, -15, -10));
        PLOTS.get(15).addAll(getChunks(-15, -10, -9, -4));
        PLOTS.get(5).addAll(getChunks(-9, -4, -9, -4));
        PLOTS.get(1).addAll(getChunks(-3, 2, -9, -4));
        PLOTS.get(6).addAll(getChunks(3, 8, -9, -4));
        PLOTS.get(16).addAll(getChunks(9, 14, -9, -4));
        PLOTS.get(10).addAll(getChunks(-15, -10, -3, 2));
        PLOTS.get(2).addAll(getChunks(-9, -4, -3, 2));
        PLOTS.get(0).addAll(getChunks(-3, 2, -3, 2)); // BARN
        PLOTS.get(3).addAll(getChunks(3, 8, -3, 2));
        PLOTS.get(11).addAll(getChunks(9, 14, -3, 2));
        PLOTS.get(17).addAll(getChunks(-15, -10, 3, 8));
        PLOTS.get(7).addAll(getChunks(-9, -4, 3, 8));
        PLOTS.get(4).addAll(getChunks(-3, 2, 3, 8));
        PLOTS.get(8).addAll(getChunks(3, 8, 3, 8));
        PLOTS.get(18).addAll(getChunks(9, 14, 3, 8));
        PLOTS.get(23).addAll(getChunks(-15, -10, 9, 14));
        PLOTS.get(19).addAll(getChunks(-9, -4, 9, 14));
        PLOTS.get(12).addAll(getChunks(-3, 2, 9, 14));
        PLOTS.get(20).addAll(getChunks(3, 8, 9, 14));
        PLOTS.get(24).addAll(getChunks(9, 14, 9, 14));
    }

    public static List<Tuple<Integer, Integer>> getPlotChunksBasedOnLocation(BlockPos pos) {
        Chunk chunk = mc.theWorld.getChunkFromBlockCoords(pos);
        for (Map.Entry<Integer, List<Tuple<Integer, Integer>>> entry : PLOTS.entrySet()) {
            if (entry.getValue().stream().anyMatch(t -> t.getFirst() == chunk.xPosition && t.getSecond() == chunk.zPosition)) {
                return entry.getValue();
            }
        }
        return new ArrayList<>();
    }

    public static List<Tuple<Integer, Integer>> getPlotChunksBasedOnLocation() {
        AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        // get blockpos in the middle of the player's bounding box
        BlockPos pos = new BlockPos(playerBoundingBox.minX + (playerBoundingBox.maxX - playerBoundingBox.minX) / 2, playerBoundingBox.minY, playerBoundingBox.minZ + (playerBoundingBox.maxZ - playerBoundingBox.minZ) / 2);
        return getPlotChunksBasedOnLocation(pos);
    }

    public static int getPlotNumberBasedOnLocation(BlockPos pos) {
        Chunk chunk = mc.theWorld.getChunkFromBlockCoords(pos);
        for (Map.Entry<Integer, List<Tuple<Integer, Integer>>> entry : PLOTS.entrySet()) {
            if (entry.getValue().stream().anyMatch(t -> t.getFirst() == chunk.xPosition && t.getSecond() == chunk.zPosition)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public static int getPlotNumberBasedOnLocation() {
        AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        // get blockpos in the middle of the player's bounding box
        BlockPos pos = new BlockPos(playerBoundingBox.minX + (playerBoundingBox.maxX - playerBoundingBox.minX) / 2, playerBoundingBox.minY, playerBoundingBox.minZ + (playerBoundingBox.maxZ - playerBoundingBox.minZ) / 2);
        return getPlotNumberBasedOnLocation(pos);
    }

    public static List<Tuple<Integer, Integer>> getPlotBasedOnNumber(int plotNumber) {
        return PLOTS.get(plotNumber);
    }

    private static List<Tuple<Integer, Integer>> getChunks(int fromX, int toX, int fromZ, int toZ) {
        List<Tuple<Integer, Integer>> chunks = new ArrayList<>();
        int maxX = Math.max(fromX, toX);
        int minX = Math.min(fromX, toX);
        int maxZ = Math.max(fromZ, toZ);
        int minZ = Math.min(fromZ, toZ);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                chunks.add(new Tuple<>(x, z));
            }
        }
        return chunks;
    }

    public static BlockPos getPlotCenter(int plotNumber) {
        List<Tuple<Integer, Integer>> chunks = getPlotBasedOnNumber(plotNumber);
        int minX = chunks.stream().mapToInt(Tuple::getFirst).min().orElse(0);
        int maxX = chunks.stream().mapToInt(Tuple::getFirst).max().orElse(0);
        int minZ = chunks.stream().mapToInt(Tuple::getSecond).min().orElse(0);
        int maxZ = chunks.stream().mapToInt(Tuple::getSecond).max().orElse(0);
        return new BlockPos((minX + maxX) / 2 * 16, 80, (minZ + maxZ) / 2 * 16);
    }

    public static BlockPos getPlotNearestEdgeToPlayer(int plotNumber) {
        List<Tuple<Integer, Integer>> chunks = getPlotBasedOnNumber(plotNumber);
        int minX = chunks.stream().mapToInt(Tuple::getFirst).min().orElse(0);
        int maxX = chunks.stream().mapToInt(Tuple::getFirst).max().orElse(0);
        int minZ = chunks.stream().mapToInt(Tuple::getSecond).min().orElse(0);
        int maxZ = chunks.stream().mapToInt(Tuple::getSecond).max().orElse(0);
        int playerX = (int) mc.thePlayer.posX;
        int playerZ = (int) mc.thePlayer.posZ;
        int x = playerX < (minX + maxX) / 2 * 16 ? minX * 16 : maxX * 16;
        int z = playerZ < (minZ + maxZ) / 2 * 16 ? minZ * 16 : maxZ * 16;
        return new BlockPos(x, 80, z);
    }
}
