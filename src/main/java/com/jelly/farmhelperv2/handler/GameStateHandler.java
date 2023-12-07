package com.jelly.farmhelperv2.handler;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.impl.AutoRepellent;
import com.jelly.farmhelperv2.feature.impl.Failsafe;
import com.jelly.farmhelperv2.mixin.gui.IGuiPlayerTabOverlayAccessor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Timer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameStateHandler {
    private static GameStateHandler INSTANCE;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");
    private final Timer notMovingTimer = new Timer();
    private final Timer reWarpTimer = new Timer();
    @Getter
    private final Clock jacobContestLeftClock = new Clock();
    private final Pattern jacobsRemainingTimePattern = Pattern.compile("([0-9]|[1-2][0-9])m([0-9]|[1-5][0-9])s");
    private final Pattern serverClosingPattern = Pattern.compile("Server closing: (?<minutes>\\d+):(?<seconds>\\d+) .*");
    @Getter
    private Location lastLocation = Location.TELEPORTING;
    @Getter
    private Location location = Location.TELEPORTING;
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
    private long randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
    private long randomRewarpValueToWait = FarmHelperConfig.getRandomRewarpDelay();
    @Getter
    private BuffState cookieBuffState = BuffState.UNKNOWN;
    @Getter
    private BuffState godPotState = BuffState.UNKNOWN;
    @Getter
    private BuffState pestRepellentState = BuffState.UNKNOWN;
    @Getter
    private double currentPurse = 0;
    @Getter
    private double previousPurse = 0;
    @Getter
    private long bits = 0;
    @Getter
    private long copper = 0;
    @Getter
    private int currentPlot = 0;
    @Getter
    private Optional<FarmHelperConfig.CropEnum> jacobsContestCrop = Optional.empty();
    @Getter
    private int jacobsContestCropNumber = 0;
    @Getter
    private JacobMedal jacobMedal = JacobMedal.NONE;
    private long randomValueToWaitNextTime = -1;
    @Getter
    @Setter
    private boolean wasInJacobContest = false;
    @Getter
    @Setter
    private Optional<Integer> serverClosingSeconds = Optional.empty();

    public static GameStateHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameStateHandler();
        }
        return INSTANCE;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        lastLocation = location;
        location = Location.TELEPORTING;
    }


    @SubscribeEvent
    public void onTickCheckCoins(TickEvent.PlayerTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        List<String> scoreboardLines = ScoreboardUtils.getScoreboardLines();
        if (scoreboardLines.isEmpty()) return;

        if (inGarden()) {
            currentPlot = PlotUtils.getPlotNumberBasedOnLocation();
        } else {
            currentPlot = -5;
        }

        for (String line : scoreboardLines) {
            String cleanedLine = StringUtils.stripControlCodes(ScoreboardUtils.cleanSB(line));
            Matcher serverClosingMatcher = serverClosingPattern.matcher(StringUtils.stripControlCodes(ScoreboardUtils.cleanSB(line)));
            if (serverClosingMatcher.find()) {
                int minutes = Integer.parseInt(serverClosingMatcher.group("minutes"));
                int seconds = Integer.parseInt(serverClosingMatcher.group("seconds"));
                serverClosingSeconds = Optional.of(minutes * 60 + seconds);
            } else {
                serverClosingSeconds = Optional.empty();
            }
            if (cleanedLine.contains("Purse:") || cleanedLine.contains("Piggy:")) {
                try {
                    String stringPurse = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringPurse.contains("(+")) {
                        stringPurse = stringPurse.substring(0, stringPurse.indexOf("("));
                    }
                    long tempCurrentPurse = Long.parseLong(stringPurse);
                    previousPurse = currentPurse;
                    currentPurse = tempCurrentPurse;
                } catch (NumberFormatException ignored) {
                }
            } else if (cleanedLine.contains("Bits:")) {
                try {
                    String stringBits = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringBits.contains("(+")) {
                        stringBits = stringBits.substring(0, stringBits.indexOf("("));
                    }
                    bits = Long.parseLong(stringBits);
                } catch (NumberFormatException ignored) {
                }
            } else if (cleanedLine.contains("Copper:")) {
                try {
                    String stringCopper = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringCopper.contains("(+")) {
                        stringCopper = stringCopper.substring(0, stringCopper.indexOf("("));
                    }
                    copper = Long.parseLong(stringCopper);
                } catch (NumberFormatException ignored) {
                }
            }
            if (inJacobContest()) {
                try {
                    if (cleanedLine.contains("with") || cleanedLine.startsWith("Collected")) {
                        jacobsContestCropNumber = Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
                    }
                } catch (NumberFormatException ignored) {
                    jacobsContestCropNumber = 0;
                }
                if (!jacobContestLeftClock.isScheduled()) {
                    Matcher matcher = jacobsRemainingTimePattern.matcher(cleanedLine);
                    if (matcher.find()) {
                        if (cleanedLine.contains("Wheat")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.WHEAT);
                        } else if (cleanedLine.contains("Carrot")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.CARROT);
                        } else if (cleanedLine.contains("Potato")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.POTATO);
                        } else if (cleanedLine.contains("Nether") || cleanedLine.contains("Wart")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.NETHER_WART);
                        } else if (cleanedLine.contains("Sugar") || cleanedLine.contains("Cane")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.SUGAR_CANE);
                        } else if (cleanedLine.contains("Mushroom")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.MUSHROOM);
                        } else if (cleanedLine.contains("Melon")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.MELON);
                        } else if (cleanedLine.contains("Pumpkin")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.PUMPKIN);
                        } else if (cleanedLine.contains("Cocoa") || cleanedLine.contains("Bean")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.COCOA_BEANS);
                        } else if (cleanedLine.contains("Cactus")) {
                            jacobsContestCrop = Optional.of(FarmHelperConfig.CropEnum.CACTUS);
                        }

                        String minutes = matcher.group(1);
                        String seconds = matcher.group(2);
                        jacobContestLeftClock.schedule((Long.parseLong(minutes) * 60 + Long.parseLong(seconds)) * 1_000L);
                    }
                }
                if (cleanedLine.contains("BRONZE with")) {
                    jacobMedal = JacobMedal.BRONZE;
                } else if (cleanedLine.contains("SILVER with")) {
                    jacobMedal = JacobMedal.SILVER;
                } else if (cleanedLine.contains("GOLD with")) {
                    jacobMedal = JacobMedal.GOLD;
                } else if (cleanedLine.contains("PLATINUM with")) {
                    jacobMedal = JacobMedal.PLATINUM;
                } else if (cleanedLine.contains("DIAMOND with")) {
                    jacobMedal = JacobMedal.DIAMOND;
                }
            } else {
                jacobsContestCrop = Optional.empty();
                jacobsContestCropNumber = 0;
                jacobMedal = JacobMedal.NONE;
            }
        }
    }

    @SubscribeEvent
    public void onTickCheckBuffs(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        boolean foundGodPotBuff = false;
        boolean foundCookieBuff = false;
        boolean foundPestRepellent = false;
        boolean loaded = false;

        IGuiPlayerTabOverlayAccessor tabOverlay = (IGuiPlayerTabOverlayAccessor) mc.ingameGUI.getTabList();
        if (tabOverlay == null) return;
        IChatComponent footer = tabOverlay.getFooter();
        if (footer == null || footer.getFormattedText().isEmpty()) return;
        String[] footerString = footer.getFormattedText().split("\n");

        for (String line : footerString) {
            String unformattedLine = StringUtils.stripControlCodes(line);
            if (unformattedLine.contains("Active Effects")) {
                loaded = true;
            }
            if (unformattedLine.contains("You have a God Potion active!")) {
                foundGodPotBuff = true;
                continue;
            }
            if (unformattedLine.contains("Pest Repellant") || unformattedLine.contains("Pest Repellent")) {
                foundPestRepellent = true;
                continue;
            }
            if (unformattedLine.contains("Cookie Buff")) {
                foundCookieBuff = true;
                continue;
            }
            if (foundCookieBuff) {
                if (unformattedLine.contains("Not active")) {
                    foundCookieBuff = false;
                }
                break;
            }
        }

        if (!loaded) {
            cookieBuffState = BuffState.UNKNOWN;
            godPotState = BuffState.UNKNOWN;
            pestRepellentState = BuffState.UNKNOWN;
            return;
        }

        cookieBuffState = foundCookieBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
        godPotState = foundGodPotBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
        pestRepellentState = foundPestRepellent ? BuffState.ACTIVE : (!AutoRepellent.repellentFailsafeClock.passed() ? BuffState.FAILSAFE : BuffState.NOT_ACTIVE);
    }

    @SubscribeEvent
    public void onTickCheckLocation(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }

        if (TablistUtils.getTabList().size() == 1 && ScoreboardUtils.getScoreboardLines().isEmpty() && PlayerUtils.isInventoryEmpty(mc.thePlayer)) {
            lastLocation = location;
            location = Location.LIMBO;
            return;
        }

        for (String line : TablistUtils.getTabList()) {
            Matcher matcher = areaPattern.matcher(line);
            if (matcher.find()) {
                String area = matcher.group(1);
                for (Location island : Location.values()) {
                    if (area.equals(island.getName())) {
                        lastLocation = location;
                        location = island;
                        return;
                    }
                }
            }
        }

        if (!ScoreboardUtils.getScoreboardTitle().contains("SKYBLOCK") && !ScoreboardUtils.getScoreboardLines().isEmpty() && ScoreboardUtils.cleanSB(ScoreboardUtils.getScoreboardLines().get(0)).contains("www.hypixel.net")) {
            lastLocation = location;
            location = Location.LOBBY;
            return;
        }
        if (location != Location.TELEPORTING) {
            lastLocation = location;
        }
        location = Location.TELEPORTING;
    }

    @SubscribeEvent
    public void onTickCheckMoving(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;


        dx = Math.abs(mc.thePlayer.posX - mc.thePlayer.lastTickPosX);
        dz = Math.abs(mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ);
        dy = Math.abs(mc.thePlayer.posY - mc.thePlayer.lastTickPosY);

        if (notMoving() && mc.currentScreen == null) {
            if (hasPassedSinceStopped() && !PlayerUtils.isStandingOnRewarpLocation()) {
                if (Failsafe.getInstance().hasDirtBlocks() && Failsafe.getInstance().isTouchingDirtBlock()) {
                    Failsafe.getInstance().addEmergency(Failsafe.EmergencyType.DIRT_CHECK);
                } else {
                    randomValueToWaitNextTime = -1;
                    notMovingTimer.reset();
                }
            }
        } else {
            if (!notMovingTimer.isScheduled())
                randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
            notMovingTimer.schedule();
        }
        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().isPresent()) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().get();
        } else {
            yaw = mc.thePlayer.rotationYaw;
        }
        if (MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.CACTUS) {
            frontWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.FORWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, 1, yaw), BlockUtils.Direction.FORWARD) && BlockUtils.getRelativeBlock(0, 0, 2, yaw) != Blocks.cactus;
            backWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.BACKWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, -1, yaw), BlockUtils.Direction.BACKWARD) && BlockUtils.getRelativeBlock(0, 0, -2, yaw) != Blocks.cactus;
        } else {
            frontWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.FORWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, 1, yaw), BlockUtils.Direction.FORWARD);
            backWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.BACKWARD) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, -1, yaw), BlockUtils.Direction.BACKWARD);
        }
        rightWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.RIGHT) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(1, 0, 0, yaw), BlockUtils.Direction.RIGHT);
        leftWalkable = BlockUtils.canWalkThroughDoor(BlockUtils.Direction.LEFT) && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-1, 0, 0, yaw), BlockUtils.Direction.LEFT);
    }

    @SubscribeEvent
    public void onTickCheckRewarp(TickEvent.ClientTickEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!MacroHandler.getInstance().isMacroToggled()) return;

        if (PlayerUtils.isStandingOnRewarpLocation()) {
            if (!reWarpTimer.isScheduled()) {
                reWarpTimer.schedule();
            }
        } else {
            reWarpTimer.reset();
        }
    }

    public boolean canRewarp() {
        return reWarpTimer.hasPassed(randomRewarpValueToWait);
    }

    public void scheduleRewarp() {
        randomRewarpValueToWait = FarmHelperConfig.getRandomRewarpDelay();
        reWarpTimer.reset();
    }

    public boolean hasPassedSinceStopped() {
        return notMovingTimer.hasPassed(randomValueToWaitNextTime != -1 ? randomValueToWaitNextTime : randomValueToWait);
    }

    public boolean notMoving() {
        return (dx < 0.01 && dz < 0.01 && dy < 0.01 && mc.currentScreen == null) || (!holdingKeybindIsWalkable() && mc.thePlayer != null && mc.thePlayer.isPushedByWater() && mc.thePlayer.isInWater()) || RotationHandler.getInstance().isRotating();
    }

    public boolean holdingKeybindIsWalkable() {
        KeyBinding[] holdingKeybinds = KeyBindUtils.getHoldingKeybinds();
        for (KeyBinding key : holdingKeybinds) {
            if (key != null && key.isKeyDown()) {
                if (key == mc.gameSettings.keyBindForward && !frontWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindBack && !backWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindRight && !rightWalkable) {
                    return false;
                } else if (key == mc.gameSettings.keyBindLeft && !leftWalkable) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canChangeDirection() {
        return !notMovingTimer.isScheduled();
    }

    public void scheduleNotMoving(int time) {
        randomValueToWaitNextTime = time;
        notMovingTimer.schedule();
    }

    public void scheduleNotMoving() {
        randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
        notMovingTimer.schedule();
    }

    public boolean inGarden() {
        return location != Location.TELEPORTING && location == Location.GARDEN;
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

    @Getter
    public enum Location {
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

    public enum BuffState {
        ACTIVE,
        FAILSAFE,
        NOT_ACTIVE,
        UNKNOWN
    }

    private enum JacobMedal {
        NONE,
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        DIAMOND
    }
}
