package com.github.may2beez.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class VisitorsMacro implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static VisitorsMacro instance;

    public static VisitorsMacro getInstance() {
        if (instance == null) {
            instance = new VisitorsMacro();
        }
        return instance;
    }

    enum MainState {
        NONE,
        TRAVEL,
        AUTO_SELL,
        COMPACTORS,
        VISITORS,
        END
    }

    @Getter
    private MainState mainState = MainState.NONE;

    enum TravelState {
        NONE,
        ROTATE_TO_EDGE,
        FLY_TO_EDGE,
        ROTATE_TO_CENTER,
        FLY_TO_CENTER,
        ROTATE_TO_DESK,
        MOVE_TOWARDS_DESK,
        END
    }

    @Getter
    private TravelState travelState = TravelState.NONE;
    private int previousDistanceToCheck = Integer.MAX_VALUE;
    private int retriesGettingCloser = 0;
    private final Clock aotvTpCooldown = new Clock();
    private Optional<BlockPos> beforeTeleportationPos = Optional.empty();
    private Optional<BlockPos> currentEdge = Optional.empty();
    private int speed = 0;

    enum CompactorState {
        NONE,
        GET_LIST,
        HOLD_COMPACTOR,
        OPEN_COMPACTOR,
        TOGGLE_COMPACTOR,
        CLOSE_COMPACTOR,
        END
    }

    @Getter
    private CompactorState compactorState = CompactorState.NONE;
    private final ArrayList<Integer> compactors = new ArrayList<>();
    private boolean enableCompactors = false;

    enum VisitorsState {
        NONE,
        GET_CLOSEST_VISITOR,
        ROTATE_TO_VISITOR,
        OPEN_VISITOR,
        GET_LIST,
        CLOSE_VISITOR,
        OPEN_BZ,
        BUY_STATE,
        ROTATE_TO_VISITOR_2,
        OPEN_VISITOR_2,
        FINISH_VISITOR,
        END
    }

    @Getter
    private VisitorsState visitorsState = VisitorsState.NONE;
    @Getter
    private Optional<Entity> currentVisitor = Optional.empty();
    @Getter
    private Optional<Entity> currentCharacter = Optional.empty();
    private boolean rejectVisitor = false;

    enum BuyState {
        NONE,
        CLICK_CROP,
        CLICK_BUY,
        CLICK_SIGN,
        CLICK_CONFIRM,
        CLOSE_BZ,
        END
    }

    @Getter
    private BuyState buyState = BuyState.NONE;
    private final ArrayList<Pair<String, Long>> itemsToBuy = new ArrayList<>();
    private Optional<Pair<String, Long>> currentItemToBuy = Optional.empty();


    @Getter
    private final Clock delayClock = new Clock();
    @Getter
    private final Clock stuckClock = new Clock();
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.visitorsMacroGuiDelay + FarmHelperConfig.visitorsMacroGuiDelayRandomness);
    public final List<String> profitRewards = Arrays.asList("Dedication", "Cultivating", "Delicate", "Replenish", "Music Rune", "Green Bandana", "Overgrown Grass", "Space Helmet");
    private final RotationUtils rotation = new RotationUtils();

    @Override
    public String getName() {
        return "Visitors Macro";
    }

    @Override
    public boolean isRunning() {
        return enabled;
    }

    @Override
    public boolean shouldPauseMacroExecution() {
        return true;
    }

    @Override
    public boolean shouldStartAtMacroStart() {
        return false;
    }

    @Override
    public void start() {
        if (!canEnableMacro(manuallyStarted) && !forceStart) {
            setManuallyStarted(false);
            return;
        }
        enabled = true;
        rejectVisitor = false;
        enableCompactors = false;
        mainState = MainState.NONE;
        travelState = TravelState.NONE;
        compactorState = CompactorState.NONE;
        visitorsState = VisitorsState.NONE;
        buyState = BuyState.NONE;
        if (manuallyStarted || forceStart) {
            setMainState(MainState.TRAVEL);
            setTravelState(TravelState.END);
        }
        forceStart = false;
        delayClock.reset();
        rotation.reset();
        stuckClock.schedule(STUCK_DELAY);
        currentVisitor = Optional.empty();
        currentCharacter = Optional.empty();
        currentItemToBuy = Optional.empty();
        beforeTeleportationPos = Optional.empty();
        currentEdge = Optional.empty();
        itemsToBuy.clear();
        compactors.clear();
        previousDistanceToCheck = Integer.MAX_VALUE;
        retriesGettingCloser = 0;
        speed = InventoryUtils.getRancherBootSpeed();
        LogUtils.sendDebug("[Visitors Macro] Speed: " + speed);
        LogUtils.sendDebug("[Visitors Macro] Macro started");
        if (FarmHelperConfig.onlyAcceptProfitableVisitors) {
            LogUtils.sendDebug("[Visitors Macro] Only accepting profitable rewards. " + String.join(", ", profitRewards));
        }
    }

    @Override
    public void stop() {
        enabled = false;
        manuallyStarted = false;
        LogUtils.sendDebug("[Visitors Macro] Macro stopped");
        rotation.reset();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.visitorsMacro;
    }

    private boolean enabled = false;

    private final ArrayList<String> visitors = new ArrayList<>();

    private Optional<Integer> aspectOfTheSlot = Optional.empty();

    @Getter
    @Setter
    private boolean manuallyStarted = false;

    private boolean forceStart = false;

    public boolean canEnableMacro(boolean manual) {
        if (!isToggled()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return false;

        if (!manual && (!PlayerUtils.isSpawnLocationSet() || !PlayerUtils.isStandingOnSpawnPoint())) {
            LogUtils.sendError("[Visitors Macro] Player is not standing on spawn location, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            LogUtils.sendError("[Visitors Macro] Cookie buff is not active, skipping...");
            return false;
        }

        if (FarmHelperConfig.pauseVisitorsMacroDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendError("[Visitors Macro] Jacob's contest is active, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCurrentPurse() < FarmHelperConfig.visitorsMacroMinMoney) {
            LogUtils.sendError("[Visitors Macro] Player's purse is too low, skipping...");
            return false;
        }

//        if (!manual && visitors.size() < 5) {
//            LogUtils.sendError("[Visitors Macro] Queue is not full, skipping...");
//            return false;
//        }

        if (!FarmHelperConfig.visitorsMacroAction && !canSeeClosestEdgeOfBarn()) {
            LogUtils.sendError("[Visitors Macro] Player can't see closest edge of barn, skipping...");
            return false;
        }

        if (!FarmHelperConfig.visitorsMacroAction && !isAboveHeadClear()) {
            LogUtils.sendError("[Visitors Macro] Player's head is not clear, skipping...");
            return false;
        }

        if (!manual) {
            int aspectOfTheVoid = InventoryUtils.getSlotOfItemInHotbar("Aspect of the Void", "Aspect of the End");
            if (aspectOfTheVoid == -1) {
                LogUtils.sendError("[Visitors Macro] Player does not have Aspect of the Void or End in hotbar, travel will be slower");
                aspectOfTheSlot = Optional.empty();
            } else {
                aspectOfTheSlot = Optional.of(aspectOfTheVoid);
            }
        }

        if (!PlayerUtils.isDeskPosSet()) {
            LogUtils.sendError("[Visitors Macro] Player's desk position is not set, skipping...");
            return false;
        }

        return true;
    }

    private final List<BlockPos> barnEdges = Arrays.asList(new BlockPos(33, 85, -6), new BlockPos(-32, 85, -6));
    private final BlockPos barnCenter = new BlockPos(0, 85, -7);

    private boolean canSeeClosestEdgeOfBarn() {
        Vec3 positionFrom = new Vec3(mc.thePlayer.posX, 80, mc.thePlayer.posZ);
        BlockPos closestEdge = null;
        double closestDistance = Double.MAX_VALUE;
        List<BlockPos> tempBarnEdges = new ArrayList<>(barnEdges);
        tempBarnEdges.add(barnCenter);
        for (BlockPos edge : tempBarnEdges) {
            double distance = positionFrom.distanceTo(new Vec3(edge.getX(), edge.getY(), edge.getZ()));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEdge = edge;
            }
        }
        return closestEdge != null && mc.theWorld.rayTraceBlocks(positionFrom, new Vec3(closestEdge.getX() + 0.5, closestEdge.getY() + 0.5, closestEdge.getZ() + 0.5), false, true, false) == null;
    }

    private boolean canSeeCenterOfBarn() {
        Vec3 positionFrom = mc.thePlayer.getPositionVector();
        return mc.theWorld.rayTraceBlocks(positionFrom, new Vec3(barnCenter.getX() + 0.5, barnCenter.getY() + 0.5, barnCenter.getZ() + 0.5), false, true, false) == null;
    }

    private boolean canSeeDeskPos() {
        Vec3 positionFrom = mc.thePlayer.getPositionVector();
        Vec3 positionFrom1 = new Vec3(positionFrom.xCoord, FarmHelperConfig.visitorsDeskPosY + 2.5, positionFrom.zCoord);
        Vec3 deskPos1 = new Vec3(FarmHelperConfig.visitorsDeskPosX + 0.5, FarmHelperConfig.visitorsDeskPosY + 0.5, FarmHelperConfig.visitorsDeskPosZ + 0.5);
        return mc.theWorld.rayTraceBlocks(positionFrom1, deskPos1, false, true, false) == null &&
                mc.theWorld.rayTraceBlocks(positionFrom, deskPos1, false, true, false) == null;
    }

    private boolean isAboveHeadClear() {
        for (int y = BlockUtils.getRelativeBlockPos(0, 1, 0).getY(); y < 100; y++) {
            BlockPos blockPos = BlockUtils.getRelativeBlockPos(0, y, 0);
            Block block = mc.theWorld.getBlockState(blockPos).getBlock();
            if (!mc.theWorld.isAirBlock(blockPos) && !block.equals(Blocks.reeds) && !block.equals(Blocks.water) && !block.equals(Blocks.flowing_water)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public void onTickCheckVisitors(TickEvent.ClientTickEvent event) {
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        List<String> tabList = TablistUtils.getTabList();
        if (tabList.size() < 2) return;
        boolean foundVisitors = false;
        ArrayList<String> newVisitors = new ArrayList<>();
        for (String line : tabList) {
            if (line.contains("Visitors:")) {
                foundVisitors = true;
                continue;
            }
            if (StringUtils.stripControlCodes(line).trim().isEmpty()) {
                if (foundVisitors) {
                    break;
                }
                continue;
            }
            if (foundVisitors) {
                newVisitors.add(StringUtils.stripControlCodes(line));
            }
        }
        if (newVisitors.equals(visitors)) return;
        visitors.clear();
        visitors.addAll(newVisitors);
        LogUtils.sendDebug("[Visitors Macro] Visitors: " + visitors.size());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendError("[Visitors Macro] Player is stuck, restarting macro");
            stop();
            forceStart = true;
            start();
            return;
        }

        switch (mainState) {
            case NONE:
                setMainState(MainState.TRAVEL);
                delayClock.schedule(getRandomDelay());
                break;
            case TRAVEL:
                onTravelState();
                break;
            case AUTO_SELL:
                AutoSell.getInstance().start();
                enableCompactors = false;
                setMainState(MainState.COMPACTORS);
                delayClock.schedule(getRandomDelay());
                break;
            case COMPACTORS:
                if (AutoSell.getInstance().isRunning()) {
                    stuckClock.schedule(STUCK_DELAY);
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                onCompactorState();
                break;
            case VISITORS:
                onVisitorsState();
                break;
            case END:
                break;
        }
    }

    private void onTravelState() {
        switch (travelState) {
            case NONE:
                if (FarmHelperConfig.visitorsMacroAction) {
                    // Teleport to plot 4 and fly
                    beforeTeleportationPos = Optional.of(BlockUtils.getRelativeBlockPos(0, 0, 0));
                    mc.thePlayer.sendChatMessage("/tptoplot 4");
                    setTravelState(TravelState.ROTATE_TO_CENTER);
                } else {
                    // Fly from farm
                    setTravelState(TravelState.ROTATE_TO_EDGE);
                }
                break;
            case ROTATE_TO_EDGE:
                BlockPos closestEdge = barnEdges.stream().min(Comparator.comparingDouble(edge -> mc.thePlayer.getDistance(edge.getX(), edge.getY(), edge.getZ()))).orElse(null);
                LogUtils.sendDebug("[Visitors Macro] Closest edge: " + closestEdge);
                if (closestEdge == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find closest edge, restarting macro");
                    stop();
                    forceStart = true;
                    start();
                    return;
                }

                if (mc.thePlayer.getDistance(barnCenter.getX(), barnCenter.getY(), barnCenter.getZ()) < mc.thePlayer.getDistance(closestEdge.getX(), closestEdge.getY(), closestEdge.getZ())) {
                    setTravelState(TravelState.ROTATE_TO_CENTER);
                    return;
                }

                RotationUtils.Rotation rotationToEdge = AngleUtils.getRotation(closestEdge.add(0.5, 0.5, 0.5));
                long rotationTime = FarmHelperConfig.getRandomRotationTime();
                rotation.easeTo(rotationToEdge.getYaw(), rotationToEdge.getPitch(), rotationTime);
                currentEdge = Optional.of(closestEdge);
                setTravelState(TravelState.FLY_TO_EDGE);
                delayClock.schedule(rotationTime);
                break;
            case FLY_TO_EDGE:
                aspectOfTheSlot.ifPresent(integer -> mc.thePlayer.inventory.currentItem = integer);

                stuckClock.schedule(STUCK_DELAY);
                assert currentEdge.isPresent();
                double distanceToEdge = mc.thePlayer.getDistance(currentEdge.get().getX(), mc.thePlayer.posY, currentEdge.get().getZ());

                if (canSeeCenterOfBarn() && distanceToEdge < 25) {
                    LogUtils.sendDebug("[Visitors Macro] Player can see center of barn");
                    setTravelState(TravelState.ROTATE_TO_CENTER);
                    break;
                }

                if (distanceToEdge > previousDistanceToCheck && distanceToEdge > 10) {
                    retriesGettingCloser++;
                    setTravelState(TravelState.ROTATE_TO_EDGE);
                    KeyBindUtils.stopMovement();
                    previousDistanceToCheck = Integer.MAX_VALUE;
                    if (retriesGettingCloser >= 2) {
                        LogUtils.sendError("[Visitors Macro] Player is not getting closer to edge. Stopping Visitors Macro... Report it in the Discord Server");
                        setMainState(MainState.END);
                        return;
                    }
                    break;
                }

                if (flyUpAndForward()) break;

                LogUtils.sendDebug("[Visitors Macro] Player is close enough to edge");
                KeyBindUtils.stopMovement();
                setTravelState(TravelState.ROTATE_TO_CENTER);
                delayClock.schedule(getRandomDelay());
                break;
            case ROTATE_TO_CENTER:
                if (beforeTeleportationPos.isPresent()) {
                    if (mc.thePlayer.getDistance(beforeTeleportationPos.get().getX(), beforeTeleportationPos.get().getY(), beforeTeleportationPos.get().getZ()) > 1) {
                        beforeTeleportationPos = Optional.empty();
                        LogUtils.sendDebug("[Visitors Macro] Player teleported to plot 4");
                        delayClock.schedule(getRandomDelay());
                    }
                    break;
                }
                if (rotation.rotating) return;

                if (canSeeDeskPos()) {
                    LogUtils.sendDebug("[Visitors Macro] Player can see desk pos");
                    setTravelState(TravelState.ROTATE_TO_DESK);
                    break;
                }

                stuckClock.schedule(STUCK_DELAY);
                RotationUtils.Rotation rotationToCenter = AngleUtils.getRotation(barnCenter.add(0.5, 0.5, 0.5));
                currentEdge = Optional.of(barnCenter);
                long rotationTime2 = FarmHelperConfig.getRandomRotationTime();
                rotation.easeTo(rotationToCenter.getYaw(), rotationToCenter.getPitch(), rotationTime2);
                setTravelState(TravelState.FLY_TO_CENTER);
                delayClock.schedule(rotationTime2);
                break;
            case FLY_TO_CENTER:
                aspectOfTheSlot.ifPresent(integer -> mc.thePlayer.inventory.currentItem = integer);

                stuckClock.schedule(STUCK_DELAY);

                if (canSeeDeskPos()) {
                    LogUtils.sendDebug("[Visitors Macro] Player can see desk pos");
                    setTravelState(TravelState.ROTATE_TO_DESK);
                    break;
                }

                if (flyUpAndForward()) break;

                LogUtils.sendDebug("[Visitors Macro] Player is close enough to center");
                KeyBindUtils.stopMovement();
                setTravelState(TravelState.ROTATE_TO_DESK);
                break;
            case ROTATE_TO_DESK:
                if (GameStateHandler.getInstance().getDx() > 0.25 || GameStateHandler.getInstance().getDz() > 0.25) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                    break;
                }
                KeyBindUtils.stopMovement();
                RotationUtils.Rotation rotationToDesk = AngleUtils.getRotation(new Vec3(FarmHelperConfig.visitorsDeskPosX + 0.5, FarmHelperConfig.visitorsDeskPosY + 1.5, FarmHelperConfig.visitorsDeskPosZ + 0.5));
                rotation.easeTo(rotationToDesk.getYaw(), rotationToDesk.getPitch(), FarmHelperConfig.getRandomRotationTime());
                setTravelState(TravelState.MOVE_TOWARDS_DESK);
                previousDistanceToCheck = Integer.MAX_VALUE;
                break;
            case MOVE_TOWARDS_DESK:
                RotationUtils.Rotation rotationToDesk2 = AngleUtils.getRotation(new Vec3(FarmHelperConfig.visitorsDeskPosX + 0.5, FarmHelperConfig.visitorsDeskPosY + 1.5, FarmHelperConfig.visitorsDeskPosZ + 0.5));
                if ((Math.abs(rotationToDesk2.getYaw() - mc.thePlayer.rotationYaw) > 1 || Math.abs(rotationToDesk2.getPitch() - mc.thePlayer.rotationPitch) > 1) && !rotation.rotating) {
                    rotation.easeTo(rotationToDesk2.getYaw(), rotationToDesk2.getPitch(), FarmHelperConfig.getRandomRotationTime());
                }

                BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
                BlockPos deskPos = new BlockPos(FarmHelperConfig.visitorsDeskPosX, playerPos.getY(), FarmHelperConfig.visitorsDeskPosZ);
                double distance = Math.sqrt(playerPos.distanceSq(deskPos));
                stuckClock.schedule(STUCK_DELAY);
                if (distance <= 0.75f || playerPos.equals(deskPos) || (previousDistanceToCheck < distance && distance < 1.75f)) {
                    KeyBindUtils.stopMovement();
                    setTravelState(TravelState.END);
                    delayClock.schedule(getRandomDelay());
                } else {
                    if (mc.thePlayer.capabilities.isFlying) {
                        mc.thePlayer.capabilities.isFlying = false;
                        mc.thePlayer.sendPlayerAbilities();
                    }
                    KeyBindUtils.holdThese(
                            mc.gameSettings.keyBindForward,
                            (speed > 150 && distance < 2.5) ? mc.gameSettings.keyBindSneak : null,
                            distance > 10 ? mc.gameSettings.keyBindSprint : null
                    );
                    if (shouldJump()) {
                        mc.thePlayer.jump();
                    }
                }
                previousDistanceToCheck = (int) distance;
                break;
            case END:
                if (FarmHelperConfig.visitorsMacroAutosellBeforeServing) {
                    setMainState(MainState.AUTO_SELL);
                } else if (InventoryUtils.hasItemInHotbar("Compactor")) {
                    enableCompactors = false;
                    setMainState(MainState.COMPACTORS);
                } else {
                    setMainState(MainState.VISITORS);
                }
                break;
        }
    }

    private boolean shouldJump() {
        BlockPos block1 = BlockUtils.getRelativeBlockPos(0, 0, 1);
        BlockPos block2 = BlockUtils.getRelativeBlockPos(0, 0, 2);
        Block block = mc.theWorld.getBlockState(block1).getBlock();
        Block block3 = mc.theWorld.getBlockState(block2).getBlock();
        return blockNotPassable(block2, block3) || blockNotPassable(block1, block);
    }

    private boolean blockNotPassable(BlockPos block2, Block block3) {
        if (mc.thePlayer.onGround && !block3.equals(Blocks.air) && !block3.isPassable(mc.theWorld, block2) && !(block3 instanceof BlockSlab) && !(block3 instanceof BlockStairs) && !(block3 instanceof BlockTallGrass)) {
            rotation.reset();
            return true;
        }
        return false;
    }

    private boolean flyUpAndForward() {
        assert currentEdge.isPresent();
        double distanceToEdge = mc.thePlayer.getDistance(currentEdge.get().getX(), mc.thePlayer.posY, currentEdge.get().getZ());
        int playerY = mc.thePlayer.getPosition().getY();

        if (distanceToEdge > 5) {
            if (!mc.thePlayer.capabilities.isFlying && mc.thePlayer.capabilities.allowFlying) {
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.sendPlayerAbilities();
            }
            KeyBindUtils.holdThese(
                    (forwardIsClear()) ? mc.gameSettings.keyBindForward : null,
                    playerY < 85 ? mc.gameSettings.keyBindJump : null,
                    mc.gameSettings.keyBindSprint
            );

            if (distanceToEdge > 14) {
                if ((aotvTpCooldown.passed() || !aotvTpCooldown.isScheduled()) && aspectOfTheSlot.isPresent() && !rotation.rotating) {
                    KeyBindUtils.rightClick();
                    aotvTpCooldown.schedule((long) (150 + Math.random() * 150));
                    rotation.reset();
                }
            }

            RotationUtils.Rotation rotationToEdge2 = AngleUtils.getRotation(currentEdge.get().add(0.5, 0.5, 0.5));
            float randomValue = playerY > 80 ? 5 + (float) (Math.random() * 1 - 0.5) : 1 + (float) (Math.random() * 1 - 0.5);

            if (forwardIsClear() && (Math.abs(mc.thePlayer.rotationYaw - rotationToEdge2.getYaw()) > 5 || Math.abs(mc.thePlayer.rotationPitch - randomValue) > 5) && !rotation.rotating && aotvTpCooldown.isScheduled()) {
                aotvTpCooldown.schedule((long) (200 + Math.random() * 200));
                rotation.reset();
                rotation.easeTo(rotationToEdge2.getYaw(), randomValue, FarmHelperConfig.getRandomRotationTime());
            }
            return true;
        }
        return false;
    }

    private boolean forwardIsClear() {
        Block block1 = BlockUtils.getRelativeBlock(0, 0, 1);
        Block block2 = BlockUtils.getRelativeBlock(0, 1, 1);
        Block block3 = BlockUtils.getRelativeBlock(0, 0, 2);
        Block block4 = BlockUtils.getRelativeBlock(0, 1, 2);
        return blockPassable(block1) && blockPassable(block2) && blockPassable(block3) && blockPassable(block4);
    }

    private boolean blockPassable(Block block) {
        return block.equals(Blocks.air) || block.equals(Blocks.water) || block.equals(Blocks.flowing_water);
    }

    private void onCompactorState() {
        switch (compactorState) {
            case NONE:
                rotation.easeTo(mc.thePlayer.rotationYaw, (float) (85 + (Math.random() * 10 - 5)), FarmHelperConfig.getRandomRotationTime());
                setCompactorState(CompactorState.GET_LIST);
                break;
            case GET_LIST:
                if (rotation.rotating) return;
                compactors.clear();
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (itemStack != null && itemStack.getDisplayName().contains("Compactor")) {
                        compactors.add(i);
                    }
                }
                if (compactors.isEmpty()) {
                    LogUtils.sendError("[Visitors Macro] Player does not have any compactors in hotbar, skipping...");
                    setMainState(MainState.VISITORS);
                    return;
                }
                setCompactorState(CompactorState.HOLD_COMPACTOR);
                break;
            case HOLD_COMPACTOR:
                LogUtils.sendDebug("[Visitors Macro] Holding compactor");
                if (compactors.isEmpty()) {
                    LogUtils.sendError("[Visitors Macro] All compactors has been disabled");
                    setCompactorState(CompactorState.END);
                    return;
                }
                mc.thePlayer.inventory.currentItem = compactors.get(0);
                compactors.remove(0);
                setCompactorState(CompactorState.OPEN_COMPACTOR);
                delayClock.schedule(getRandomDelay());
                break;
            case OPEN_COMPACTOR:
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                KeyBindUtils.rightClick();
                setCompactorState(CompactorState.TOGGLE_COMPACTOR);
                delayClock.schedule(getRandomDelay());
                break;
            case TOGGLE_COMPACTOR:
                if (mc.currentScreen == null) break;
                int slot = InventoryUtils.getSlotOfItemInContainer("Compactor Currently");
                if (slot == -1) break;
                Slot slotObject = InventoryUtils.getSlotOfIdInContainer(slot);
                if (slotObject == null) break;
                ItemStack itemStack = slotObject.getStack();
                if (itemStack == null) break;
                if (itemStack.getDisplayName().contains("OFF") && !enableCompactors) {
                    LogUtils.sendDebug("[Visitors Macro] Compactor is already OFF, skipping...");
                } else if (!enableCompactors && itemStack.getDisplayName().contains("ON")) {
                    LogUtils.sendDebug("[Visitors Macro] Disabling compactor in slot " + slot);
                    InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                }
                if (itemStack.getDisplayName().contains("ON") && enableCompactors) {
                    LogUtils.sendDebug("[Visitors Macro] Compactor is already ON, skipping...");
                } else if (enableCompactors && itemStack.getDisplayName().contains("OFF")) {
                    LogUtils.sendDebug("[Visitors Macro] Enabling compactor in slot " + slot);
                    InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                }
                setCompactorState(CompactorState.CLOSE_COMPACTOR);
                delayClock.schedule(getRandomDelay());
                break;
            case CLOSE_COMPACTOR:
                LogUtils.sendDebug("[Visitors Macro] Closing compactor");
                if (mc.currentScreen != null) {
                    mc.thePlayer.closeScreen();
                }
                setCompactorState(CompactorState.HOLD_COMPACTOR);
                delayClock.schedule(getRandomDelay());
                break;
            case END:
                setMainState(MainState.VISITORS);
                delayClock.schedule(getRandomDelay());
                break;
        }
    }

    private void onVisitorsState() {
        switch (visitorsState) {
            case NONE:
                setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
                break;
            case GET_CLOSEST_VISITOR:
                LogUtils.sendDebug("[Visitors Macro] Getting closest visitor");
                if (getNonToolItem() == -1) {
                    LogUtils.sendError("[Visitors Macro] Player does not have any free slots in hotbar, might get stuck...");
                } else {
                    mc.thePlayer.inventory.currentItem = getNonToolItem();
                }
                if (visitors.isEmpty()) {
                    LogUtils.sendError("[Visitors Macro] No visitors in queue, ending...");
                    setMainState(MainState.END);
                    return;
                }
                Entity closest = mc.theWorld.getLoadedEntityList().
                        stream().
                        filter(entity ->
                                entity.hasCustomName() &&
                                visitors.stream().anyMatch(
                                        v ->
                                            StringUtils.stripControlCodes(v).contains(StringUtils.stripControlCodes(entity.getCustomNameTag()))))
                        .filter(entity -> entity.getDistanceToEntity(mc.thePlayer) < 4)
                        .min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)))
                        .orElse(null);
                if (closest == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find closest visitor, waiting");
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                Entity character = PlayerUtils.getEntityCuttingOtherEntity(closest);

                if (character == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find character of closest visitor, restarting macro");
                    stop();
                    forceStart = true;
                    start();
                    return;
                }

                LogUtils.sendDebug("[Visitors Macro] Closest visitor: " + closest.getCustomNameTag());
                currentVisitor = Optional.of(closest);
                currentCharacter = Optional.of(character);
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                rotation.reset();
                break;
            case ROTATE_TO_VISITOR:
                if (currentVisitor.isPresent()) {
                    if (rotation.rotating) return;
                    RotationUtils.Rotation rotationToVisitor = AngleUtils.getRotation(currentVisitor.get().getPositionEyes(1).add(new Vec3(0, -0.25, 0)), true);
                    rotation.easeTo(rotationToVisitor.getYaw(), rotationToVisitor.getPitch(), FarmHelperConfig.getRandomRotationTime());
                    setVisitorsState(VisitorsState.OPEN_VISITOR);
                    delayClock.schedule(FarmHelperConfig.getRandomRotationTime());
                } else {
                    setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
                }
                break;
            case OPEN_VISITOR:
                if (mc.currentScreen != null) {
                    setVisitorsState(VisitorsState.GET_LIST);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                if (rotation.rotating) return;
                assert currentVisitor.isPresent();
                if (entityIsMoving(currentVisitor.get())) {
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    break;
                }
                itemsToBuy.clear();
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    assert currentVisitor.isPresent();
                    assert currentCharacter.isPresent();
                    if (entity.equals(currentVisitor.get()) || entity.equals(currentCharacter.get())) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        setVisitorsState(VisitorsState.GET_LIST);
                        KeyBindUtils.rightClick();
                        delayClock.schedule(getRandomDelay());
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Looking at something else");
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    }
                    break;
                }
                break;
            case GET_LIST:
                if (mc.currentScreen == null) {
                    setVisitorsState(VisitorsState.OPEN_VISITOR);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                Slot npcSlot = InventoryUtils.getSlotOfIdInContainer(13);
                if (npcSlot == null) break;
                ItemStack npcItemStack = npcSlot.getStack();
                if (npcItemStack == null) break;
                ArrayList<String> lore = InventoryUtils.getItemLore(npcItemStack);
                boolean isNpc = lore.size() == 4 && lore.get(3).contains("Offers Accepted: ");
                String npcName = isNpc ? StringUtils.stripControlCodes(npcSlot.getStack().getDisplayName()) : "";
                assert currentVisitor.isPresent();
                if (npcName.isEmpty() || !StringUtils.stripControlCodes(npcName).contains(StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()))) {
                    LogUtils.sendError("[Visitors Macro] Opened wrong NPC.");
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    mc.thePlayer.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                LogUtils.sendDebug("[Visitors Macro] Opened NPC: " + npcName);
                break;
            case CLOSE_VISITOR:
                break;
            case OPEN_BZ:
                break;
            case BUY_STATE:
                onBuyState();
                break;
            case ROTATE_TO_VISITOR_2:
                break;
            case OPEN_VISITOR_2:
                break;
            case FINISH_VISITOR:
                break;
            case END:
                break;
        }
    }

    private boolean entityIsMoving(Entity e) {
        return e.motionX != 0 || e.motionY != 0 || e.motionZ != 0;
    }

    private void onBuyState() {
        switch (buyState) {

            case NONE:
                break;
            case CLICK_CROP:
                break;
            case CLICK_BUY:
                break;
            case CLICK_SIGN:
                break;
            case CLICK_CONFIRM:
                break;
            case CLOSE_BZ:
                break;
            case END:
                break;
        }
    }

    private long getRandomDelay() {
        return (long) (500 + Math.random() * 1_000);
    }

    private void setMainState(MainState state) {
        mainState = state;
        LogUtils.sendDebug("[Visitors Macro] Main state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private void setTravelState(TravelState state) {
        travelState = state;
        LogUtils.sendDebug("[Visitors Macro] Travel state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private void setCompactorState(CompactorState state) {
        compactorState = state;
        LogUtils.sendDebug("[Visitors Macro] Compactor state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private void setVisitorsState(VisitorsState state) {
        visitorsState = state;
        LogUtils.sendDebug("[Visitors Macro] Visitors state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private void setBuyState(BuyState state) {
        buyState = state;
        LogUtils.sendDebug("[Visitors Macro] Buy state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    private int getNonToolItem() {
        ArrayList<Integer> slotsWithoutItems = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] == null || mc.thePlayer.inventory.mainInventory[i].getItem() == null) {
                slotsWithoutItems.add(i);
            }
        }
        if (!slotsWithoutItems.isEmpty()) {
            return slotsWithoutItems.get(0);
        }
        for (int i = 0; i < 8; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null &&
                    itemStack.getItem() != null &&
                    !(itemStack.getItem() instanceof ItemTool) &&
                    !(itemStack.getItem() instanceof ItemSword) &&
                    !(itemStack.getItem() instanceof ItemHoe) &&
                    !(itemStack.getItem() instanceof ItemSpade) &&
                    !itemStack.getDisplayName().contains("Compactor") &&
                    !itemStack.getDisplayName().contains("Cropie") &&
                    !itemStack.getDisplayName().contains("Squash") &&
                    !itemStack.getDisplayName().contains("Fermento")) {
                return i;
            }
        }
        return -1;
    }

    public boolean isInBarn() {
        BlockPos barn1 = new BlockPos(-30, 65, -45);
        BlockPos barn2 = new BlockPos(36, 80, -2);
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(barn1, barn2);
        boolean flag = axisAlignedBB.isVecInside(mc.thePlayer.getPositionVector());
        LogUtils.sendDebug("[Visitors Macro] Player is in barn: " + flag);
        return flag;
    }

    @SubscribeEvent
    public void onReceiveChat(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!isRunning()) return;
        if (!currentVisitor.isPresent()) return;
        if (visitorsState != VisitorsState.GET_LIST) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        String npcName = StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag());
        if (msg.startsWith("[NPC] " + npcName + ":")) {
            Multithreading.schedule(() -> {
                if (mc.currentScreen == null) {
                    KeyBindUtils.rightClick();
                }
            }, (long) (250 + Math.random() * 150), TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (rotation.rotating) {
            rotation.update();
        }
    }
}
