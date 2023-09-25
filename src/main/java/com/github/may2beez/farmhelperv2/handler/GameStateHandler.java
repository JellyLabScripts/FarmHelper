package com.github.may2beez.farmhelperv2.handler;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.util.BlockUtils;
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
    private long randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();

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

        if (notMoving()) {
            if (hasPassedSinceStopped()) {
                notMovingTimer.reset();
            }
        } else {
            if (!notMovingTimer.isScheduled())
                randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
            notMovingTimer.schedule();
        }
        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg() != -1337) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg();
        } else {
            yaw = mc.thePlayer.rotationYaw;
        }
        frontWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, 1, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 1, 1, yaw)));
        rightWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 0, 0, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(1, 1, 0, yaw)));
        backWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 0, -1, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(0, 1, -1, yaw)));
        leftWalkable = (BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 0, 0, yaw)) && BlockUtils.isWalkable(BlockUtils.getRelativeBlock(-1, 1, 0, yaw)));
    }

    public boolean hasPassedSinceStopped() {
        return notMovingTimer.hasPassed(randomValueToWait);
    }

    public boolean notMoving() {
        return dx < 0.01 && dz < 0.01 && dy < 0.01 && mc.currentScreen == null;
    }

    public boolean canChangeDirection() {
        return !notMovingTimer.isScheduled();
    }
    public void scheduleNotMoving(int time) {
        randomValueToWait = time;
        notMovingTimer.schedule();
    }
    public void scheduleNotMoving() {
        randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
        notMovingTimer.schedule();
    }
    public boolean inGarden() {
        return location == Location.GARDEN;
    }

    public boolean inJacobContest() {
        for (String line : ScoreboardUtils.getScoreboardLines()) {
            String cleanedLine = ScoreboardUtils.cleanSB(line);
            if ((cleanedLine.toLowerCase()).contains("jacob's contest")) {
                return true;
            }
        }
        return false;
    }
}
