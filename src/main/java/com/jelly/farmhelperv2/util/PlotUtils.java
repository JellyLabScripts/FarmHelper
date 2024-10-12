package com.jelly.farmhelperv2.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jelly.farmhelperv2.FarmHelper;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class PlotUtils {
    private static final File plotsFile = new File("config/farmhelperv2/plots.json");
    @Getter
    private static final HashMap<Integer, Plot> PLOTS = new HashMap<>();
    @Getter
    private static final ArrayList<Integer> PLOT_NUMBERS = new ArrayList<>(Arrays.asList(21, 13, 9, 14, 22, 15, 5, 1, 6, 16, 10, 2, 0, 3, 11, 17, 7, 4, 8, 18, 23, 19, 12, 20, 24));
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void init() {
        for (int i = 0; i <= 24; i++) {
            PLOTS.put(i, new Plot(""));
        }

        String plotsContent = "";
        if (!plotsFile.exists()) {
            try {
                Files.createDirectories(plotsFile.getParentFile().toPath());
                Files.createFile(plotsFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            plotsContent = new String(Files.readAllBytes(plotsFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonObject plotsJson = new JsonObject();
        if (!plotsContent.isEmpty() && FarmHelper.gson.fromJson(plotsContent, JsonElement.class).isJsonObject())
            plotsJson = FarmHelper.gson.fromJson(plotsContent, JsonObject.class);

        PLOTS.get(21).addAll(getChunks(-15, -10, -15, -10)).setName(plotsJson.has("21") ? plotsJson.get("21").getAsString() : "").setNumber(21);
        PLOTS.get(13).addAll(getChunks(-9, -4, -15, -10)).setName(plotsJson.has("13") ? plotsJson.get("13").getAsString() : "").setNumber(13);
        PLOTS.get(9).addAll(getChunks(-3, 2, -15, -10)).setName(plotsJson.has("9") ? plotsJson.get("9").getAsString() : "").setNumber(9);
        PLOTS.get(14).addAll(getChunks(3, 8, -15, -10)).setName(plotsJson.has("14") ? plotsJson.get("14").getAsString() : "").setNumber(14);
        PLOTS.get(22).addAll(getChunks(9, 14, -15, -10)).setName(plotsJson.has("22") ? plotsJson.get("22").getAsString() : "").setNumber(22);
        PLOTS.get(15).addAll(getChunks(-15, -10, -9, -4)).setName(plotsJson.has("15") ? plotsJson.get("15").getAsString() : "").setNumber(15);
        PLOTS.get(5).addAll(getChunks(-9, -4, -9, -4)).setName(plotsJson.has("5") ? plotsJson.get("5").getAsString() : "").setNumber(5);
        PLOTS.get(1).addAll(getChunks(-3, 2, -9, -4)).setName(plotsJson.has("1") ? plotsJson.get("1").getAsString() : "").setNumber(1);
        PLOTS.get(6).addAll(getChunks(3, 8, -9, -4)).setName(plotsJson.has("6") ? plotsJson.get("6").getAsString() : "").setNumber(6);
        PLOTS.get(16).addAll(getChunks(9, 14, -9, -4)).setName(plotsJson.has("16") ? plotsJson.get("16").getAsString() : "").setNumber(16);
        PLOTS.get(10).addAll(getChunks(-15, -10, -3, 2)).setName(plotsJson.has("10") ? plotsJson.get("10").getAsString() : "").setNumber(10);
        PLOTS.get(2).addAll(getChunks(-9, -4, -3, 2)).setName(plotsJson.has("2") ? plotsJson.get("2").getAsString() : "").setNumber(2);
        PLOTS.get(0).addAll(getChunks(-3, 2, -3, 2)).setName("Barn").setNumber(0); // BARN
        PLOTS.get(3).addAll(getChunks(3, 8, -3, 2)).setName(plotsJson.has("3") ? plotsJson.get("3").getAsString() : "").setNumber(3);
        PLOTS.get(11).addAll(getChunks(9, 14, -3, 2)).setName(plotsJson.has("11") ? plotsJson.get("11").getAsString() : "").setNumber(11);
        PLOTS.get(17).addAll(getChunks(-15, -10, 3, 8)).setName(plotsJson.has("17") ? plotsJson.get("17").getAsString() : "").setNumber(17);
        PLOTS.get(7).addAll(getChunks(-9, -4, 3, 8)).setName(plotsJson.has("7") ? plotsJson.get("7").getAsString() : "").setNumber(7);
        PLOTS.get(4).addAll(getChunks(-3, 2, 3, 8)).setName(plotsJson.has("4") ? plotsJson.get("4").getAsString() : "").setNumber(4);
        PLOTS.get(8).addAll(getChunks(3, 8, 3, 8)).setName(plotsJson.has("8") ? plotsJson.get("8").getAsString() : "").setNumber(8);
        PLOTS.get(18).addAll(getChunks(9, 14, 3, 8)).setName(plotsJson.has("18") ? plotsJson.get("18").getAsString() : "").setNumber(18);
        PLOTS.get(23).addAll(getChunks(-15, -10, 9, 14)).setName(plotsJson.has("23") ? plotsJson.get("23").getAsString() : "").setNumber(23);
        PLOTS.get(19).addAll(getChunks(-9, -4, 9, 14)).setName(plotsJson.has("19") ? plotsJson.get("19").getAsString() : "").setNumber(19);
        PLOTS.get(12).addAll(getChunks(-3, 2, 9, 14)).setName(plotsJson.has("12") ? plotsJson.get("12").getAsString() : "").setNumber(12);
        PLOTS.get(20).addAll(getChunks(3, 8, 9, 14)).setName(plotsJson.has("20") ? plotsJson.get("20").getAsString() : "").setNumber(20);
        PLOTS.get(24).addAll(getChunks(9, 14, 9, 14)).setName(plotsJson.has("24") ? plotsJson.get("24").getAsString() : "").setNumber(24);
    }

    public static List<Tuple<Integer, Integer>> getPlotChunksBasedOnLocation(BlockPos pos) {
        Chunk chunk = mc.theWorld.getChunkFromBlockCoords(pos);
        for (Map.Entry<Integer, Plot> entry : PLOTS.entrySet()) {
            if (entry.getValue().chunks.stream().anyMatch(t -> t.getFirst() == chunk.xPosition && t.getSecond() == chunk.zPosition)) {
                return entry.getValue().chunks;
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

    public static Plot getPlotNumberBasedOnLocation(BlockPos pos) {
        Chunk chunk = mc.theWorld.getChunkFromBlockCoords(pos);
        for (Map.Entry<Integer, Plot> entry : PLOTS.entrySet()) {
            if (entry.getValue().chunks.stream().anyMatch(t -> t.getFirst() == chunk.xPosition && t.getSecond() == chunk.zPosition)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static Plot getPlotNumberBasedOnLocation() {
        AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
        // get blockpos in the middle of the player's bounding box
        BlockPos pos = new BlockPos(playerBoundingBox.minX + (playerBoundingBox.maxX - playerBoundingBox.minX) / 2, playerBoundingBox.minY, playerBoundingBox.minZ + (playerBoundingBox.maxZ - playerBoundingBox.minZ) / 2);
        return getPlotNumberBasedOnLocation(pos);
    }

    public static Plot getPlotBasedOnNumber(int plotNumber) {
        return PLOTS.get(plotNumber);
    }

    public static List<Tuple<Integer, Integer>> getPlotChunksBasedOnNumber(int plotNumber) {
        return PLOTS.get(plotNumber).chunks;
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
        List<Tuple<Integer, Integer>> chunks = getPlotChunksBasedOnNumber(plotNumber);
        int minX = chunks.get(0).getFirst() * 16;
        int maxX = chunks.get(chunks.size() - 1).getFirst() * 16;
        int minZ = chunks.get(0).getSecond() * 16;
        int maxZ = chunks.get(chunks.size() - 1).getSecond() * 16;
        float centerX = (float) (minX + (maxX - minX) / 2);
        float centerZ = (float) (minZ + (maxZ - minZ) / 2);
        return new BlockPos(centerX, 80, centerZ);
    }

    public static class Plot {
        public Integer number;
        public String name;
        List<Tuple<Integer, Integer>> chunks;

        public Plot(String name) {
            this.name = name;
            this.chunks = new ArrayList<>();
        }

        public static Plot of(String name) {
            return new Plot(name);
        }

        public Plot addAll(List<Tuple<Integer, Integer>> chunks) {
            this.chunks.addAll(chunks);
            return this;
        }

        public Plot setName(String name) {
            this.name = name;
            return this;
        }

        public Plot setNumber(int number) {
            this.number = number;
            return this;
        }
    }

    public static void savePlots() {
        JsonObject plotsJson = new JsonObject();
        for (Map.Entry<Integer, Plot> entry : PLOTS.entrySet()) {
            plotsJson.addProperty(String.valueOf(entry.getKey()), entry.getValue().name);
        }
        try {
            Files.write(plotsFile.toPath(), FarmHelper.gson.toJson(plotsJson).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPlot(int number, String name) {
        PLOTS.get(number).name = name;
    }

    public static boolean needToUpdatePlots() {
        return PLOTS.values().stream().anyMatch(plot -> plot.name.isEmpty());
    }
}
