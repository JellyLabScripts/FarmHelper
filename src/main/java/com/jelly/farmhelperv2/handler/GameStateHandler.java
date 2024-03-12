package com.jelly.farmhelperv2.handler;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.failsafe.impl.DirtFailsafe;
import com.jelly.farmhelperv2.feature.impl.AutoRepellent;
import com.jelly.farmhelperv2.mixin.gui.IGuiPlayerTabOverlayAccessor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Timer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
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
    public final Pattern jacobsRemainingTimePattern = Pattern.compile("([0-9]|[1-2][0-9])m([0-9]|[1-5][0-9])s");
    private final Pattern serverClosingPattern = Pattern.compile("Server closing: (?<minutes>\\d+):(?<seconds>\\d+) .*");
    private final Pattern pestsFromVacuumPattern = Pattern.compile("Vacuum Bag: (\\d+) Pest(s)?");
    @Getter
    private Location lastLocation = Location.TELEPORTING;
    @Getter
    private Location location = Location.TELEPORTING;

    private boolean isInJacobContest = false;
    private boolean isGuestOnGarden = false;
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
    private BuffState pestHunterBonus = BuffState.UNKNOWN;
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
    @Getter
    private int speed = 0;

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
    public void onTickCheckGameState(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.theWorld == null || mc.thePlayer == null) {
            isGuestOnGarden = false;
            return;
        }

        List<String> cleanScoreboardLines = ScoreboardUtils.getScoreboardLines(true);
        List<String> tabList = TablistUtils.getTabList();

        onTickCheckCoins(cleanScoreboardLines);
        onTickCheckLocation(cleanScoreboardLines, tabList);
        checkGuestOnGarden(tabList);
        onTickCheckPlot();
        onTickCheckBuffs();
        onTickCheckSpeed();
        onTickCheckMoving();
        onTickCheckRewarp();
    }

    public void onTickCheckCoins(List<String> scoreboardLines) {
        boolean foundJacobContest = false;
        for (String cleanedLine : scoreboardLines) {
            Matcher serverClosingMatcher = serverClosingPattern.matcher(cleanedLine);
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
                } catch (Exception ignored) {
                }
            } else if (cleanedLine.contains("Bits:")) {
                try {
                    String stringBits = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringBits.contains("(+")) {
                        stringBits = stringBits.substring(0, stringBits.indexOf("("));
                    }
                    bits = Long.parseLong(stringBits);
                } catch (Exception ignored) {
                }
            } else if (cleanedLine.contains("Copper:")) {
                try {
                    String stringCopper = cleanedLine.split(" ")[1].replace(",", "").trim();
                    if (stringCopper.contains("(+")) {
                        stringCopper = stringCopper.substring(0, stringCopper.indexOf("("));
                    }
                    copper = Long.parseLong(stringCopper);
                } catch (Exception ignored) {
                }
            } else if (cleanedLine.toLowerCase().contains("jacob's contest")) {
                isInJacobContest = true;
                foundJacobContest = true;
            }
            if (inJacobContest()) {
                try {
                    if (cleanedLine.contains("with") || cleanedLine.startsWith("Collected")) {
                        jacobsContestCropNumber = Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
                    }
                } catch (Exception ignored) {
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
        if (!foundJacobContest) {
            isInJacobContest = false;
        }
    }

    public void onTickCheckLocation(List<String> scoreboardLines, List<String> tabList) {
        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }

        if (tabList.size() == 1 && scoreboardLines.isEmpty() && PlayerUtils.isInventoryEmpty(mc.thePlayer)) {
            lastLocation = location;
            location = Location.LIMBO;
            return;
        }

        for (String line : tabList) {
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

        if (!ScoreboardUtils.getScoreboardTitle().contains("SKYBLOCK") && !scoreboardLines.isEmpty() && scoreboardLines.get(0).contains("www.hypixel.net")) {
            lastLocation = location;
            location = Location.LOBBY;
            return;
        }
        if (location != Location.TELEPORTING) {
            lastLocation = location;
        }
        location = Location.TELEPORTING;
    }

    private void checkGuestOnGarden(List<String> tabList) {
        boolean hasGuestsOnTabList = false;
        for (String name : tabList) {
            if (name.contains("Guests "))
                hasGuestsOnTabList = true;
            if (name.contains("Guests (0)")) {
                isGuestOnGarden = false;
            }
        }
        isGuestOnGarden = hasGuestsOnTabList && location == Location.GARDEN;
    }

    public void onTickCheckPlot() {
        if (inGarden()) {
            currentPlot = PlotUtils.getPlotNumberBasedOnLocation();
        } else {
            currentPlot = -5;
        }
    }

    public void onTickCheckBuffs() {
        boolean foundGodPotBuff = false;
        boolean foundCookieBuff = false;
        boolean foundPestRepellent = false;
        boolean foundPestHunterBonus = false;
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
                if (!AutoRepellent.repellentFailsafeClock.isScheduled()) {
                    String[] split = unformattedLine.split(" ");
                    String time = split[split.length - 1];
                    if (time.contains("m")) {
                        AutoRepellent.repellentFailsafeClock.schedule(Long.parseLong(time.replace("m", "").trim()) * 60 * 1_000L);
                    } else {
                        AutoRepellent.repellentFailsafeClock.schedule(Long.parseLong(time.replace("s", "")) * 1_000L);
                    }
                    LogUtils.sendDebug("Scheduled auto repellent failsafe clock for " + time);
                }
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
            if (unformattedLine.contains("Bonus: +")) {
                foundPestHunterBonus = true;
                break;
            }
        }

        if (!loaded) {
            cookieBuffState = BuffState.UNKNOWN;
            godPotState = BuffState.UNKNOWN;
            pestRepellentState = BuffState.UNKNOWN;
            pestHunterBonus = BuffState.UNKNOWN;
            return;
        }

        cookieBuffState = foundCookieBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
        godPotState = foundGodPotBuff ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
        pestRepellentState = foundPestRepellent ? BuffState.ACTIVE : (!AutoRepellent.repellentFailsafeClock.passed() ? BuffState.FAILSAFE : BuffState.NOT_ACTIVE);
        pestHunterBonus = foundPestHunterBonus ? BuffState.ACTIVE : BuffState.NOT_ACTIVE;
    }

    public void onTickCheckSpeed() {
        float speed = mc.thePlayer.capabilities.getWalkSpeed();
        this.speed = (int) (speed * 1_000);
    }

    public void onTickCheckMoving() {
        dx = Math.abs(mc.thePlayer.motionX);
        dy = Math.abs(mc.thePlayer.motionY);
        dz = Math.abs(mc.thePlayer.motionZ);

        if (notMoving() && mc.currentScreen == null) {
            if (hasPassedSinceStopped() && !PlayerUtils.isStandingOnRewarpLocation()) {
                if (DirtFailsafe.getInstance().hasDirtBlocks() && DirtFailsafe.getInstance().isTouchingDirtBlock()) {
                    FailsafeManager.getInstance().possibleDetection(DirtFailsafe.getInstance());
                } else {
                    if (notMovingTimer.isScheduled()) {
                        randomValueToWaitNextTime = -1;
                        notMovingTimer.reset();
                        randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
                    }
                }
            }
        } else {
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

    public void onTickCheckRewarp() {
        if (!MacroHandler.getInstance().isMacroToggled()) return;

        if (PlayerUtils.isStandingOnRewarpLocation()) {
            if (!reWarpTimer.isScheduled()) {
                reWarpTimer.schedule();
            }
        } else {
            reWarpTimer.reset();
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            ItemStack slot = packet.func_149174_e();
            if (slot == null || slot.getItem() == null || (!(slot.getItem() instanceof ItemHoe) && !(slot.getItem() instanceof ItemAxe)))
                return;
            long cult = getCultivating(slot);
            if (cult == 0) return;
            currentCultivating.put(slot.getDisplayName(), cult);
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
        if ((dx < 0.01 && dz < 0.01 && dyIsRest() && mc.currentScreen == null)) {
            return true;
        }
        return !holdingKeybindIsWalkable() && (playerIsInFlowingWater(0) || playerIsInFlowingWater(1)) && mc.thePlayer.isInWater();
    }

    private boolean dyIsRest() {
        return dy < 0.05 || dy <= 0.079 && dy >= 0.078; // weird calculation of motionY being -0.0784000015258789 while resting at block and 0.0 is while flying for some reason
    }

    public boolean playerIsInFlowingWater(int y) {
        IBlockState state = mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0, y, 0));
        if (!state.getBlock().equals(Blocks.water)) return false;
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level == 0) {
            if (FarmHelperConfig.alwaysHoldW) {
                return false;
            }
            double motionX = mc.thePlayer.motionX;
            double motionZ = mc.thePlayer.motionZ;
            return motionX > 0.01 || motionX < -0.01 || motionZ > 0.01 || motionZ < -0.01;
        } else {
            return true;
        }
    }

    public boolean holdingKeybindIsWalkable() {
        KeyBinding[] holdingKeybinds = KeyBindUtils.getHoldingKeybinds();
        for (KeyBinding key : holdingKeybinds) {
            if (key != null && key.isKeyDown()) {
                if (key == mc.gameSettings.keyBindForward && !frontWalkable && !FarmHelperConfig.alwaysHoldW) {
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
        if (FarmHelperConfig.jacobContestCurrentCropsOnly) {
            return isInJacobContest && jacobsContestCrop.isPresent() && jacobsContestCrop.get() == MacroHandler.getInstance().getCrop();
        } else {
            return isInJacobContest;
        }
    }

    public boolean isGuestOnGarden() {
        return isGuestOnGarden;
    }

    public int getPestsFromVacuum() {
        if (mc.theWorld == null || mc.thePlayer == null)
            return 0;
        for (Slot slot : mc.thePlayer.inventoryContainer.inventorySlots) {
            if (!slot.getHasStack())
                continue;
            ItemStack stack = slot.getStack();
            if (stack == null)
                continue;
            String itemName = StringUtils.stripControlCodes(stack.getDisplayName());
            if (itemName == null || !itemName.toLowerCase().contains("vacuum"))
                continue;
            ArrayList<String> lore = InventoryUtils.getItemLore(stack);
            if (lore.isEmpty())
                continue;
            for (String line : lore) {
                if (line.contains("Vacuum Bag:")) {
                    Matcher matcher = pestsFromVacuumPattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            return Integer.parseInt(matcher.group(1));
                        } catch (Exception e) {
                            LogUtils.sendError("Failed to parse pests from vacuum bag!");
                            LogUtils.sendWarning("Report this to #bug-reports!");
                            return 0;
                        }
                    }
                }
            }
        }
        return 0;
    }

    @Getter
    private HashMap<String, Long> currentCultivating = new HashMap<>();

    public Long getCultivating(ItemStack item) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return 0L;
        NBTTagCompound tag = item.getTagCompound();
        if (tag == null)
            return 0L;
        if (tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound ea = tag.getCompoundTag("ExtraAttributes");

            if (ea.hasKey("farmed_cultivating", 99)) {
                return (long) ea.getInteger("farmed_cultivating");
            }
        }
        return 0L;
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
