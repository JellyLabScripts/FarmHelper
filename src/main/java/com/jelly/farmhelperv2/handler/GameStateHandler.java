package com.jelly.farmhelperv2.handler;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.*;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.failsafe.impl.CobwebFailsafe;
import com.jelly.farmhelperv2.failsafe.impl.DirtFailsafe;
import com.jelly.farmhelperv2.failsafe.impl.JacobFailsafe;
import com.jelly.farmhelperv2.feature.impl.AutoRepellent;
import com.jelly.farmhelperv2.feature.impl.PestsDestroyer;
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
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
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
    public final Pattern jacobsStartsInTimePattern = Pattern.compile("Starts In: ([1-3]?[0-9])?m ?([1-5]?[0-9])?s?");
    private final Pattern serverClosingPattern = Pattern.compile("Server closing: (?<minutes>\\d+):(?<seconds>\\d+) .*");
    private final Pattern pestsFromVacuumPattern = Pattern.compile("Vacuum Bag: (\\d+) Pest(s)?");
    @Getter
    private Location lastLocation = Location.TELEPORTING;
    @Getter
    private Location location = Location.TELEPORTING;
    @Getter
    private long lastTimeInGarden = -1;

    private boolean isInJacobContest = false;
    private boolean isGuestInGarden = false;
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
    private long randomValueToWait = -1;
    private long randomRewarpValueToWait = -1;
    @Getter
    private BuffState cookieBuffState = BuffState.UNKNOWN;
    @Getter
    private BuffState godPotState = BuffState.UNKNOWN;
    @Getter
    @Setter
    private BuffState pestRepellentState = BuffState.UNKNOWN;
    @Getter
    private BuffState pestHunterBonus = BuffState.UNKNOWN;
    @Getter
    private BuffState sprayonatorState = BuffState.UNKNOWN;
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
    private List<Integer> infestedPlots = new ArrayList<>();
    @Getter
    private int pestsCount = 0;
    @Getter
    private int currentPlotPestsCount = 0;
    @Getter
    private Optional<FarmHelperConfig.CropEnum> jacobsContestCrop = Optional.empty();
    @Getter
    private List<FarmHelperConfig.CropEnum> jacobsContestNextCrop = new ArrayList<>();
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
    @Setter
    private boolean updatedState = false;

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
        serverClosingSeconds = Optional.empty();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
            serverIP = mc.getCurrentServerData().serverIP;
        }
    }

    @SubscribeEvent
    public void onTablistUpdate(UpdateTablistEvent event) {
        if (event.tablist.isEmpty()) return;
        List<String> tabList = new ArrayList<>(event.tablist);

        List<String> scoreboardLines = ScoreboardUtils.getScoreboardLines(true);
        if (PlayerUtils.isInventoryEmpty(mc.thePlayer) && scoreboardLines.isEmpty() && mc.thePlayer.experienceLevel == 0 && mc.thePlayer.dimension == 1) {
            lastLocation = location;
            location = Location.LIMBO;
            return;
        }

        boolean hasGuestsOnTabList = false;
        boolean foundPestHunterBonus = false;
        boolean foundLocation = false;
        boolean foundSpray = false;
        int nextJacobCropFound = -1;
        List<FarmHelperConfig.CropEnum> newJacobsContestNextCrop = new ArrayList<>();

        for (String cleanedLine : tabList) {
            if (cleanedLine.matches(areaPattern.pattern())) {
                Matcher matcher = areaPattern.matcher(cleanedLine);
                if (matcher.find()) {
                    String area = matcher.group(1);
                    for (Location island : Location.values()) {
                        if (area.equals(island.getName())) {
                            lastLocation = location;
                            location = island;
                            foundLocation = true;
                            break;
                        }
                    }
                    if (foundLocation) continue;
                }
            }
            if (!hasGuestsOnTabList) {
                if (checkGuestInGardenTabList(cleanedLine)) {
                    hasGuestsOnTabList = true;
                }
            }
            checkInfestedPlotsTabList(cleanedLine);
            if (!foundPestHunterBonus) {
                int retPestHunter = checkPestHunterBonusTabList(cleanedLine);
                if (retPestHunter == 1) {
                    pestHunterBonus = BuffState.ACTIVE;
                    foundPestHunterBonus = true;
                } else if (retPestHunter == 2) {
                    pestHunterBonus = BuffState.NOT_ACTIVE;
                    foundPestHunterBonus = true;
                }
            }
            if (nextJacobCropFound >= 0 && nextJacobCropFound < 3) { // Make sure only 3 crops are added and no irrelevant text are being scanned
                FarmHelperConfig.CropEnum crop = convertCrop(cleanedLine);
                if (crop != FarmHelperConfig.CropEnum.NONE && !newJacobsContestNextCrop.contains(crop))
                    newJacobsContestNextCrop.add(crop);
                nextJacobCropFound++;
            }
            if (nextJacobCropFound == 3) {
                jacobsContestNextCrop = newJacobsContestNextCrop;
            }
            if (cleanedLine.contains("Starts In")) {
                nextJacobCropFound = 0;
            }
            if(cleanedLine.startsWith(" Spray: ")){
                sprayonatorState = cleanedLine.endsWith("None") ? BuffState.NOT_ACTIVE : BuffState.ACTIVE;
                foundSpray = true;
            }
        }
        if (!foundPestHunterBonus) {
            pestHunterBonus = BuffState.UNKNOWN;
        }
        if(!foundSpray){
            sprayonatorState = BuffState.UNKNOWN;
        }
        if (foundLocation) return;

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

    @SubscribeEvent
    public void onUpdateScoreboardLine(UpdateScoreboardLineEvent event) {
        String updatedLine = event.getLine();
        checkServerClosing(updatedLine);
        checkCoins(updatedLine);
        checkJacob(updatedLine);
    }

    @SubscribeEvent
    public void onUpdateScoreboardList(UpdateScoreboardListEvent event) {
        checkCurrentPests(event.cleanScoreboardLines);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        onTickCheckMoving();
        onTickCheckRewarp();
        onTickCheckPlot();
        onTickCheckSpeed();
    }

    @SubscribeEvent
    public void onUpdateTabFooter(UpdateTablistFooterEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        List<String> footer = event.footer;
        checkBuffsTabList(footer);
    }

    private FarmHelperConfig.CropEnum convertCrop(String s) {
        if (s.contains("Wheat")) {
            return FarmHelperConfig.CropEnum.WHEAT;
        } else if (s.contains("Carrot")) {
            return FarmHelperConfig.CropEnum.CARROT;
        } else if (s.contains("Potato")) {
            return FarmHelperConfig.CropEnum.POTATO;
        } else if (s.contains("Nether") || s.contains("Wart")) {
            return FarmHelperConfig.CropEnum.NETHER_WART;
        } else if (s.contains("Sugar") || s.contains("Cane")) {
            return FarmHelperConfig.CropEnum.SUGAR_CANE;
        } else if (s.contains("Mushroom")) {
            return FarmHelperConfig.CropEnum.MUSHROOM;
        } else if (s.contains("Melon")) {
            return FarmHelperConfig.CropEnum.MELON;
        } else if (s.contains("Pumpkin")) {
            return FarmHelperConfig.CropEnum.PUMPKIN;
        } else if (s.contains("Cocoa") || s.contains("Bean")) {
            return FarmHelperConfig.CropEnum.COCOA_BEANS;
        } else if (s.contains("Cactus")) {
            return FarmHelperConfig.CropEnum.CACTUS;
        }
        return FarmHelperConfig.CropEnum.NONE;
    }

    private void checkJacob(String cleanedLine) {
        if (cleanedLine.toLowerCase().contains("jacob's contest") && !isInJacobContest) {
            isInJacobContest = true;
        }
        if (isInJacobContest) {
            try {
                if (cleanedLine.contains("with") || cleanedLine.startsWith("Collected")) {
                    jacobsContestCropNumber = Integer.parseInt(cleanedLine.substring(cleanedLine.lastIndexOf(" ") + 1).replace(",", ""));
                    return;
                }
            } catch (Exception ignored) {
                jacobsContestCropNumber = 0;
            }
            if (jacobContestLeftClock.isScheduled() && jacobContestLeftClock.passed()) {
                LogUtils.sendDebug("Jacob's contest left clock passed");
                jacobContestLeftClock.reset();
                isInJacobContest = false;
            }
            if (!jacobContestLeftClock.isScheduled() || !jacobsContestCrop.isPresent()) {
                Matcher matcher = jacobsRemainingTimePattern.matcher(cleanedLine);
                if (matcher.find()) {
                    FarmHelperConfig.CropEnum crop = convertCrop(cleanedLine);
                    if (crop != FarmHelperConfig.CropEnum.NONE)
                        jacobsContestCrop = Optional.of(crop);

                    String minutes = matcher.group(1);
                    String seconds = matcher.group(2);
                    jacobContestLeftClock.schedule((Long.parseLong(minutes) * 60 + Long.parseLong(seconds)) * 1_000L + 1_000L);
                    return;
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
            jacobContestLeftClock.reset();
            jacobMedal = JacobMedal.NONE;
        }
    }

    private void checkCoins(String cleanedLine) {
        if (cleanedLine.contains("Purse:") || cleanedLine.contains("Piggy:")) {
            if (cleanedLine.endsWith(",")) return;
            try {
                String stringPurse = cleanedLine.split(" ")[1].replace(",", "").trim();
                if (stringPurse.contains("(+")) {
                    stringPurse = stringPurse.substring(0, stringPurse.indexOf("("));
                }
                long tempCurrentPurse = Long.parseLong(stringPurse);
                if (previousPurse == 0) {
                    previousPurse = tempCurrentPurse;
                } else {
                    previousPurse = currentPurse;
                }
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
        }
    }

    private void checkServerClosing(String cleanedLine) {
        Matcher serverClosingMatcher = serverClosingPattern.matcher(cleanedLine);
        if (serverClosingMatcher.find()) {
            int minutes = Integer.parseInt(serverClosingMatcher.group("minutes"));
            int seconds = Integer.parseInt(serverClosingMatcher.group("seconds"));
            serverClosingSeconds = Optional.of(minutes * 60 + seconds);
        }
    }

    private void checkCurrentPests(List<String> list) {
        int pestsCountTemp = 0;
        for (String cleanedLine : list) {
            if (cleanedLine.contains("The Garden") && cleanedLine.contains("ൠ")) {
                try {
                    String[] split = cleanedLine.trim().split(" ");
                    int temp = Integer.parseInt(split[split.length - 1].trim().replace("x", ""));
                    int previousPestsCount = pestsCount;
                    pestsCount = temp;
                    pestsCountTemp = temp;
                    if (pestsCount > previousPestsCount && !PestsDestroyer.getInstance().isRunning() && pestsCount > FarmHelperConfig.startKillingPestsAt) {
                        if (FarmHelperConfig.sendWebhookLogIfPestsDetectionNumberExceeded) {
                            LogUtils.webhookLog("[Pests Destroyer]\\nThere " + (pestsCount > 1 ? "are" : "is") + " currently **" + pestsCount + "** " + (pestsCount > 1 ? "pests" : "pest") + " in the garden!", FarmHelperConfig.pingEveryoneOnPestsDetectionNumberExceeded);
                        }
                        if (FarmHelperConfig.sendNotificationIfPestsDetectionNumberExceeded) {
                            FailsafeUtils.getInstance().sendNotification("There " + (pestsCount > 1 ? "are" : "is") + " currently " + pestsCount + " " + (pestsCount > 1 ? "pests" : "pest") + " in the garden!", TrayIcon.MessageType.WARNING);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    pestsCount = 0;
                }
            }
            if (cleanedLine.contains("Plot") && cleanedLine.contains("x")) {
                String[] split = cleanedLine.trim().split(" ");
                String last = split[split.length - 1];
                try {
                    currentPlotPestsCount = Integer.parseInt(last.replace("x", ""));
                } catch (NumberFormatException ignored) {
                    currentPlotPestsCount = 0;
                }
            } else if (cleanedLine.contains("Plot")) {
                if (cleanedLine.contains("ൠ")) {
                    currentPlotPestsCount = 1;
                } else {
                    currentPlotPestsCount = 0;
                }
            }
        }
        if (pestsCountTemp != pestsCount) {
            pestsCount = pestsCountTemp;
        }
        if (pestsCount == 0) {
            infestedPlots.clear();
        }
    }

    private int checkPestHunterBonusTabList(String cleanedLine) {
        if (cleanedLine.contains("Bonus: +")) {
            return 1;
        }
        if (cleanedLine.contains("Bonus: INACTIVE")) {
            return 2;
        }
        return -1;
    }

    private void checkInfestedPlotsTabList(String cleanedLine) {
        if (cleanedLine.contains("Plots:")) {
            try {
                String[] split = cleanedLine.trim().split(" ");
                infestedPlots.clear();
                for (int i = 1; i < split.length; i++) {
                    try {
                        infestedPlots.add(Integer.parseInt(split[i].replace(",", "")));
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
                infestedPlots.clear();
            }
        }
    }

    private boolean checkGuestInGardenTabList(String cleanedLine) {
        if (cleanedLine.contains("Guests (0)")) {
            isGuestInGarden = false;
            return true;
        }
        if (cleanedLine.contains("Guests ")) {
            isGuestInGarden = true;
            return true;
        }
        return false;
    }

    public void onTickCheckPlot() {
        if (inGarden()) {
            PlotUtils.Plot plot = PlotUtils.getPlotNumberBasedOnLocation();
            if (plot == null) {
                currentPlot = -5;
                return;
            }
            currentPlot = plot.number;
        } else {
            currentPlot = -5;
        }
    }

    public void checkBuffsTabList(List<String> footerString) {
        boolean foundGodPotBuff = false;
        boolean foundCookieBuff = false;
        boolean foundPestRepellent = false;
        boolean loaded = false;

        for (String line : footerString) {
            if (line.contains("Active Effects")) {
                loaded = true;
            }
            if (line.contains("You have a God Potion active!")) {
                foundGodPotBuff = true;
                continue;
            }
            if (line.contains("Pest Repellant") || line.contains("Pest Repellent")) {
                foundPestRepellent = true;
                if (!AutoRepellent.repellentFailsafeClock.isScheduled()) {
                    String[] split = line.split(" ");
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
            if (line.contains("Cookie Buff")) {
                foundCookieBuff = true;
                continue;
            }
            if (foundCookieBuff) {
                if (line.contains("Not active")) {
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
                } else if (!FailsafeManager.getInstance().firstCheckReturn()
                        && (CobwebFailsafe.getInstance().isTouchingCobwebBlock() || CobwebFailsafe.getInstance().hasCobwebs())) {
                    FailsafeManager.getInstance().possibleDetection(CobwebFailsafe.getInstance());
                } else {
                    if (notMovingTimer.isScheduled()) {
                        randomValueToWaitNextTime = -1;
                        notMovingTimer.reset();
                        randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
                        updatedState = false;
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
        if (randomRewarpValueToWait == -1) randomRewarpValueToWait = FarmHelperConfig.getRandomRewarpDelay();
        return reWarpTimer.hasPassed(randomRewarpValueToWait);
    }

    public void scheduleRewarp() {
        randomRewarpValueToWait = FarmHelperConfig.getRandomRewarpDelay();
        reWarpTimer.reset();
    }

    public boolean hasPassedSinceStopped() {
        if (randomValueToWait == -1) randomValueToWait = FarmHelperConfig.getRandomTimeBetweenChangingRows();
        return notMovingTimer.hasPassed(randomValueToWaitNextTime != -1 ? randomValueToWaitNextTime : randomValueToWait);
    }

    public boolean notMoving() {
        if (dx < 0.01 && dz < 0.01 && dyIsRest() && mc.currentScreen == null) {
            return true;
        }
        return !holdingKeybindIsWalkable() && (playerIsInFlowingWater(0) || playerIsInFlowingWater(1)) && mc.thePlayer.isInWater();
    }

    public boolean notMovingHorizontally() {
        if (dx < 0.01 && dz < 0.01 && mc.currentScreen == null) {
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
        return level != 0;
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
        return !updatedState && !notMovingTimer.isScheduled();
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
        boolean isInGarden = location != Location.TELEPORTING && location == Location.GARDEN;
        if (isInGarden)
            lastTimeInGarden = System.currentTimeMillis();
        return isInGarden;
    }

    public boolean inJacobContest() {
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent() && FailsafeManager.getInstance().triggeredFailsafe.get().equals(JacobFailsafe.getInstance())) {
            return isInJacobContest;
        }
        if (FarmHelperConfig.jacobContestCurrentCropsOnly) {
            if (jacobsContestCrop.isPresent()) {
                return isInJacobContest && jacobsContestCrop.get() == MacroHandler.getInstance().getCrop();
            }
        }
        return isInJacobContest;
    }

    public boolean isGuestInGarden() {
        return isGuestInGarden;
    }

    public int getPestsFromVacuum() {
        if (mc.theWorld == null || mc.thePlayer == null)
            return 0;
        Slot slot = InventoryUtils.getSlotOfItemInInventory("Vacuum");
        if (slot == null)
            return 0;
        if (!slot.getHasStack())
            return 0;
        ItemStack stack = slot.getStack();
        if (stack == null)
            return 0;
        ArrayList<String> lore = InventoryUtils.getItemLore(stack);
        if (lore.isEmpty())
            return 0;
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

            if (ea.hasKey("farmed_cultivating", 99) || ea.hasKey("mined_crops", 99)) {
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
        UNKNOWN(""),
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
