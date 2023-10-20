package com.github.may2beez.farmhelperv2.feature.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.IFeature;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import javafx.util.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        MOVE_TOWARDS_DESK,
        END
    }

    @Getter
    private TravelState travelState = TravelState.NONE;
    private int previousDistanceToCheck = 0;
    private int retriesGettingCloser = 0;
    private final Clock aotvTpCooldown = new Clock();

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

    enum VisitorsState {
        NONE,
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
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.visitorsMacroGuiDelay * 1000 + FarmHelperConfig.visitorsMacroGuiDelayRandomness * 1000);
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
        forceStart = false;
        rejectVisitor = false;
        mainState = MainState.NONE;
        travelState = TravelState.NONE;
        compactorState = CompactorState.NONE;
        visitorsState = VisitorsState.NONE;
        buyState = BuyState.NONE;
        delayClock.reset();
        rotation.reset();
        stuckClock.schedule(STUCK_DELAY);
        currentVisitor = Optional.empty();
        currentItemToBuy = Optional.empty();
        itemsToBuy.clear();
        compactors.clear();
        previousDistanceToCheck = Integer.MIN_VALUE;
        retriesGettingCloser = 0;
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

        if (!manual && (!PlayerUtils.isSpawnLocationSet() || !PlayerUtils.isStandingOnRewarpLocation())) {
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

        if (!manual && visitors.size() < 5) {
            LogUtils.sendError("[Visitors Macro] Queue is not full, skipping...");
            return false;
        }

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
                break;
            case ROTATE_TO_EDGE:
                break;
            case FLY_TO_EDGE:
                break;
            case ROTATE_TO_CENTER:
                break;
            case FLY_TO_CENTER:
                break;
            case MOVE_TOWARDS_DESK:
                break;
            case END:
                break;
        }
    }

    private void onCompactorState() {
        switch (compactorState) {

            case NONE:
                break;
            case GET_LIST:
                break;
            case HOLD_COMPACTOR:
                break;
            case OPEN_COMPACTOR:
                break;
            case TOGGLE_COMPACTOR:
                break;
            case CLOSE_COMPACTOR:
                break;
            case END:
                break;
        }
    }

    private void onVisitorsState() {
        switch (visitorsState) {

            case NONE:
                break;
            case ROTATE_TO_VISITOR:
                break;
            case OPEN_VISITOR:
                break;
            case GET_LIST:
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
}
