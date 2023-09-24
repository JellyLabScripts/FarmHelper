package com.github.may2beez.farmhelperv2.handler;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.util.PlayerUtils;
import com.github.may2beez.farmhelperv2.util.ScoreboardUtils;
import com.github.may2beez.farmhelperv2.util.TablistUtils;
import com.github.may2beez.farmhelperv2.util.helper.Timer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameStateHandler {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static GameStateHandler INSTANCE;
    public static GameStateHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameStateHandler();
        }
        return INSTANCE;
    }

    @Getter
    enum Location {
        PRIVATE_ISLAND("Private Island"),
        HUB("Hub"),
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
        DUNGEON("Dungeon"),
        TELEPORTING("Teleporting");

        private final String name;

        Location(String name) {
            this.name = name;
        }
    }

    @Getter
    private Location location = Location.TELEPORTING;
    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");

    @Getter
    private boolean frontWalkable;
    @Getter
    private boolean rightWalkable;
    @Getter
    private boolean backWalkable;
    @Getter
    private boolean leftWalkable;

    @Getter
    private double dx;
    @Getter
    private double dz;
    @Getter
    private double dy;

    @Getter
    private String serverIP;
    private final Timer notMovingTimer = new Timer();
    private long randomValueToWait = (long) (Math.random() * (FarmHelperConfig.maxTimeBetweenChangingRows - FarmHelperConfig.minTimeBetweenChangingRows) + FarmHelperConfig.minTimeBetweenChangingRows);

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        location = Location.TELEPORTING;
    }

    @SubscribeEvent
    public void onTickCheckLocation(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }

        if (TablistUtils.getTabList().size() == 1 && ScoreboardUtils.getScoreboardLines().isEmpty() && PlayerUtils.isInventoryEmpty(mc.thePlayer)) {
            location = Location.LIMBO;
            return;
        }

        for (String line : TablistUtils.getTabList()) {
            Matcher matcher = areaPattern.matcher(line);
            if (matcher.find()) {
                String area = matcher.group(1);
                for (Location island : Location.values()) {
                    if (area.equals(island.getName())) {
                        location = island;
                        return;
                    }
                }
            }
        }

        location = Location.LOBBY;
    }

    @SubscribeEvent
    public void onTickCheckMoving(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
        dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
        dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);

        if (dx < 0.01 && dz < 0.01 && dy < 0.01 && mc.currentScreen == null) {
            if (hasPassedSinceStopped()) {
                notMovingTimer.reset();
            }
        } else {
            if (!notMovingTimer.isScheduled())
                newRandomValueToWait();
            notMovingTimer.schedule();
        }
    }

    public boolean hasPassedSinceStopped() {
        return notMovingTimer.hasPassed(randomValueToWait);
    }

    public boolean canChangeDirection() {
        return !notMovingTimer.isScheduled();
    }
    public void scheduleNotMoving(int time) {
        randomValueToWait = time;
        notMovingTimer.schedule();
    }
    public void scheduleNotMoving() {
        newRandomValueToWait();
        notMovingTimer.schedule();
    }
    public void newRandomValueToWait() {
        randomValueToWait = (long) (Math.random() * (FarmHelperConfig.maxTimeBetweenChangingRows - FarmHelperConfig.minTimeBetweenChangingRows) + FarmHelperConfig.minTimeBetweenChangingRows);
    }
    public boolean inGarden() {
        return location == Location.GARDEN;
    }

}
