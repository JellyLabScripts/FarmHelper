package com.jelly.farmhelperv2.feature.impl;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.BaritoneHandler;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VisitorsMacro implements IFeature {
    private static VisitorsMacro instance;
    public final List<String> profitRewards = Arrays.asList("Dedication", "Cultivating", "Delicate", "Replenish", "Music Rune", "Green Bandana", "Overgrown Grass", "Space Helmet");
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ArrayList<Integer> compactors = new ArrayList<>();
    private final ArrayList<Entity> servedCustomers = new ArrayList<>();
    private final ArrayList<Pair<String, Integer>> itemsToBuy = new ArrayList<>();
    @Getter
    private final Clock delayClock = new Clock();
    @Getter
    private final Clock stuckClock = new Clock();
    private final Clock afkDelay = new Clock();
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.macroGuiDelay + FarmHelperConfig.macroGuiDelayRandomness);
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final ArrayList<String> visitors = new ArrayList<>();
    Pattern itemNamePattern = Pattern.compile("^(.*?)(?:\\sx(\\d+))?$");
    @Getter
    private MainState mainState = MainState.NONE;
    @Getter
    private TravelState travelState = TravelState.NONE;
    @Getter
    private CompactorState compactorState = CompactorState.NONE;
    private boolean compactorsDisabled = false;
    @Getter
    private VisitorsState visitorsState = VisitorsState.NONE;
    @Getter
    private Optional<Entity> currentVisitor = Optional.empty();
    @Getter
    private Optional<Entity> currentCharacter = Optional.empty();
    @Getter
    private final ArrayList<Tuple<String, String>> currentRewards = new ArrayList<>();
    private boolean rejectVisitor = false;
    private float spentMoney = 0;
    @Getter
    private BuyState buyState = BuyState.NONE;
    private boolean haveItemsInSack = false;
    private boolean enabled = false;
    @Getter
    @Setter
    private boolean manuallyStarted = false;
    private BlockPos positionBeforeTp = null;
    private final List<Entity> ignoredNPCs = new ArrayList<>();
    private boolean profitNpc = false;
    private Optional<Vec3> closestVec = Optional.empty();

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
        if (visitors.isEmpty()) {
            LogUtils.sendWarning("[Visitors Macro] No visitors in queue, disabling...");
            if (enabled) {
                setMainState(MainState.END);
                return;
            }
            stop();
            return;
        }
        MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> macro.getRotation().reset());
        mainState = MainState.NONE;
        compactorsDisabled = false;
        travelState = TravelState.NONE;
        compactorState = CompactorState.NONE;
        visitorsState = VisitorsState.NONE;
        buyState = BuyState.NONE;
        enabled = true;
        rejectVisitor = false;
        closestVec = Optional.empty();
        if (PlayerUtils.isInBarn())
            setTravelState(TravelState.END);
        else
            setTravelState(TravelState.NONE);
        haveItemsInSack = false;
        delayClock.reset();
        rotation.reset();
        stuckClock.schedule(STUCK_DELAY);
        currentVisitor = Optional.empty();
        currentCharacter = Optional.empty();
        itemsToBuy.clear();
        compactors.clear();
        currentRewards.clear();
        servedCustomers.clear();
        spentMoney = 0;
        LogUtils.sendWarning("[Visitors Macro] Macro started");
        if (FarmHelperConfig.visitorsActionUncommon == 1
                || FarmHelperConfig.visitorsActionRare == 1
                || FarmHelperConfig.visitorsActionLegendary == 1
                || FarmHelperConfig.visitorsActionMythic == 1
                || FarmHelperConfig.visitorsActionSpecial == 1) {
            LogUtils.sendWarning("[Visitors Macro] Accepting profitable offers only for one or more visitors. " + String.join(", ", profitRewards));
        }
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
        }
        ignoredNPCs.clear();
        profitNpc = false;
        LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro started");
    }

    @Override
    public void stop() {
        enabled = false;
        manuallyStarted = false;
        LogUtils.sendWarning("[Visitors Macro] Macro stopped");
        rotation.reset();
        AutoBazaar.getInstance().stop();
        PlayerUtils.closeScreen();
        KeyBindUtils.stopMovement();
        BaritoneHandler.stopPathing();
        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.getCheckOnSpawnClock().schedule(5_000));
        Multithreading.schedule(() -> {
            servedCustomers.clear();
            ignoredNPCs.clear();
        }, 5_000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        ignoredNPCs.clear();
        if (!FarmHelperConfig.visitorsMacroAfkInfiniteMode) return;
        FarmHelperConfig.visitorsMacroAfkInfiniteMode = false;
        LogUtils.sendWarning("[Visitors Macro] AFK Mode has been disabled");
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.visitorsMacro;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return travelState != TravelState.WAIT_FOR_TP && mainState != MainState.END && compactorState == CompactorState.NONE;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!FarmHelperConfig.visitorsMacroAfkInfiniteMode) return;
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
            LogUtils.sendWarning("[Visitors Macro] AFK Mode has been disabled");
            stop();
            FarmHelperConfig.visitorsMacroAfkInfiniteMode = false;
        }
    }

    @SubscribeEvent
    public void onTickAFKMode(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (!FarmHelperConfig.visitorsMacroAfkInfiniteMode) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            FarmHelperConfig.visitorsMacroAfkInfiniteMode = false;
            FailsafeUtils.getInstance().sendNotification("[Visitors Macro] AFK Mode has been disabled", TrayIcon.MessageType.WARNING);
            return;
        }
        if (MacroHandler.getInstance().isMacroToggled()) return;
        if (!PlayerUtils.isInBarn()) return;
        if (isRunning()) return;
        if (!afkDelay.passed()) return;

        if (canEnableMacro(true, true)) {
            manuallyStarted = true;
            start();
        }
    }

    public boolean canEnableMacro(boolean manual, boolean withError) {
        if (!isToggled()) return false;
        if (isRunning()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled()) return false;

        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Visitors Macro] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            return false;
        }

        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            if (withError) LogUtils.sendError("[Visitors Macro] Cookie buff is not active, skipping...");
            return false;
        }

        if (!manual && FarmHelperConfig.pauseVisitorsMacroDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            if (withError) LogUtils.sendError("[Visitors Macro] Jacob's contest is active, skipping...");
            return false;
        }

        if (GameStateHandler.getInstance().getCurrentPurse() < FarmHelperConfig.visitorsMacroMinMoney * 1_000) {
            if (withError) LogUtils.sendError("[Visitors Macro] The player's purse is too low, skipping...");
            if (FarmHelperConfig.visitorsMacroAfkInfiniteMode) {
                LogUtils.sendWarning("[Visitors Macro] Your purse is too low. Disabling AFK mode");
                FarmHelperConfig.visitorsMacroAfkInfiniteMode = false;
            }
            return false;
        }

        List<String> visitorsWithoutIgnored = visitors.stream().filter(s -> ignoredNPCs.stream().noneMatch(s2 -> StringUtils.stripControlCodes(s2.getCustomNameTag()).contains(StringUtils.stripControlCodes(s)))).collect(Collectors.toList());
        List<String> visitorsWithoutServed = visitorsWithoutIgnored.stream().filter(s -> servedCustomers.stream().noneMatch(s2 -> StringUtils.stripControlCodes(s2.getCustomNameTag()).contains(StringUtils.stripControlCodes(s)))).collect(Collectors.toList());

        if (visitorsWithoutServed.size() < FarmHelperConfig.visitorsMacroMinVisitors) {
            if (withError) LogUtils.sendError("[Visitors Macro] Not enough Visitors in queue, skipping...");
            if (FarmHelperConfig.visitorsMacroAfkInfiniteMode) {
                LogUtils.sendDebug("[Visitors Macro] Waiting 15 seconds for visitors to spawn...");
                afkDelay.schedule(15_000);
            }
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public void onTickCheckVisitors(TickEvent.ClientTickEvent event) {
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase != TickEvent.Phase.START) return;

        List<String> tabList = TablistUtils.getTabList();
        if (tabList.size() < 2) return;
        boolean foundVisitors = false;
        ArrayList<String> newVisitors = new ArrayList<>();
        for (String line : tabList) {
            if (line.contains("Visitors:")) {
                foundVisitors = true;
                continue;
            }
            if (StringUtils.stripControlCodes(line).trim().isEmpty()
                    || newVisitors.size() == 5
                    || StringUtils.stripControlCodes(line).trim().contains("Next Visitor")) {
                if (foundVisitors) {
                    break;
                }
                continue;
            }
            if (foundVisitors) {
                newVisitors.add(line.replace("NEW!", "").trim());
            }
        }
        if (newVisitors.equals(visitors)) return;
        visitors.clear();
        visitors.addAll(newVisitors);
        LogUtils.sendDebug("[Visitors Macro] The visitors: " + visitors.size());
        if (visitors.isEmpty()) {
            servedCustomers.clear();
        }
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
        if (event.phase == TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Visitors Macro] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            stop();
            return;
        }

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            if (visitors.isEmpty()) {
                LogUtils.sendWarning("[Visitors Macro] The player is stuck but there are no visitors in the queue...");
                setMainState(MainState.END);
                stuckClock.reset();
                return;
            }
            LogUtils.sendError("[Visitors Macro] The player is stuck, restarting the macro...");
            stop();
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
                compactorsDisabled = false;
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
                if (FarmHelperConfig.visitorsMacroAfkInfiniteMode) {
                    LogUtils.sendWarning("[Visitors Macro] The macro has finished. Waiting 15 seconds for visitors to spawn...");
                    afkDelay.schedule(15_000);
                    enabled = false;
                } else if (manuallyStarted) {
                    stop();
                } else {
                    stop();
                    MacroHandler.getInstance().triggerWarpGarden(true, true, false);
                    delayClock.schedule(2_500);
                }
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
                setTravelState(TravelState.WAIT_FOR_TP);
                mc.thePlayer.sendChatMessage("/tptoplot barn");
                delayClock.schedule((long) (1_000 + Math.random() * 500));
                break;
            case WAIT_FOR_TP:
                if (mc.thePlayer.getDistanceSqToCenter(positionBeforeTp) < 3 || PlayerUtils.isPlayerSuffocating()) {
                    LogUtils.sendDebug("[Visitors Macro] Waiting for teleportation...");
                    return;
                }
                if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                    LogUtils.sendDebug("[Visitors Macro] The player is not on the ground, waiting...");
                    return;
                }
                KeyBindUtils.stopMovement();

                List<Entity> allVisitors = getVisitors().collect(Collectors.toList());

                if (allVisitors.size() < visitors.size() || allVisitors.size() < FarmHelperConfig.visitorsMacroMinVisitors) {
                    if (visitors.isEmpty()) {
                        setMainState(MainState.END);
                    }
                    LogUtils.sendDebug("[Visitors Macro] Waiting for visitors to spawn...");
                    return;
                }

                setTravelState(TravelState.END);
                break;
            case END:
                if (!mc.thePlayer.onGround) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                    break;
                }
                KeyBindUtils.stopMovement();
                if (FarmHelperConfig.visitorsMacroAutosellBeforeServing) {
                    setMainState(MainState.AUTO_SELL);
                } else if (InventoryUtils.hasItemInHotbar("Compactor")) {
                    compactorsDisabled = false;
                    setMainState(MainState.COMPACTORS);
                } else {
                    setMainState(MainState.VISITORS);
                }
                setTravelState(TravelState.NONE);
                break;
        }
    }

    private boolean shouldJump() {
        if (!mc.thePlayer.onGround) return false;
        Vec3 playerPos = mc.thePlayer.getPositionVector();
        Vec3 lookVector = mc.thePlayer.getLookVec();
        Vec3 feetBlock = playerPos.addVector(lookVector.xCoord * 0.6, 0.1, lookVector.zCoord * 0.6);
        Vec3 feetBlockUp = playerPos.addVector(lookVector.xCoord * 0.6, 0.6, lookVector.zCoord * 0.6);
        MovingObjectPosition movingObjectPosition = mc.theWorld.rayTraceBlocks(feetBlock, feetBlockUp, false, true, false);
        if (movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = movingObjectPosition.getBlockPos();
            if (BlockUtils.hasCollision(blockPos)) {
                return true;
            }
        }
        MovingObjectPosition movingObjectPosition2 = mc.theWorld.rayTraceBlocks(playerPos, feetBlock, false, true, false);
        if (movingObjectPosition2 != null && movingObjectPosition2.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = movingObjectPosition2.getBlockPos();
            return BlockUtils.hasCollision(blockPos);
        }

        return false;
    }

    private void onCompactorState() {
        switch (compactorState) {
            case NONE:
                setCompactorState(CompactorState.GET_LIST);
                break;
            case GET_LIST:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
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
                    LogUtils.sendError("[Visitors Macro] Couldn't find a rotation to the entity, moving away a little");
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
                        LogUtils.sendError("[Visitors Macro] Couldn't find a way to move away from the entity, restarting the macro...");
                        stop();
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
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                LogUtils.sendDebug("[Visitors Macro] Holding compactor");
                if (compactors.isEmpty()) {
                    LogUtils.sendWarning("[Visitors Macro] All compactors have been disabled");
                    setCompactorState(CompactorState.END);
                    return;
                }
                mc.thePlayer.inventory.currentItem = compactors.get(0);
                setCompactorState(CompactorState.OPEN_COMPACTOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case OPEN_COMPACTOR:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();
                if (currentItem == null || !currentItem.getDisplayName().contains("Compactor")) {
                    LogUtils.sendDebug("[Visitors Macro] Not holding compactor, holding compactor again...");
                    setCompactorState(CompactorState.HOLD_COMPACTOR);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                KeyBindUtils.rightClick();
                setCompactorState(CompactorState.TOGGLE_COMPACTOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case TOGGLE_COMPACTOR:
                if (mc.currentScreen == null) {
                    setCompactorState(CompactorState.OPEN_COMPACTOR);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                String invName = InventoryUtils.getInventoryName();
                if (invName != null && !invName.contains("Compactor")) {
                    LogUtils.sendDebug("[Visitors Macro] Not in compactor, opening compactor again...");
                    setCompactorState(CompactorState.OPEN_COMPACTOR);
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
                if (itemStack.getDisplayName().contains("OFF") && !compactorsDisabled) {
                    LogUtils.sendDebug("[Visitors Macro] Compactor is already OFF, skipping...");
                } else if (!compactorsDisabled && itemStack.getDisplayName().contains("ON")) {
                    LogUtils.sendDebug("[Visitors Macro] Disabling compactor in slot " + slot);
                    InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                }
                if (itemStack.getDisplayName().contains("ON") && compactorsDisabled) {
                    LogUtils.sendDebug("[Visitors Macro] Compactor is already ON, skipping...");
                } else if (compactorsDisabled && itemStack.getDisplayName().contains("OFF")) {
                    LogUtils.sendDebug("[Visitors Macro] Enabling compactor in slot " + slot);
                    InventoryUtils.clickContainerSlot(slot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                }
                compactors.remove(0);
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
                if (compactorsDisabled) {
                    setMainState(MainState.END);
                    compactorsDisabled = false;
                } else {
                    compactorsDisabled = true;
                    setMainState(MainState.VISITORS);
                }
                setCompactorState(CompactorState.NONE);
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
                ).easeOutBack(Math.random() > 0.5)
        );
        return true;
    }

    private void onVisitorsState() {
        switch (visitorsState) {
            case NONE:
                if (visitors.isEmpty() && FarmHelperConfig.visitorsMacroAfkInfiniteMode) {
                    LogUtils.sendDebug("[Visitors Macro] No visitors in the queue, waiting...");
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                break;
            case ROTATE_TO_VISITOR:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                if (BaritoneHandler.isWalkingToGoalBlock()) {
                    return;
                }
                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    if (!rotation.isRotating() && currentCharacter.isPresent()) {
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(currentCharacter.get()),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).followTarget(true)
                        );
                    }
                    return;
                }
                if (PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, true) != -1) {
                    mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, true);
                }
                if (!getVisitors().findAny().isPresent()) {
                    if (FarmHelperConfig.visitorsMacroAfkInfiniteMode) {
                        enabled = false;
                        afkDelay.schedule(15_000);
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] No visitors in queue...");
                        setVisitorsState(VisitorsState.END);
                        delayClock.schedule(getRandomDelay());
                    }
                    return;
                }
                Entity closest = getClosestVisitor();
                if (closest == null) {
                    System.out.println("[Visitors Macro] Couldn't find the closest visitor, waiting...");
                    // waiting
                    return;
                }
                VisitorEntities result = getVisitorEntities(closest);

                if (result == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the character of closest visitor, restarting the macro...");
                    stop();
                    start();
                    return;
                }
                LogUtils.sendDebug("Position of visitor: " + result.entityCharacter.getPositionEyes(1));
                if (mc.thePlayer.getDistanceToEntity(result.entityCharacter) > 3) {
                    Vec3 closestVec = PlayerUtils.getClosestVecAround(result.entityCharacter, 1.25, 90, 45);
                    if (closestVec == null) {
                        LogUtils.sendError("[Visitors Macro] Couldn't find a position to get closer");
                        stop();
                        FarmHelperConfig.visitorsMacro = false;
                        return;
                    }
                    this.closestVec = Optional.of(closestVec);
                    LogUtils.sendDebug("[Visitors Macro] Walking closer to visitor to: " + closestVec);
                    if (FarmHelperConfig.visitorsMacroUsePathFinder) {
//                        BaritoneHandler.walkCloserToBlockPos(result.entityCharacter.getPosition(), 1);
                        FlyPathFinderExecutor.getInstance().setSprinting(false);
                        FlyPathFinderExecutor.getInstance().setDontRotate(true);
                        FlyPathFinderExecutor.getInstance().findPath(closestVec.addVector(0, 1.8, 0), false, true);
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(result.entityCharacter),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).followTarget(true)
                        );
                    } else {
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(closestVec.addVector(0, 1.6, 0)),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).easeOutBack(Math.random() > 0.5)
                        );
                        setVisitorsState(VisitorsState.GET_TO_CLOSEST_VISITOR);
                    }
                } else {
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Target(result.entityCharacter),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).easeOutBack(Math.random() > 0.5)
                    );
                    setVisitorsState(VisitorsState.OPEN_VISITOR);
                }
                currentVisitor = Optional.of(result.nameArmorStand);
                currentCharacter = Optional.of(result.entityCharacter);
                break;
            case GET_TO_CLOSEST_VISITOR:
                LogUtils.sendDebug("[Visitors Macro] Getting the closest visitor");
                assert currentVisitor.isPresent();
                if (!rotation.isRotating())
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Target(closestVec.get().addVector(0, 1.6, 0)),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).easeOutBack(Math.random() > 0.5)
                    );
                if (getCloserToEntity()) break;
                KeyBindUtils.stopMovement();

                LogUtils.sendDebug("[Visitors Macro] Closest visitor: " + currentVisitor.get().getCustomNameTag());

                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                break;
            case OPEN_VISITOR:
                if (mc.currentScreen != null) {
                    setVisitorsState(VisitorsState.GET_LIST);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                assert currentVisitor.isPresent();
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    VisitorEntities result2 = getVisitorEntities(entity);
                    if (result2 == null) break;

                    if (result2.nameArmorStand.equals(currentVisitor.get()) || result2.entityCharacter.equals(currentCharacter.get()) || entity.equals(result2.entityClickStand)) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        setVisitorsState(VisitorsState.GET_LIST);
                        KeyBindUtils.leftClick();
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Looking at something else");
                        LogUtils.sendDebug("[Visitors Macro] Looking at: " + entity.getName());
                        moveSideways();
                    }
                    break;
                }
                if (rotation.isRotating()) return;
                if (moveAwayIfPlayerTooClose()) return;
                LogUtils.sendDebug("[Visitors Macro] Looking at nothing");
                LogUtils.sendDebug("[Visitors Macro] Distance: " + mc.thePlayer.getDistanceToEntity(currentCharacter.get()));
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                break;
            case GET_LIST:
                if (mc.currentScreen == null) {
                    setVisitorsState(VisitorsState.OPEN_VISITOR);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                Slot npcSlot = InventoryUtils.getSlotOfIdInContainer(13);
                if (npcSlot == null) break;
                ItemStack npcItemStack = npcSlot.getStack();
                if (npcItemStack == null) break;
                ArrayList<String> lore = InventoryUtils.getItemLore(npcItemStack);
                boolean isNpc = lore.size() == 4 && lore.get(3).contains("Offers Accepted: ");
                haveItemsInSack = false;
                itemsToBuy.clear();
                String npcName = isNpc ? StringUtils.stripControlCodes(npcSlot.getStack().getDisplayName()) : "";
                if (npcName.isEmpty()) {
                    LogUtils.sendError("[Visitors Macro] Opened wrong NPC.");
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
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
                    LogUtils.sendError("[Visitors Macro] Couldn't find the \"Accept Offer\" ItemStack!");
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
                            int amount = (quantity != null) ? Integer.parseInt(quantity) : 1;
                            itemsToBuy.add(Pair.of(itemName, amount));
                            LogUtils.sendWarning("[Visitors Macro] Required item: " + itemName + " Amount: " + amount);
                        }
                    }
                    if (foundRewards) {
                        System.out.println(StringUtils.stripControlCodes(line));
                        if (StringUtils.stripControlCodes(line).trim().isEmpty()) {
                            foundRewards = false;
                            continue;
                        }
                        if (line.contains("+")) {
                            String[] split = StringUtils.stripControlCodes(line).trim().split(" ");
                            String amount = split[0].trim();
                            String item = String.join(" ", Arrays.copyOfRange(split, 1, split.length)).trim();
                            currentRewards.add(new Tuple<>(item, amount));
                        } else {
                            int amount = 1;
                            String item = StringUtils.stripControlCodes(line).trim();
                            currentRewards.add(new Tuple<>(item, String.valueOf(amount)));
                        }
                    }
                }

                LogUtils.sendDebug("Current Rewards:");
                for (Tuple<String, String> currentReward : currentRewards) {
                    LogUtils.sendDebug(currentReward.getFirst() + " " + currentReward.getSecond());
                }

                if (itemsToBuy.isEmpty()) {
                    LogUtils.sendDebug("[Visitors Macro] Something went wrong with collecting required items...");
                    stop();
                    start();
                    return;
                }
                LogUtils.sendDebug("[Visitors Macro] Items to buy: " + itemsToBuy);

                Optional<Tuple<String, String>> profitableReward = currentRewards.stream().filter(item -> {
                    String name = StringUtils.stripControlCodes(item.getFirst());
                    return profitRewards.stream().anyMatch(reward -> reward.toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(reward.toLowerCase()));
                }).findFirst();

                profitNpc = profitableReward.isPresent();
                if (profitNpc && FarmHelperConfig.sendVisitorsMacroLogs) {
                    String reward = StringUtils.stripControlCodes(profitableReward.get().getFirst());
                    LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro found profitable item: " + reward, FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs);
                }

                switch (npcRarity) {
                    case UNKNOWN:
                        LogUtils.sendDebug("[Visitors Macro] The visitor is unknown rarity. Accepting offer...");
                        break;
                    case UNCOMMON:
                        if (FarmHelperConfig.visitorsActionUncommon == 0) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Accepting...");
                        } else if (FarmHelperConfig.visitorsActionUncommon == 1) {
                            checkIfCurrentVisitorIsProfitable();
                        } else if (FarmHelperConfig.visitorsActionUncommon == 3) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Ignoring...");
                            ignoredNPCs.add(currentVisitor.get());
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
                        } else if (FarmHelperConfig.visitorsActionRare == 3) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Ignoring...");
                            ignoredNPCs.add(currentVisitor.get());
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
                        } else if (FarmHelperConfig.visitorsActionLegendary == 3) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Ignoring...");
                            ignoredNPCs.add(currentVisitor.get());
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
                        } else if (FarmHelperConfig.visitorsActionMythic == 3) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Ignoring...");
                            ignoredNPCs.add(currentVisitor.get());
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
                        } else if (FarmHelperConfig.visitorsActionSpecial == 3) {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is uncommon rarity. Ignoring...");
                            ignoredNPCs.add(currentVisitor.get());
                        } else {
                            LogUtils.sendDebug("[Visitors Macro] The visitor is special rarity. Rejecting...");
                            rejectVisitor = true;
                        }
                        break;
                }

                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());

                if (ignoredNPCs.contains(currentVisitor.get())) {
                    LogUtils.sendWarning("[Visitors Macro] Ignoring NPC: " + npcName);
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(getRandomDelay());
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
                        delayClock.schedule(FarmHelperConfig.getRandomRotationTime());
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
                break;
            case BUY_STATE:
                onBuyState();
                break;
            case ROTATE_TO_VISITOR_2:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    if (!rotation.isRotating() && currentCharacter.isPresent()) {
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(currentCharacter.get()),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).followTarget(true)
                        );
                    }
                    return;
                }
                if (rotation.isRotating()) return;
                if (PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, true) != -1) {
                    mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop(), true, true);
                }
                assert currentVisitor.isPresent();
                assert currentCharacter.isPresent();

                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    VisitorEntities result2 = getVisitorEntities(entity);
                    if (result2 == null) break;

                    if (result2.entityCharacter.equals(currentVisitor.get()) || result2.entityCharacter.equals(currentCharacter.get()) || entity.equals(result2.entityClickStand)) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    }
                }
                LogUtils.sendDebug("Position of visitor: " + currentCharacter.get().getPositionEyes(1));
                if (mc.thePlayer.getDistanceToEntity(currentCharacter.get()) > 3) {
                    Vec3 closestVec = PlayerUtils.getClosestVecAround(currentCharacter.get(), 1.25, 90, 45);
                    if (closestVec == null) {
                        LogUtils.sendError("[Visitors Macro] Couldn't find a position to get closer");
                        stop();
                        FarmHelperConfig.visitorsMacro = false;
                        return;
                    }
                    this.closestVec = Optional.of(closestVec);
                    if (FarmHelperConfig.visitorsMacroUsePathFinder) {
//                        BaritoneHandler.walkCloserToBlockPos(currentCharacter.get().getPosition(), 1);
                        FlyPathFinderExecutor.getInstance().setSprinting(false);
                        FlyPathFinderExecutor.getInstance().setDontRotate(true);
                        FlyPathFinderExecutor.getInstance().findPath(closestVec.addVector(0, 1.8, 0), false, true);
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(currentCharacter.get()),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).followTarget(true)
                        );
                    } else {
                        rotation.easeTo(
                                new RotationConfiguration(
                                        new Target(closestVec.addVector(0, 1.6, 0)),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).easeOutBack(Math.random() > 0.5)
                        );
                        setVisitorsState(VisitorsState.GET_TO_CLOSEST_VISITOR_2);
                    }
                } else {
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Target(currentCharacter.get()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).easeOutBack(Math.random() > 0.5)
                    );
                    setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                }
                break;
            case GET_TO_CLOSEST_VISITOR_2:
                LogUtils.sendDebug("[Visitors Macro] Getting the closest visitor");
                assert currentVisitor.isPresent();
                if (!rotation.isRotating())
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Target(closestVec.get().addVector(0, 1.6, 0)),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).easeOutBack(Math.random() > 0.5)
                    );
                if (getCloserToEntity()) break;
                KeyBindUtils.stopMovement();

                LogUtils.sendDebug("[Visitors Macro] Closest visitor: " + currentVisitor.get().getCustomNameTag());

                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                break;
            case OPEN_VISITOR_2:
                if (mc.currentScreen != null) {
                    setVisitorsState(VisitorsState.FINISH_VISITOR);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
//                if (BaritoneHandler.isWalkingToGoalBlock()) return;
                assert currentVisitor.isPresent();
                assert currentCharacter.isPresent();
                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    Entity entity = mc.objectMouseOver.entityHit;
                    VisitorEntities result2 = getVisitorEntities(entity);
                    if (result2 == null) break;

                    if (result2.nameArmorStand.equals(currentVisitor.get()) || result2.entityCharacter.equals(currentCharacter.get()) || entity.equals(result2.entityClickStand)) {
                        LogUtils.sendDebug("[Visitors Macro] Looking at Visitor");
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        setVisitorsState(VisitorsState.FINISH_VISITOR);
                        KeyBindUtils.leftClick();
                    } else {
                        LogUtils.sendDebug("[Visitors Macro] Looking at something else");
                        LogUtils.sendDebug("[Visitors Macro] Looking at: " + entity.getName());
                        moveSideways();
                    }
                    break;
                }
                if (rotation.isRotating()) return;
                if (moveAwayIfPlayerTooClose()) return;
                LogUtils.sendDebug("[Visitors Macro] Looking at nothing");
                LogUtils.sendDebug("[Visitors Macro] Distance: " + mc.thePlayer.getDistanceToEntity(currentCharacter.get()));
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                break;
            case FINISH_VISITOR:
                if (mc.currentScreen == null) {
                    setVisitorsState(VisitorsState.OPEN_VISITOR_2);
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                String inventoryName = InventoryUtils.getInventoryName();
                if (!(mc.currentScreen instanceof GuiChest)) {
                    LogUtils.sendError("[Visitors Macro] Not in the visitor's inventory, closing the screen...");
                    PlayerUtils.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    break;
                }
                if (inventoryName == null || inventoryName.isEmpty()) {
                    break;
                }
                if (!currentVisitor.get().getCustomNameTag().contains(inventoryName)) {
                    PlayerUtils.closeScreen();
                    setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
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
                    for (Pair<String, Integer> item : itemsToBuy) {
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
                if (FarmHelperConfig.sendVisitorsMacroLogs) {
                    assert currentVisitor.isPresent();
                    LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro accepted visitor: " + StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()), FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs, currentRewards.toArray(new Tuple[0]));
                }
                currentVisitor.ifPresent(servedCustomers::add);
                currentRewards.clear();
                profitNpc = false;
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                break;
            case END:
                currentRewards.clear();
                LogUtils.sendSuccess("[Visitors Macro] Spent §2" + ProfitCalculator.getInstance().getFormatter().format(spentMoney) + " on visitors");
                spentMoney = 0;
                if (compactorsDisabled) {
                    setMainState(MainState.COMPACTORS);
                } else {
                    setMainState(MainState.END);
                }
                setVisitorsState(VisitorsState.NONE);
                delayClock.schedule(1_800);
                break;
        }
    }

    private boolean getCloserToEntity() {
        if (rotation.isRotating()) {
            KeyBindUtils.stopMovement();
            return true;
        }
        if (closestVec.isPresent() && currentCharacter.isPresent() && mc.thePlayer.getDistance(closestVec.get().xCoord, closestVec.get().yCoord, closestVec.get().zCoord) > 0.6) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, shouldJump() ? mc.gameSettings.keyBindJump : null, GameStateHandler.getInstance().getSpeed() > 250 && (Math.abs(mc.thePlayer.motionX) > 0.1 || Math.abs(mc.thePlayer.motionZ) > 0.1) ? mc.gameSettings.keyBindSneak : null);
            stuckClock.schedule(STUCK_DELAY);
            return true;
        }
        return false;
    }

    @Nullable
    private VisitorEntities getVisitorEntities(Entity entity) {
        Entity entityCharacter;
        Entity nameArmorStand;
        Entity entityClickStand;
        if (entity instanceof EntityArmorStand) {
            if (entity.getCustomNameTag().contains("CLICK")) {
                Entity realName = PlayerUtils.getEntityCuttingOtherEntity(entity, (entity2 -> (entity2 instanceof EntityArmorStand) && !entity2.equals(entity)));
                if (realName == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the real name of the visitor");
                    stop();
                    return null;
                }
                entityClickStand = entity;
                nameArmorStand = realName;
            } else {
                nameArmorStand = entity;
                Entity realClick = PlayerUtils.getEntityCuttingOtherEntity(entity, (entity2 -> (entity2 instanceof EntityArmorStand) && entity2.getCustomNameTag().contains("CLICK")));
                if (realClick == null) {
                    LogUtils.sendError("[Visitors Macro] Couldn't find the real click of the visitor");
                    stop();
                    return null;
                }
                entityClickStand = realClick;
            }
            Entity realCharacter = PlayerUtils.getEntityCuttingOtherEntity(nameArmorStand, (entity2 -> !(entity2 instanceof EntityArmorStand)));
            if (realCharacter == null) {
                LogUtils.sendError("[Visitors Macro] Couldn't find the real character of the visitor");
                stop();
                return null;
            }
            entityCharacter = realCharacter;
        } else {
            entityCharacter = entity;
            Entity realName = PlayerUtils.getEntityCuttingOtherEntity(entity, (entity2 -> (entity2 instanceof EntityArmorStand) && !entity2.getCustomNameTag().contains("CLICK")));
            if (realName == null) {
                LogUtils.sendError("[Visitors Macro] Couldn't find the real name of the visitor");
                stop();
                return null;
            }
            nameArmorStand = realName;
            Entity realClick = PlayerUtils.getEntityCuttingOtherEntity(entity, (entity2 -> (entity2 instanceof EntityArmorStand) && entity2.getCustomNameTag().contains("CLICK")));
            if (realClick == null) {
                LogUtils.sendError("[Visitors Macro] Couldn't find the real click of the visitor");
                stop();
                return null;
            }
            entityClickStand = realClick;
        }
        return new VisitorEntities(entityCharacter, nameArmorStand, entityClickStand);
    }

    private static class VisitorEntities {
        public final Entity entityCharacter;
        public final Entity nameArmorStand;
        public final Entity entityClickStand;

        public VisitorEntities(Entity entityCharacter, Entity nameArmorStand, Entity entityClickStand) {
            this.entityCharacter = entityCharacter;
            this.nameArmorStand = nameArmorStand;
            this.entityClickStand = entityClickStand;
        }
    }

    private Entity getClosestVisitor() {
        Optional<Entity> visitorsFiltered = getVisitors()
                .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
        return visitorsFiltered.orElse(null);
    }

    private Stream<Entity> getVisitors() {
        return mc.theWorld.getLoadedEntityList().stream()
                .filter(e -> e instanceof EntityArmorStand)
                .filter(e -> e.hasCustomName() && e.getCustomNameTag().startsWith("§"))
                .filter(e -> !servedCustomers.contains(e))
                .filter(entity2 -> entity2.hasCustomName() && visitors.stream().anyMatch(v -> equalsWithoutFormatting(v, entity2.getCustomNameTag())))
                .filter(entity2 -> !ignoredNPCs.contains(entity2));
    }

    private boolean equalsWithoutFormatting(String name1, String name2) {
        return StringUtils.stripControlCodes(name1.toLowerCase()).trim().equals(StringUtils.stripControlCodes(name2.toLowerCase()).trim());
    }

    private boolean moveAwayIfPlayerTooClose() {
        try {
            assert currentCharacter.isPresent();
            if (mc.thePlayer.getDistanceToEntity(currentCharacter.get()) < 0.25) {
                if (GameStateHandler.getInstance().isBackWalkable()) {
                    KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, GameStateHandler.getInstance().getSpeed() > 250 && (mc.thePlayer.motionX > 0.1 || mc.thePlayer.motionZ > 0.1) ? mc.gameSettings.keyBindSneak : null);
                    Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
                    return true;
                }
                if (moveSideways()) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean moveSideways() {
        if (GameStateHandler.getInstance().isLeftWalkable()) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindLeft, GameStateHandler.getInstance().getSpeed() > 250 && (mc.thePlayer.motionX > 0.1 || mc.thePlayer.motionZ > 0.1) ? mc.gameSettings.keyBindSneak : null);
            Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
            return true;
        }
        if (GameStateHandler.getInstance().isRightWalkable()) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindRight, GameStateHandler.getInstance().getSpeed() > 250 && (mc.thePlayer.motionX > 0.1 || mc.thePlayer.motionZ > 0.1) ? mc.gameSettings.keyBindSneak : null);
            Multithreading.schedule(KeyBindUtils::stopMovement, 50, TimeUnit.MILLISECONDS);
            return true;
        }
        return false;
    }

    private void checkIfCurrentVisitorIsProfitable() {
        if (profitNpc) {
            LogUtils.sendDebug("[Visitors Macro] The visitor is profitable");
            LogUtils.sendDebug("[Visitors Macro] Accepting offer...");
        } else {
            LogUtils.sendWarning("[Visitors Macro] The visitor is not profitable, skipping...");
            rejectVisitor = true;
            profitNpc = false;
        }
    }

    private void rejectCurrentVisitor() {
        if (rejectVisitor()) return;
        if (FarmHelperConfig.sendVisitorsMacroLogs)
            LogUtils.webhookLog("[Visitors Macro]\\nVisitors Macro rejected visitor: " + StringUtils.stripControlCodes(currentVisitor.get().getCustomNameTag()), FarmHelperConfig.pingEveryoneOnVisitorsMacroLogs, currentRewards.toArray(new Tuple[0]));
        currentVisitor.ifPresent(servedCustomers::add);
        currentRewards.clear();
        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
        profitNpc = false;
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
                    setBuyState(BuyState.END);
                    break;
                }
                if (InventoryUtils.getAmountOfItemInInventory(itemsToBuy.get(0).getLeft()) >= itemsToBuy.get(0).getRight()) {
                    LogUtils.sendDebug("[Visitors Macro] Already have " + itemsToBuy.get(0).getLeft() + ", skipping...");
                    itemsToBuy.remove(0);
                    return;
                }
                setBuyState(BuyState.BUY_ITEMS);
                break;
            case BUY_ITEMS:
                AutoBazaar.getInstance().buy(itemsToBuy.get(0).getLeft(), itemsToBuy.get(0).getRight(), (int) FarmHelperConfig.visitorsMacroMaxSpendLimit * 1_000_000);
                setBuyState(BuyState.WAIT_FOR_AUTOBAZAAR_FINISH);
                break;
            case WAIT_FOR_AUTOBAZAAR_FINISH:
                if (AutoBazaar.getInstance().wasPriceManipulated()) {
                    LogUtils.sendDebug("[Visitors Macro] Price manipulation detected, skipping...");
                    if (profitNpc) {
                        LogUtils.sendDebug("[Visitors Macro] The visitor is profitable, adding to ignore list...");
                        ignoredNPCs.add(currentVisitor.get());
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    } else {
                        rejectVisitor = true;
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                    }
                    setBuyState(BuyState.NONE);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    PlayerUtils.closeScreen();
                    break;
                }
                if (AutoBazaar.getInstance().hasNotFoundOnBZ()) {
                    LogUtils.sendDebug("[Visitors Macro] Couldn't find " + itemsToBuy.get(0).getLeft() + " amount " + itemsToBuy.get(0).getRight() + ", skipping...");
                    if (profitNpc) {
                        LogUtils.sendDebug("[Visitors Macro] The visitor is profitable, adding to ignore list...");
                        ignoredNPCs.add(currentVisitor.get());
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
                    } else {
                        rejectVisitor = true;
                        setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                    }
                    setBuyState(BuyState.NONE);
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    PlayerUtils.closeScreen();
                    break;
                }
                if (AutoBazaar.getInstance().hasFailed()) {
                    LogUtils.sendDebug("[Visitors Macro] Couldn't buy " + itemsToBuy.get(0).getLeft() + " amount " + itemsToBuy.get(0).getRight() + ", retrying...");
                    setBuyState(BuyState.BUY_ITEMS);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                if (AutoBazaar.getInstance().hasSucceeded()) {
                    LogUtils.sendDebug("[Visitors Macro] Bought " + itemsToBuy.get(0).getLeft() + " amount " + itemsToBuy.get(0).getRight());
                    itemsToBuy.remove(0);
                    setBuyState(BuyState.NONE);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                }
                break;
            case END:
                setBuyState(BuyState.NONE);
                setVisitorsState(VisitorsState.ROTATE_TO_VISITOR_2);
                break;
        }
    }

    private long getRandomDelay() {
        return (long) (500 + Math.random() * 500);
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

    @SubscribeEvent(receiveCanceled = true)
    public void onReceiveChat(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        if (!isRunning()) return;
        if (!currentVisitor.isPresent()) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (msg.startsWith("This server is too laggy to use the Bazaar, sorry!")) {
            LogUtils.sendDebug("[Visitors Macro] Server is too laggy to use the Bazaar, skipping...");
            setBuyState(BuyState.NONE);
            setMainState(MainState.END);
            PlayerUtils.closeScreen();
            delayClock.schedule(getRandomDelay());
            return;
        }
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
        if (buyState == BuyState.WAIT_FOR_AUTOBAZAAR_FINISH) {
            if (msg.startsWith("[Bazaar] Bought ")) {
                LogUtils.sendDebug("[Visitors Macro] Bought item");
                String spentCoins = msg.split("for")[1].replace("coins!", "").trim();
                spentMoney += Float.parseFloat(spentCoins.replace(",", ""));
            }
        }
        if (buyState == BuyState.BUY_ITEMS) {
            if (msg.startsWith("You need the Cookie Buff")) {
                PlayerUtils.closeScreen();
                LogUtils.sendDebug("[Visitors Macro] Cookie buff is needed. Skipping...");
                setBuyState(BuyState.NONE);
                setVisitorsState(VisitorsState.NONE);
                setMainState(MainState.END);
            }
        }
        if (msg.startsWith("You don't have enough of the required item!")) {
            LogUtils.sendWarning("[Visitors Macro] Bought item probably got compacted! Ignoring that visitor...");
            servedCustomers.remove(currentVisitor.get());
            ignoredNPCs.add(currentVisitor.get());
            setVisitorsState(VisitorsState.ROTATE_TO_VISITOR);
            PlayerUtils.closeScreen();
            delayClock.schedule(getRandomDelay());
        }
        if (msg.startsWith("[Bazaar] You don't have enough inventory space")) {
            LogUtils.sendWarning("[Visitors Macro] You don't have enough inventory space! Stopping macro until you clear some space...");
            FarmHelperConfig.visitorsMacro = false;
            stop();
        }
    }

    public enum MainState {
        NONE,
        TRAVEL,
        AUTO_SELL,
        COMPACTORS,
        VISITORS,
        END
    }

    enum TravelState {
        NONE,
        WAIT_FOR_TP,
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
        GET_TO_CLOSEST_VISITOR,
        ROTATE_TO_VISITOR,
        OPEN_VISITOR,
        GET_LIST,
        CLOSE_VISITOR,
        BUY_STATE,
        ROTATE_TO_VISITOR_2,
        OPEN_VISITOR_2,
        GET_TO_CLOSEST_VISITOR_2,
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
        BUY_ITEMS,
        WAIT_FOR_AUTOBAZAAR_FINISH,
        END
    }
}
