package com.jelly.farmhelper.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.SerializationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationUtils {
    private final Gson gson = new Gson();
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum Island {
        PRIVATE_ISLAND("Private Island"),
        THE_HUB("Hub"),
        THE_PARK("The Park"),
        THE_FARMING_ISLANDS("The Farming Islands"),
        SPIDER_DEN("Spider's Den"),
        THE_END("The End"),
        CRIMSON_ISLE("Crimson Isle"),
        GOLD_MINE("Gold Mine"),
        DEEP_CAVERNS("Deep Caverns"),
        DWARVEN_MINES("Dwarven Mines"),
        CRYSTAL_HOLLOWS("Crystal Hollows"),
        JERRY_WORKSHOP("Jerry's Workshop"),
        DUNGEON_HUB("Dungeon Hub"),
        LIMBO("UNKNOWN"),
        LOBBY("PROTOTYPE"),
        GARDEN("Garden"),
        DUNGEON("Dungeon");

        private final String name;

        public String getName() {
            return name;
        }

        Island(String name) {
            this.name = name;
        }
    }

    public static Island currentIsland;

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        // Check locraw message
        if (event.message == null) return;
        String unformatted = event.message.getUnformattedText();
        if (!unformatted.startsWith("{") || !unformatted.endsWith("}")) return;

        try {
            JsonObject obj = gson.fromJson(unformatted, JsonObject.class);
            if (obj.has("server") && obj.getAsJsonPrimitive("server").getAsString().equals("limbo")) {
                currentIsland = Island.LIMBO;
                return;
            }
            if (obj.getAsJsonPrimitive("gametype").getAsString().equals("MAIN")
                    || obj.getAsJsonPrimitive("gametype").getAsString().equals("PROTOTYPE")) {
                currentIsland = Island.LOBBY;
                return;
            }
            if (obj.has("map")) {
                for (Island island : Island.values()) {
                    if (obj.getAsJsonPrimitive("map").toString().equals(island.getName())) {
                        currentIsland = island;
                        return;
                    }
                }
            }
        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        currentIsland = null;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        currentIsland = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        // Check tab list
        Pattern pattern = Pattern.compile("Area:\\s(.+)");
        for (String line : TablistUtils.getTabList()) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String area = matcher.group(1);
                for (Island island : Island.values()) {
                    if (area.equals(island.getName())) {
                        currentIsland = island;
                        return;
                    }
                }
            }
        }

        // Check scoreboard
        if (!ScoreboardUtils.getScoreboardLines().isEmpty()) {
            if (ScoreboardUtils.getScoreboardDisplayName(1).contains("PROTOTYPE")) {
                currentIsland = Island.LOBBY;
                return;
            }
            for (String line : ScoreboardUtils.getScoreboardLines()) {
                String cleanedLine = ScoreboardUtils.cleanSB(line);
                if (cleanedLine.contains("Lobby: ")) {
                    currentIsland = Island.LOBBY;
                    return;
                } else if (cleanedLine.contains("Island")) {
                    currentIsland = Island.PRIVATE_ISLAND;
                    return;
                } else if (cleanedLine.contains("Garden") || cleanedLine.contains("Plot:")) {
                    currentIsland = Island.GARDEN;
                    return;
                }
            }
        }

        if (TablistUtils.getTabList().size() == 1 && ScoreboardUtils.getScoreboardLines().isEmpty() && PlayerUtils.isInventoryEmpty(mc.thePlayer)) {
            currentIsland = Island.LIMBO;
        }
    }
}
