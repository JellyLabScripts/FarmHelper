package com.jelly.farmhelperv2.feature.impl;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.SignUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VisitorsMacro implements IFeature {
    private static VisitorsMacro instance;
    public final List<String> profitRewards = Arrays.asList("Dedication", "Cultivating", "Delicate", "Replenish", "Music Rune", "Green Bandana", "Overgrown Grass", "Space Helmet");
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ArrayList<Integer> compactors = new ArrayList<>();
    private final ArrayList<Entity> servedCustomers = new ArrayList<>();
    private final ArrayList<Pair<String, Long>> itemsToBuy = new ArrayList<>();
    @Getter
    private final Clock delayClock = new Clock();
    @Getter
    private final Clock stuckClock = new Clock();
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.macroGuiDelay + FarmHelperConfig.macroGuiDelayRandomness);
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final ArrayList<String> visitors = new ArrayList<>();
    Pattern itemNamePattern = Pattern.compile("^(.*?)(?:\\sx(\\d+))?$");
    @Getter
    private MainState mainState = MainState.NONE;
    @Getter
    private TravelState travelState = TravelState.NONE;
    private double previousDistanceToCheck = Integer.MAX_VALUE;
    private int speed = 0;
    @Getter
    private CompactorState compactorState = CompactorState.NONE;
    private boolean enableCompactors = false;
    @Getter
    private VisitorsState visitorsState = VisitorsState.NONE;
    @Getter
    private Optional<Entity> currentVisitor = Optional.empty();
    @Getter
    private Optional<Entity> currentCharacter = Optional.empty();
    @Getter
    private ArrayList<Tuple<String, String>> currentRewards = new ArrayList<>();
    private boolean rejectVisitor = false;
    private float spentMoney = 0;
    @Getter
    private BuyState buyState = BuyState.NONE;
    private boolean haveItemsInSack = false;
    private boolean enabled = false;
    @Getter
    @Setter
    private boolean manuallyStarted = false;
    private boolean forceStart = false;
    private boolean pathFound = false;
    private BlockPos positionBeforeTp = null;
    private Vec3 deskRotation = null;
    private Entity closestEntity = null;

    private int differentOptionCounter = 0;

    public static VisitorsMacro getInstance() {
        if (instance == null) {
            instance = new VisitorsMacro();
        }
        return instance;
    }

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
        if (!canEnableMacro(manuallyStarted, true) && !forceStart) {
            setManuallyStarted(false);
            return;
        }
        MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> macro.getRotation().reset());
        if (forceStart) {
            if (enableCompactors) {
                mainState = MainState.VISITORS;
            } else {
                mainState = MainState.NONE;
            }
            travelState = TravelState.NONE;
            compactorState = CompactorState.NONE;
            visitorsState = VisitorsState.NONE;
            buyState = BuyState.NONE;
        } else {
            mainState = MainState.NONE;
            travelState = TravelState.NONE;
            compactorState = CompactorState.NONE;
            visitorsState = VisitorsState.NONE;
            buyState = BuyState.NONE;
            enableCompactors = false;
        }
        enabled = true;
        rejectVisitor = false;
        if (manuallyStarted || forceStart) {
            setMainState(MainState.TRAVEL);
            setTravelState(TravelState.END);
            Entity closest = mc.theWorld.getLoadedEntityList().
                    stream().
                    filter(entity ->
                            entity.hasCustomName() &&
                                    visitors.stream().anyMatch(
                                            v ->
                                                    StringUtils.stripControlCodes(v).contains(StringUtils.stripControlCodes(entity.getCustomNameTag()))))
                    .filter(entity -> entity.getDistanceToEntity(mc.thePlayer) < 14)
                    .min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)))
                    .orElse(null);
            if (closest == null) {
                setTravelState(TravelState.NONE);
            }
        }
        forceStart = false;
        haveItemsInSack = false;
        delayClock.reset();
        rotation.reset();
        pathFound = false;
        stuckClock.schedule(STUCK_DELAY);
        currentVisitor = Optional.empty();
        currentCharacter = Optional.empty();
        itemsToBuy.clear();
        compactors.clear();
        currentRewards.clear();
        servedCustomers.clear();
        previousDistanceToCheck = Integer.MAX_VALUE;
        spentMoney = 0;
        speed = InventoryUtils.getRancherBootSpeed();
        LogUtils.sendDebug("[Visitors Macro] Speed: " + speed);
        LogUtils.sendDebug("[Visitors Macro] Macro started");
        if (FarmHelperConfig.visitorsActionUncommon == 1
                || FarmHelperConfig.visitorsActionRare == 1
                || FarmHelperConfig.visitorsActionLegendary == 1
                || FarmHelperConfig.visitorsActionMythic == 1
                || FarmHelperConfig.visitorsActionSpecial == 1) {
            LogUtils.sendDebug("[Visitors Macro] Accepting profitable offers only for one or more visitors. " + String.join(", ", profitRewards));
        }
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
        }
        LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro started");
    }

    @Override
    public void stop() {
        enabled = false;
        manuallyStarted = false;
        LogUtils.sendDebug("[Visitors Macro] Macro stopped");
        rotation.reset();
        if (mc.currentScreen != null && mc.thePlayer != null) {
            PlayerUtils.closeScreen();
        }
    }

    @Override
    public void resetStatesAfterMacroDisabled() {

    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.visitorsMacro;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return travelState != TravelState.ROTATE_TO_DESK && mainState != MainState.DISABLING && mainState != MainState.END;
    }

    public boolean canEnableMacro(boolean manual, boolean withError) {
        if (!isToggled()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return false;

        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Visitors Macro] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            return false;
        }

        if (!manual && !forceStart && (!PlayerUtils.isStandingOnSpawnPoint() && !PlayerUtils.isStandingOnRewarpLocation())) {
            if (withError)
                LogUtils.sendError("[Visitors Macro] The player is not standing on spawn location, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            if (withError) LogUtils.sendError("[Visitors Macro] Cookie buff is not active, skipping...");
            return false;
        }

        if (!manual && !forceStart && FarmHelperConfig.pauseVisitorsMacroDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            if (withError) LogUtils.sendError("[Visitors Macro] Jacob's contest is active, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCurrentPurse() < FarmHelperConfig.visitorsMacroMinMoney * 1_000) {
            if (withError) LogUtils.sendError("[Visitors Macro] The player's purse is too low, skipping...");
            return false;
        }

        if (!manual && visitors.size() < FarmHelperConfig.visitorsMacroMinVisitors) {
            if (withError) LogUtils.sendError("[Visitors Macro] Not enough Visitors in queue, skipping...");
            return false;
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
                newVisitors.add(line.trim());
            }
        }
        if (newVisitors.equals(visitors)) return;
        visitors.clear();
        visitors.addAll(newVisitors);
        LogUtils.sendDebug("[Visitors Macro] The visitors: " + visitors.size());
        boolean hasSpecial = false;
        for (String visitor : visitors) {
            if (Rarity.getRarityFromNpcName(visitor) == Rarity.SPECIAL) {
                hasSpecial = true;
                break;
            }
        }
        if (hasSpecial) {
            LogUtils.sendWarning("[Visitors Macro] Special visitor found in queue");
            LogUtils.webhookLog("[Visitors Macro]\\nSpecial visitor found in queue", true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendError("[Visitors Macro] The player is stuck, restarting the macro...");
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
                setMainState(MainState.DISABLING);
                Multithreading.schedule(() -> {
                    MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.triggerWarpGarden(true));
                    Multithreading.schedule(() -> {
                        stop();
                        MacroHandler.getInstance().resumeMacro();
                    }, 1_000, TimeUnit.MILLISECONDS);
                }, 500, TimeUnit.MILLISECONDS);
                break;
            case DISABLING:
                break;
        }
    }

    private void onTravelState() {
        if (mc.currentScreen != null) {
            KeyBindUtils.stopMovement();
            PlayerUtils.closeScreen();
            delayClock.schedule(getRandomDelay());
            return;
        }

        switch (travelState) {
            case NONE:
                positionBeforeTp = mc.thePlayer.getPosition();
                setTravelState(TravelState.ROTATE_TO_DESK);
                mc.thePlayer.sendChatMessage("/tptoplot barn");
                delayClock.schedule((long) (1_000 + Math.random() * 500));
                break;
            case ROTATE_TO_DESK:
                if (mc.thePlayer.getPosition().equals(positionBeforeTp) || PlayerUtils.isPlayerSuffocating()) {
                    LogUtils.sendDebug("[Visitors Macro] Waiting for teleportation...");
                    return;
                }
                if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                    LogUtils.sendDebug("[Visitors Macro] The player is not on the ground, waiting...");
                    return;
                }
                KeyBindUtils.stopMovement();

                List<Entity> allVisitors = mc.theWorld.getLoadedEntityList().
                        stream().
                        filter(entity ->
                                entity.hasCustomName() && visitors.stream().anyMatch(
                                        v ->
                                                StringUtils.stripControlCodes(v).contains(StringUtils.stripControlCodes(entity.getCustomNameTag()))))
                        .collect(Collectors.toList());

                if (allVisitors.size() < visitors.size() || allVisitors.size() < FarmHelperConfig.visitorsMacroMinVisitors) {
                    LogUtils.sendDebug("[Visitors Macro] Waiting for visitors to spawn...");
                    return;
                }

                Entity closest = allVisitors.stream()
                        .filter(entity -> entity.getDistanceToEntity(mc.thePlayer) < 14)
                        .filter(entity -> servedCustomers.stream().noneMatch(s -> s.equals(entity)))
                        .min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)))
                        .orElse(null);

                if (closest == null) {
                    LogUtils.sendDebug("[Visitors Macro] Couldn't find the closest visitor to go to, waiting...");
                    delayClock.schedule(getRandomDelay());
                    break;
                }

                closestEntity = closest;
                List<BlockPos> blocksAroundVisitor = BlockUtils.getBlocksAroundEntity(closest);
                BlockPos closestToPlayer = blocksAroundVisitor.stream().min(Comparator.comparingDouble(blockPos -> mc.thePlayer.getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ()))).orElse(null);
                assert closestToPlayer != null;
                deskRotation = new Vec3(closestToPlayer.getX() + 0.5, closestToPlayer.getY() + 1.5, closestToPlayer.getZ() + 0.5);
                Rotation rotationToDesk = rotation.getRotation(deskRotation);
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(rotationToDesk.getYaw(), rotationToDesk.getPitch()), FarmHelperConfig.getRandomRotationTime(), null
                        ).easeOutBack(true)
                );
                setTravelState(TravelState.MOVE_TOWARDS_DESK);
                previousDistanceToCheck = Integer.MAX_VALUE;
                break;
            case MOVE_TOWARDS_DESK:
                BlockPos playerPos = BlockUtils.getRelativeBlockPos(0, 0, 0);
                BlockPos deskPos = new BlockPos(deskRotation.xCoord, mc.thePlayer.posY, deskRotation.zCoord);
                double distance = Math.sqrt(playerPos.distanceSq(deskPos));
                stuckClock.schedule(STUCK_DELAY);
                if (distance <= 1f || playerPos.equals(deskPos) || (previousDistanceToCheck < distance && distance < 1.75f) || mc.thePlayer.getDistance(closestEntity.posX, mc.thePlayer.posY, closestEntity.posZ) < 3) {
                    if (pathFound) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                        pathFound = false;
                    }
                    KeyBindUtils.stopMovement();
                    setTravelState(TravelState.END);
                    delayClock.schedule(getRandomDelay());
                } else if (!pathFound) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().setGoal(new GoalBlock(new BetterBlockPos(deskPos)));
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().findPath(new BetterBlockPos(BlockUtils.getRelativeBlockPos(0, 0, 0)));
                    pathFound = true;
                    stuckClock.schedule(STUCK_DELAY);
                    delayClock.schedule(getRandomDelay());
                }
                previousDistanceToCheck = distance;
                break;
            case END:
                if (!mc.thePlayer.onGround) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                    break;
                }
                if (FarmHelperConfig.visitorsMacroAutosellBeforeServing) {
                    setMainState(MainState.AUTO_SELL);
                } else if (InventoryUtils.hasItemInHotbar("Compactor")) {
                    enableCompactors = false;
                    setMainState(MainState.COMPACTORS);
                } else {
                    setMainState(MainState.VISITORS);
                }
                setTravelState(TravelState.NONE);
                break;
        }
    }

    private void onCompactorState() {
        switch (compactorState) {
            case NONE:
                setCompactorState(CompactorState.GET_LIST);
                break;
            case GET_LIST:
                compactors.clear();
                for (int i = 0; i < 9; i++) {
                    ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (itemStack != null && itemStack.getDisplayName().contains("Compactor")) {
                        compactors.add(i);
                    }
                }
                if (compactors.isEmpty()) {
                    LogUtils.sendWarning("[Visitors Macro] The player does not have any compactors in the hotbar, skipping...");
                    setMainState(MainState.VISITORS);
                    return;
                }
                boolean foundRotationToEntity = false;
                Vec3 playerPos = mc.thePlayer.getPositionEyes(1);
                for (int i = 0; i < 6; i++) {
                    if (findRotation(playerPos, i)) {
                        foundRotationToEntity = true;
                        break;
                    }
                }
                if (!foundRotationToEntity) {
                    for (int i = 0; i > -6; i--) {
                        if (findRotation(playerPos, i)) {
                            foundRotationToEntity = true;
                            break;
                        }
                    }
                }
                if (!foundRotationToEntity) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find a rotation to the compactor, moving away a little");
                    if (GameStateHandler.getInstance().isBackWalkable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                        Multithreading.schedule(KeyBindUtils::stopMovement, 150, TimeUnit.MILLISECONDS);
                    } else if (GameStateHandler.getInstance().isLeftWalkable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
                        Multithreading.schedule(KeyBindUtils::stopMovement, 150, TimeUnit.MILLISECONDS);
                    } else if (GameStateHandler.getInstance().isRightWalkable()) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                        Multithreading.schedule(KeyBindUtils::stopMovement, 150, TimeUnit.MILLISECONDS);
                    } else {
                        LogUtils.sendError("[Visitors Macro] Couldn't find a way to move away from the compactor, restarting the macro...");
                        stop();
                        forceStart = true;
                        start();
                        return;
                    }
                    delayClock.schedule(300);
                    return;
                }
                setCompactorState(CompactorState.HOLD_COMPACTOR);
                break;
            case HOLD_COMPACTOR:
                if (rotation.isRotating()) return;
                LogUtils.sendDebug("[Visitors Macro] Holding compactor");
                if (compactors.isEmpty()) {
                    LogUtils.sendWarning("[Visitors Macro] All compactors have been disabled");
                    setCompactorState(CompactorState.END);
                    return;
                }
                mc.thePlayer.inventory.currentItem = compactors.get(0);
                compactors.remove(0);
                setCompactorState(CompactorState.OPEN_COMPACTOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case OPEN_COMPACTOR:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                KeyBindUtils.rightClick();
                setCompactorState(CompactorState.TOGGLE_COMPACTOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case TOGGLE_COMPACTOR:
                if (mc.currentScreen == null) {
                    setCompactorState(CompactorState.GET_LIST);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String invName = InventoryUtils.getInventoryName();
                if (invName != null && !invName.contains("Compactor")) {
                    LogUtils.sendDebug("[Visitors Macro] Not in compactor, opening compactor again...");
                    setCompactorState(CompactorState.GET_LIST);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                int slot = InventoryUtils.getSlotIdOfItemInContainer("Compactor Currently");
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
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLOSE_COMPACTOR:
                LogUtils.sendDebug("[Visitors Macro] Closing compactor");
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                }
                setCompactorState(CompactorState.HOLD_COMPACTOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case END:
                if (enableCompactors) {
                    setMainState(MainState.END);
                    enableCompactors = false;
                } else {
                    enableCompactors = true;
                    setMainState(MainState.VISITORS);
                }
                setCompactorState(CompactorState.NONE);
                delayClock.schedule(getRandomDelay());
                break;
        }
    }

    private boolean findRotation(Vec3 playerPos, int i) {
        float yawToCheck = (float) (mc.thePlayer.rotationYaw + (i * 15) + Math.random() * 5 - 2.5);
        float pitchToCheck = mc.thePlayer.rotationPitch + (float) (Math.random() * 5 - 2.5);
        Vec3 testRotation = AngleUtils.getVectorForRotation(pitchToCheck, yawToCheck);
        Vec3 lookVector = playerPos.addVector(testRotation.xCoord * 5, testRotation.yCoord * 5, testRotation.zCoord * 5);
        List<Entity> entitiesInFront = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().addCoord(testRotation.xCoord * 5, testRotation.yCoord * 5, testRotation.zCoord * 5).expand(1, 1, 1));
        if (!entitiesInFront.isEmpty()) {
            for (Entity entity : entitiesInFront) {
                AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox().expand(entity.getCollisionBorderSize(), entity.getCollisionBorderSize(), entity.getCollisionBorderSize());
                MovingObjectPosition movingObjectPosition = entityBoundingBox.calculateIntercept(playerPos, lookVector);

                if (entityBoundingBox.isVecInside(playerPos)) {
                    LogUtils.sendDebug("[Visitors Macro] Player is inside entity");
                    return false;
                } else if (movingObjectPosition != null) {
                    LogUtils.sendDebug("[Visitors Macro] Found entity in front of player");
                    return false;
                }
            }
        }
        LogUtils.sendDebug("Found rotation: " + yawToCheck + " " + pitchToCheck);
        rotation.easeTo(
                new RotationConfiguration(
                        new Rotation(yawToCheck, pitchToCheck), FarmHelperConfig.getRandomRotationTime(), null
                ).easeOutBack(true)
        );
        return true;
    }

    private void onVisitorsState() {
        switch (visitorsState) {
            case NONE:
                if (visitors.isEmpty() && manuallyStarted) {
                    LogUtils.sendDebug("[Visitors Macro] No visitors in the queue, waiting...");
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
                break;
            case GET_CLOSEST_VISITOR:
                LogUtils.sendDebug("[Visitors Macro] Getting the closest visitor");
                if (PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false) != -1) {
                    mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
                }
                if (visitors.isEmpty() || visitors.stream().noneMatch(s -> servedCustomers.stream().noneMatch(s2 -> StringUtils.stripControlCodes(s2.getCustomNameTag()).contains(StringUtils.stripControlCodes(s))))) {
                    LogUtils.sendWarning("[Visitors Macro] No visitors in queue...");
                    setVisitorsState(VisitorsState.END);
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                Entity closest = mc.theWorld.getLoadedEntityList().
                        stream().
                        filter(entity ->
                                entity.hasCustomName() &&
                                        visitors.stream().anyMatch(
                                                v ->
                                                        StringUtils.stripControlCodes(v).contains(StringUtils.stripControlCodes(entity.getCustomNameTag()))))
                        .filter(entity -> entity.getDistance(mc.thePlayer.posX, entity.posY, mc.thePlayer.posZ) < 6)
                        .filter(entity -> servedCustomers.stream().noneMatch(s -> s.equals(entity)))
                        .min(Comparator.comparingDouble(entity -> entity.getDistanceToEntity(mc.thePlayer)))
                        .orElse(null);
                if (closest == null) {
                    LogUtils.sendWarning("[Visitors Macro] Couldn't find the closest visitor, getting a little closer...");
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
                    shouldJump();
                    return;
                }
                KeyBindUtils.stopMovement();
                Entity character = PlayerUtils.getEntityCuttingOtherEntity(closest);

                if (character == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the character of closest visitor, restarting the macro...");
                    stop();
                    forceStart = true;
                    start();
                    return;
                }

                LogUtils.sendDebug("[Visitors Macro] Closest visitor: " + closest.getCustomNameTag());
                currentVisitor = Optional.of(closest);
                currentCharacter = Optional.of(character);
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                differentOptionCounter = 0;
                rotation.reset();
                break;
            case ROTATE_TO_VISITOR:
                if (currentVisitor.isPresent()) {
                    if (rotation.isRotating()) return;
                    LogUtils.sendDebug("Position of visitor: " + currentVisitor.get().getPositionEyes(1));
                    Rotation rotationToVisitor = rotation.getRotation(currentVisitor.get(), true);
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Rotation(rotationToVisitor.getYaw(), rotationToVisitor.getPitch()), FarmHelperConfig.getRandomRotationTime(), null
                            ).easeOutBack(true)
                    );
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
                if (rotation.isRotating()) return;
                assert currentVisitor.isPresent();
                if (moveAwayIfPlayerTooClose()) return;
                itemsToBuy.clear();
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    assert currentCharacter.isPresent();
                    if (entity.equals(currentVisitor.get()) || entity.equals(currentCharacter.get()) || entity.getCustomNameTag().contains("CLICK") && entity.getDistanceToEntity(currentVisitor.get()) < 1) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        setVisitorsState(VisitorsState.GET_LIST);
                        KeyBindUtils.leftClick();
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Looking at something else");
                        LogUtils.sendDebug("[Visitors Macro] Looking at: " + entity.getName());
                        LogUtils.sendDebug("[Visitors Macro] Correct Entities: " + currentVisitor.get().getName() + " " + currentCharacter.get().getName());
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    }
                    break;
                } else {
                    LogUtils.sendDebug("[Visitors Macro] Looking at nothing");
                    LogUtils.sendDebug("[Visitors Macro] Distance: " + mc.thePlayer.getDistanceToEntity(currentVisitor.get()));
                    if (mc.thePlayer.getDistanceToEntity(currentVisitor.get()) > 3) {
                        LogUtils.sendDebug("[Visitors Macro] Visitor is too far away, getting closer...");
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
                        shouldJump();
                        Multithreading.schedule(KeyBindUtils::stopMovement, 150, TimeUnit.MILLISECONDS);
                    } else if (mc.thePlayer.getDistanceToEntity(currentVisitor.get()) < 1.5) {
                        LogUtils.sendDebug("[Visitors Macro] The current visitor is too close, getting further...");
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                        Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                    } else {
                        if (differentOptionCounter < 3) {
                            differentOptionCounter++;
                            LogUtils.sendDebug("[Visitors Macro] Looking at nothing, trying different option");
                            setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                            break;
                        }
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, currentVisitor.get());
                        LogUtils.sendDebug("[Visitors Macro] Opening Visitor with different option");
                        setVisitorsState(VisitorsState.GET_LIST);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    }
                    delayClock.schedule(300);
                    setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
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
                haveItemsInSack = false;
                String npcName = isNpc ? StringUtils.stripControlCodes(npcSlot.getStack().getDisplayName()) : "";
                assert currentVisitor.isPresent();
                if (npcName.isEmpty() || !StringUtils.stripControlCodes(npcName).contains(StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()))) {
                    LogUtils.sendError("[Visitors Macro] Opened wrong NPC.");
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    differentOptionCounter = 0;
                    PlayerUtils.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                Rarity npcRarity = Rarity.getRarityFromNpcName(npcSlot.getStack().getDisplayName());
                LogUtils.sendDebug("[Visitors Macro] Opened NPC: " + npcName + " Rarity: " + npcRarity);

                Slot acceptOfferSlot = InventoryUtils.getSlotOfItemInContainer("Accept Offer");
                if (acceptOfferSlot == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the \"Accept Offer\" slot!");
                    break;
                }
                ItemStack acceptOfferItemStack = acceptOfferSlot.getStack();
                if (acceptOfferItemStack == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the \"Accept Offer\" slot!");
                    break;
                }
                ArrayList<String> loreAcceptOffer = InventoryUtils.getItemLore(acceptOfferItemStack);
                boolean foundRequiredItems = false;
                boolean foundRewards = false;
                for (String line : loreAcceptOffer) {
                    if (line.toLowerCase().contains("click to give")) {
                        haveItemsInSack = true;
                        continue;
                    }
                    if (line.contains("Required:")) {
                        foundRequiredItems = true;
                        continue;
                    }
                    if (foundRewards && line.trim().isEmpty()) {
                        continue;
                    }
                    if (line.trim().contains("Rewards:") || line.trim().isEmpty() && foundRequiredItems) {
                        foundRewards = true;
                        foundRequiredItems = false;
                        continue;
                    }
                    if (foundRequiredItems) {
                        Matcher matcher = itemNamePattern.matcher(StringUtils.stripControlCodes(line).trim());
                        if (matcher.matches()) {
                            String itemName = matcher.group(1);
                            String quantity = matcher.group(2);
                            long amount = (quantity != null) ? Long.parseLong(quantity) : 1L;
                            itemsToBuy.add(Pair.of(itemName, amount));
                            LogUtils.sendWarning("[Visitors Macro] Required item: " + itemName + " Amount: " + amount);
                        }
                    }
                    if (foundRewards) {
                        if (line.contains("+")) {
                            String[] split = StringUtils.stripControlCodes(line).trim().split(" ");
                            String amount = split[0].trim();
                            String item = String.join(" ", Arrays.copyOfRange(split, 1, split.length)).trim();
                            currentRewards.add(new Tuple<>(item, amount));
                        }
                    }
                }

                if (itemsToBuy.isEmpty()) {
                    LogUtils.sendDebug("[Visitors Macro] Something went wrong with collecting required items...");
                    stop();
                    forceStart = true;
                    start();
                    return;
                }
                LogUtils.sendDebug("[Visitors Macro] Items to buy: " + itemsToBuy);

                switch (npcRarity) {
                    case UNKNOWN:
                        LogUtils.sendDebug("[Visitors Macro] The visitor is unknown rarity. Accepting offer...");
                        break;
                    case UNCOMMON:
                        if (FarmHelperConfig.visitorsActionUncommon == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionUncommon == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                    case RARE:
                        if (FarmHelperConfig.visitorsActionRare == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is rare rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionRare == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is rare rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                    case LEGENDARY:
                        if (FarmHelperConfig.visitorsActionLegendary == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is legendary rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionLegendary == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is legendary rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                    case MYTHIC:
                        if (FarmHelperConfig.visitorsActionMythic == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is mythic rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionMythic == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is mythic rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                    case SPECIAL:
                        if (FarmHelperConfig.visitorsActionSpecial == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is special rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionSpecial == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is special rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                }

                if (haveItemsInSack && !rejectVisitor) {
                    setVisitorsState(VisitorsState.FINISH_VISITOR);
                    break;
                }

                setVisitorsState(VisitorsState.CLOSE_VISITOR);
                break;
            case CLOSE_VISITOR:
                if (rejectVisitor) {
                    if (mc.currentScreen == null) {
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                        differentOptionCounter = 0;
                        delayClock.schedule(getRandomDelay());
                        break;
                    }
                    rejectCurrentVisitor();
                    break;
                }
                LogUtils.sendDebug("[Visitors Macro] Closing menu");
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                }
                setVisitorsState(VisitorsState.BUY_STATE);
                delayClock.schedule(getRandomDelay());
                break;
            case BUY_STATE:
                onBuyState();
                break;
            case ROTATE_TO_VISITOR_2:
                if (mc.currentScreen != null) return;
                if (rotation.isRotating()) return;
                if (PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false) != -1) {
                    mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, false);
                }
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    assert currentVisitor.isPresent();
                    assert currentCharacter.isPresent();
                    if (entity.equals(currentVisitor.get()) || entity.equals(currentCharacter.get()) || entity.getCustomNameTag().contains("CLICK") && entity.getDistanceToEntity(currentVisitor.get()) < 1) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                        delayClock.schedule(FarmHelperConfig.getRandomRotationTime());
                        break;
                    }
                }
                LogUtils.sendDebug("Position of visitor: " + currentVisitor.get().getPositionEyes(1));
                Rotation rotationToVisitor = rotation.getRotation(currentVisitor.get(), true);
                rotation.easeTo(
                        new RotationConfiguration(
                                new Rotation(rotationToVisitor.getYaw(), rotationToVisitor.getPitch()), FarmHelperConfig.getRandomRotationTime(), null
                        ).easeOutBack(true)
                );
                setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                delayClock.schedule(FarmHelperConfig.getRandomRotationTime());
                break;
            case OPEN_VISITOR_2:
                if (mc.currentScreen != null) {
                    setVisitorsState(VisitorsState.FINISH_VISITOR);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                if (rotation.isRotating()) return;
                assert currentVisitor.isPresent();
                if (moveAwayIfPlayerTooClose()) return;
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    assert currentCharacter.isPresent();
                    if (entity.equals(currentVisitor.get()) || entity.equals(currentCharacter.get()) || entity.getCustomNameTag().contains("CLICK") && entity.getDistanceToEntity(currentVisitor.get()) < 1) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        setVisitorsState(VisitorsState.FINISH_VISITOR);
                        KeyBindUtils.leftClick();
                        delayClock.schedule(getRandomDelay());
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Looking at something else");
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                    }
                    break;
                } else {
                    LogUtils.sendDebug("[Visitors Macro] Looking at nothing");
                    LogUtils.sendDebug("[Visitors Macro] Distance: " + mc.thePlayer.getDistanceToEntity(currentVisitor.get()));
                    if (mc.thePlayer.getDistanceToEntity(currentVisitor.get()) > 3) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward);
                        shouldJump();
                        Multithreading.schedule(KeyBindUtils::stopMovement, 150, TimeUnit.MILLISECONDS);
                    } else if (mc.thePlayer.getDistanceToEntity(currentVisitor.get()) < 1.5) {
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                        Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                    } else {
                        if (differentOptionCounter < 3) {
                            differentOptionCounter++;
                            LogUtils.sendDebug("[Visitors Macro] Looking at nothing, trying different option");
                            setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                            differentOptionCounter = 0;
                            break;
                        }
                        mc.playerController.interactWithEntitySendPacket(mc.thePlayer, currentVisitor.get());
                        LogUtils.sendDebug("[Visitors Macro] Opening Visitor with different option");
                        setVisitorsState(VisitorsState.GET_LIST);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    }
                    delayClock.schedule(300);
                    setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
                }
                break;
            case FINISH_VISITOR:
                if (mc.currentScreen == null) {
                    setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                if (rejectVisitor) {
                    rejectCurrentVisitor();
                    break;
                }
                LogUtils.sendDebug("[Visitors Macro] Giving items to the visitor.");
                Slot acceptOfferSlot2 = InventoryUtils.getSlotOfItemInContainer("Accept Offer");
                if (acceptOfferSlot2 == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the \"Accept Offer\" slot!");
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                ItemStack acceptOfferItemStack2 = acceptOfferSlot2.getStack();
                if (acceptOfferItemStack2 == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the \"Accept Offer\" slot!");
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                if (!haveItemsInSack) {
                    for (Pair<String, Long> item : itemsToBuy) {
                        if (InventoryUtils.getAmountOfItemInInventory(item.getLeft()) < item.getRight()) {
                            LogUtils.sendError("[Visitors Macro] Missing item " + item.getLeft() + " amount " + item.getRight());
                            setVisitorsState(VisitorsState.BUY_STATE);
                            PlayerUtils.closeScreen();
                            delayClock.schedule(getRandomDelay());
                            return;
                        }
                    }
                }
                haveItemsInSack = false;
                InventoryUtils.clickContainerSlot(acceptOfferSlot2.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                if (FarmHelperConfig.sendVisitorsMacroLogs)
                    LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro accepted visitor: " + StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()), FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs, currentRewards.toArray(new Tuple[0]));
                currentVisitor.ifPresent(servedCustomers::add);
                currentRewards.clear();
                setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case END:
                servedCustomers.clear();
                currentRewards.clear();
                LogUtils.sendSuccess("[Visitors Macro] Spent §2" + ProfitCalculator.getInstance().getFormatter().format(spentMoney) + " on visitors");
                if (!manuallyStarted) {
                    if (enableCompactors) {
                        setMainState(MainState.COMPACTORS);
                    } else {
                        setMainState(MainState.END);
                    }
                }
                setVisitorsState(VisitorsState.NONE);
                delayClock.schedule(getRandomDelay());
                break;
        }
    }

    private boolean moveAwayIfPlayerTooClose() {
        if (mc.thePlayer.getDistanceToEntity(currentVisitor.get()) < 0.5) {
            if (GameStateHandler.getInstance().isBackWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindBack);
                Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                return true;
            }
            if (GameStateHandler.getInstance().isLeftWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft);
                Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                return true;
            }
            if (GameStateHandler.getInstance().isRightWalkable()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                return true;
            }
        }
        return false;
    }

    private void shouldJump() {
        if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, 1))
                && BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 1, 1)) && mc.thePlayer.onGround) {
            rotation.reset();
            mc.thePlayer.jump();
        }
    }

    private void checkIfCurrentVisitorIsProfitable() {
        if (itemsToBuy.stream().anyMatch(item -> {
            String name = StringUtils.stripControlCodes(item.getLeft());
            return profitRewards.stream().anyMatch(reward -> reward.contains(name));
        })) {
            LogUtils.sendDebug("[Visitors Macro] The visitor is profitable");
            String profitableReward = itemsToBuy.stream().filter(item -> {
                String name = StringUtils.stripControlCodes(item.getLeft());
                return profitRewards.stream().anyMatch(reward -> reward.contains(name));
            }).findFirst().get().getLeft();

            if (FarmHelperConfig.sendVisitorsMacroLogs)
                LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro found profitable item: " + profitableReward, FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs);
            LogUtils.sendDebug("[Visitors Macro] Accepting offer...");
        } else {
            LogUtils.sendWarning("[Visitors Macro] The visitor is not profitable, skipping...");
            rejectVisitor = true;
        }
    }

    private void rejectCurrentVisitor() {
        if (rejectVisitor()) return;
        if (FarmHelperConfig.sendVisitorsMacroLogs)
            LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro rejected visitor: " + StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()), FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs, currentRewards.toArray(new Tuple[0]));
        currentVisitor.ifPresent(servedCustomers::add);
        currentRewards.clear();
        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
        setVisitorsState(VisitorsState.GET_CLOSEST_VISITOR);
    }

    private boolean rejectVisitor() {
        LogUtils.sendDebug("[Visitors Macro] Rejecting the visitor");
        Slot rejectOfferSlot = InventoryUtils.getSlotOfItemInContainer("Refuse Offer");
        if (rejectOfferSlot == null || rejectOfferSlot.getStack() == null) {
            LogUtils.sendError("[Visitors Macro] Couldn't find the \"Reject Offer\" slot!");
            delayClock.schedule(getRandomDelay());
            return true;
        }
        InventoryUtils.clickContainerSlot(rejectOfferSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
        rejectVisitor = false;
        return false;
    }

    private void onBuyState() {
        switch (buyState) {
            case NONE:
                if (itemsToBuy.isEmpty()) {
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                    differentOptionCounter = 0;
                    break;
                }
                if (InventoryUtils.getAmountOfItemInInventory(itemsToBuy.get(0).getLeft()) >= itemsToBuy.get(0).getRight()) {
                    LogUtils.sendDebug("[Visitors Macro] Already have " + itemsToBuy.get(0).getLeft() + ", skipping...");
                    itemsToBuy.remove(0);
                    return;
                }
                setBuyState(BuyState.OPEN_BZ);
                break;
            case OPEN_BZ:
                if (mc.currentScreen == null) {
                    Pair<String, Long> firstItem = itemsToBuy.get(0);
                    String itemName = firstItem.getLeft();
                    long amount = firstItem.getRight();
                    LogUtils.sendDebug("[Visitors Macro] Opening Bazaar for item " + itemName + " amount " + amount);
                    mc.thePlayer.sendChatMessage("/bz " + itemName.toLowerCase());
                }
                setBuyState(BuyState.CLICK_CROP);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_CROP:
                if (mc.currentScreen == null) {
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String chestName = InventoryUtils.getInventoryName();
                if (chestName == null) break;
                if (!chestName.startsWith("Bazaar ➜ \"")) {
                    LogUtils.sendError("[Visitors Macro] Opened wrong Bazaar Menu");
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    PlayerUtils.closeScreen();
                    break;
                }
                Pair<String, Long> firstItem = itemsToBuy.get(0);
                String itemName = firstItem.getLeft();
                Slot cropSlot = InventoryUtils.getSlotOfItemInContainer(itemName, true);
                if (cropSlot == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find crop slot");
                    rejectVisitor = true;
                    setVisitorsState(VisitorsState.CLOSE_VISITOR);
                    PlayerUtils.closeScreen();
                    break;
                }
                ItemStack cropItemStack = cropSlot.getStack();
                if (cropItemStack == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find crop item");
                    rejectVisitor = true;
                    setVisitorsState(VisitorsState.CLOSE_VISITOR);
                    PlayerUtils.closeScreen();
                    break;
                }
                InventoryUtils.clickContainerSlot(cropSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.CLICK_BUY);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_BUY:
                if (mc.currentScreen == null) {
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String chestName2 = InventoryUtils.getInventoryName();
                if (chestName2 == null) break;
                Slot buyInstantly = InventoryUtils.getSlotOfItemInContainer("Buy Instantly");
                if (buyInstantly == null) break;
                ItemStack buyInstantlyItemStack = buyInstantly.getStack();
                if (buyInstantlyItemStack == null) break;

                ArrayList<String> lore = InventoryUtils.getItemLore(buyInstantlyItemStack);
                float pricePerUnit = -1;

                for (String line : lore) {
                    if (line.toLowerCase().contains("per unit")) {
                        String[] split = line.split(":");
                        pricePerUnit = Float.parseFloat(split[1].replace(",", "").replace("coins", "").trim());
                        break;
                    }
                }

                if (pricePerUnit == -1) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the price per unit for " + itemsToBuy.get(0).getLeft() + " in the Bazaar menu. Report it to the developer.");
                } else {
                    LogUtils.sendDebug("[Visitors Macro] Price per unit for " + itemsToBuy.get(0).getLeft() + " is " + pricePerUnit);
                }

                ProfitCalculator.BazaarItem bazaarItem = ProfitCalculator.getInstance().getVisitorsItem("_" + itemsToBuy.get(0).getLeft());
                if (bazaarItem != null) {
                    if (pricePerUnit > bazaarItem.npcPrice * FarmHelperConfig.visitorsMacroPriceManipulationMultiplier) {
                        LogUtils.sendDebug("[Visitors Macro] Price manipulation detected, skipping...");
                        LogUtils.sendDebug("[Visitors Macro] Current price: " + pricePerUnit + " Npc price: " + bazaarItem.npcPrice + " Npc price after manipulation: " + bazaarItem.npcPrice * FarmHelperConfig.visitorsMacroPriceManipulationMultiplier);
                        rejectVisitor = true;
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                        differentOptionCounter = 0;
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        PlayerUtils.closeScreen();
                        break;
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Price manipulation not detected. NPC Price per item: " + bazaarItem.npcPrice + " Bazaar Price per item: " + pricePerUnit);
                    }
                } else {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the crop price in the API data. Can't check if the price has been manipulated. Buying anyway...");
                }

                InventoryUtils.clickContainerSlot(buyInstantly.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                if (itemsToBuy.get(0).getRight() == 1) {
                    setBuyState(BuyState.CLICK_BUY_ONLY_ONE);
                } else {
                    setBuyState(BuyState.CLICK_SIGN);
                }
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_BUY_ONLY_ONE:
                if (mc.currentScreen == null) {
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String chestName3 = InventoryUtils.getInventoryName();
                if (chestName3 == null) break;
                Slot buyOnlyOne = InventoryUtils.getSlotOfItemInContainer("Buy only one!");
                if (buyOnlyOne == null) break;
                ItemStack buyOnlyOneItemStack = buyOnlyOne.getStack();
                if (buyOnlyOneItemStack == null) break;
                setBuyState(BuyState.WAIT_FOR_BUY);
                InventoryUtils.clickContainerSlot(buyOnlyOne.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case CLICK_SIGN:
                if (mc.currentScreen == null) {
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String chestName4 = InventoryUtils.getInventoryName();
                if (chestName4 == null) break;
                Slot signSlot = InventoryUtils.getSlotOfItemInContainer("Custom Amount");
                if (signSlot == null) break;
                ItemStack signItemStack = signSlot.getStack();
                if (signItemStack == null) break;
                InventoryUtils.clickContainerSlot(signSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                setBuyState(BuyState.CLICK_CONFIRM);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay() * 2);
                Multithreading.schedule(() -> SignUtils.setTextToWriteOnString(itemsToBuy.get(0).getRight().toString()), (long) (400 + Math.random() * 400), TimeUnit.MILLISECONDS);
                break;
            case CLICK_CONFIRM:
                if (mc.currentScreen == null) {
                    setBuyState(BuyState.OPEN_BZ);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String chestName5 = InventoryUtils.getInventoryName();
                if (chestName5 == null) break;
                if (!chestName5.equals("Confirm Instant Buy")) break;
                Slot confirmSlot = InventoryUtils.getSlotOfItemInContainer("Custom Amount");
                if (confirmSlot == null) break;
                ItemStack confirmItemStack = confirmSlot.getStack();
                if (confirmItemStack == null) break;
                setBuyState(BuyState.WAIT_FOR_BUY);
                InventoryUtils.clickContainerSlot(confirmSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case WAIT_FOR_BUY:
                // waiting
                break;
            case CLOSE_BZ:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                }
                if (itemsToBuy.isEmpty()) {
                    setBuyState(BuyState.END);
                } else {
                    setBuyState(BuyState.OPEN_BZ);
                }
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case END:
                setBuyState(BuyState.NONE);
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                differentOptionCounter = 0;
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

    public boolean isInBarn() {
        BlockPos barn1 = new BlockPos(-30, 65, -45);
        BlockPos barn2 = new BlockPos(36, 80, -2);
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(barn1, barn2);
        return axisAlignedBB.isVecInside(mc.thePlayer.getPositionVector());
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onReceiveChat(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!isRunning()) return;
        if (!currentVisitor.isPresent()) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (visitorsState == VisitorsState.GET_LIST) {
            String npcName = StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag());
            if (msg.startsWith("[NPC] " + npcName + ":")) {
                Multithreading.schedule(() -> {
                    if (mc.currentScreen == null) {
                        KeyBindUtils.leftClick();
                    }
                }, (long) (250 + Math.random() * 150), TimeUnit.MILLISECONDS);
            }
        }
        if (buyState == BuyState.WAIT_FOR_BUY) {
            if (msg.startsWith("[Bazaar] Bought ")) {
                LogUtils.sendDebug("[Visitors Macro] Bought item");
                String spentCoins = msg.split("for")[1].replace("coins!", "").trim();
                spentMoney += Float.parseFloat(spentCoins.replace(",", ""));
                itemsToBuy.remove(0);
                setBuyState(BuyState.CLOSE_BZ);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
            }
        }
        if (buyState == BuyState.CLICK_CROP || buyState == BuyState.OPEN_BZ) {
            if (msg.startsWith("You need the Cookie Buff")) {
                if (mc.currentScreen != null && mc.thePlayer != null) {
                    PlayerUtils.closeScreen();
                }
                LogUtils.sendDebug("[Visitors Macro] Cookie buff is needed. Skipping...");
                setBuyState(BuyState.NONE);
                setVisitorsState(VisitorsState.NONE);
                setMainState(MainState.END);
            }
        }
    }

    enum MainState {
        NONE,
        TRAVEL,
        AUTO_SELL,
        COMPACTORS,
        VISITORS,
        END,
        DISABLING
    }

    enum TravelState {
        NONE,
        ROTATE_TO_DESK,
        MOVE_TOWARDS_DESK,
        END
    }

    enum CompactorState {
        NONE,
        GET_LIST,
        HOLD_COMPACTOR,
        OPEN_COMPACTOR,
        TOGGLE_COMPACTOR,
        CLOSE_COMPACTOR,
        END
    }

    enum VisitorsState {
        NONE,
        GET_CLOSEST_VISITOR,
        ROTATE_TO_VISITOR,
        OPEN_VISITOR,
        GET_LIST,
        CLOSE_VISITOR,
        BUY_STATE,
        ROTATE_TO_VISITOR_2,
        OPEN_VISITOR_2,
        FINISH_VISITOR,
        END
    }

    enum Rarity {
        UNKNOWN,
        UNCOMMON,
        RARE,
        LEGENDARY,
        MYTHIC,
        SPECIAL;

        public static Rarity getRarityFromNpcName(String npcName) {
            npcName = npcName.replace("§f", "");
            if (npcName.startsWith("§a")) {
                return UNCOMMON;
            } else if (npcName.startsWith("§9")) {
                return RARE;
            } else if (npcName.startsWith("§6")) {
                return LEGENDARY;
            } else if (npcName.startsWith("§d")) {
                return MYTHIC;
            } else if (npcName.startsWith("§c")) {
                return SPECIAL;
            } else {
                return UNKNOWN;
            }
        }
    }

    enum BuyState {
        NONE,
        OPEN_BZ,
        CLICK_CROP,
        CLICK_BUY,
        CLICK_BUY_ONLY_ONE,
        CLICK_SIGN,
        CLICK_CONFIRM,
        WAIT_FOR_BUY,
        CLOSE_BZ,
        END
    }
}
