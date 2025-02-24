package com.jelly.farmhelperv2.feature.impl;

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
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
import jline.internal.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoComposter implements IFeature {
    Minecraft mc = Minecraft.getMinecraft();
    private static AutoComposter instance;

    public static AutoComposter getInstance() {
        if (instance == null) {
            instance = new AutoComposter();
        }
        return instance;
    }

    private final ArrayList<Pair<String, Integer>> itemsToBuy = new ArrayList<>();
    @Getter
    private final Clock delayClock = new Clock();
    @Getter
    private final Clock stuckClock = new Clock();
    private final int STUCK_DELAY = (int) (7_500 + FarmHelperConfig.macroGuiDelay + FarmHelperConfig.macroGuiDelayRandomness);
    private final Pattern composterResourcePattern = Pattern.compile("^(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)/(\\d{1,3})k$");
    @Getter
    private MainState mainState = MainState.NONE;
    @Getter
    private TravelState travelState = TravelState.NONE;
    @Getter
    private ComposterState composterState = ComposterState.NONE;
    @Getter
    private BuyState buyState = BuyState.NONE;
    private boolean enabled = false;
    @Getter
    @Setter
    private boolean manuallyStarted = false;
    private BlockPos positionBeforeTp = null;
    private final BlockPos initialComposterPos = new BlockPos(-11, 72, -27);
    private Entity composter = null;
    private boolean composterChecked = false;

    private BlockPos composterPos() {
        return new BlockPos(FarmHelperConfig.composterX, FarmHelperConfig.composterY, FarmHelperConfig.composterZ);
    }

    private boolean isComposterPosSet() {
        return FarmHelperConfig.composterX != 0 && FarmHelperConfig.composterY != 0 && FarmHelperConfig.composterZ != 0;
    }

    @Override
    public String getName() {
        return "Auto Composter";
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
        MacroHandler.getInstance().getCurrentMacro().ifPresent(macro -> macro.getRotation().reset());
        mainState = MainState.NONE;
        travelState = TravelState.NONE;
        composterState = ComposterState.NONE;
        buyState = BuyState.NONE;
        enabled = true;
        composterChecked = false;
        delayClock.reset();
        RotationHandler.getInstance().reset();
        stuckClock.schedule(STUCK_DELAY);
        itemsToBuy.clear();
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
        }
        LogUtils.sendWarning("[Auto Composter] Macro started");
        if (FarmHelperConfig.logAutoComposterEvents) {
            LogUtils.webhookLog("[Auto Composter]\\nAuto Composter started");
        }
        IFeature.super.start();
    }

    @Override
    public void stop() {
        enabled = false;
        manuallyStarted = false;
        LogUtils.sendWarning("[Auto Composter] Macro stopped");
        RotationHandler.getInstance().reset();
        AutoBazaar.getInstance().stop();
        PlayerUtils.closeScreen();
        KeyBindUtils.stopMovement();
        FlyPathFinderExecutor.getInstance().stop();
        BaritoneHandler.stopPathing();
        MacroHandler.getInstance().getCurrentMacro().ifPresent(cm -> cm.getCheckOnSpawnClock().schedule(5_000));
        IFeature.super.stop();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        mainState = MainState.NONE;
        travelState = TravelState.NONE;
        composterState = ComposterState.NONE;
        buyState = BuyState.NONE;
        composterChecked = false;
        itemsToBuy.clear();
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoComposter;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return travelState != TravelState.WAIT_FOR_TP && mainState != MainState.END;
    }

    public boolean canEnableMacro(boolean manual) {
        if (!isToggled()) return false;
        if (isRunning()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return false;

        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Auto Composter] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            return false;
        }

        if (GameStateHandler.getInstance().getCookieBuffState() == GameStateHandler.BuffState.NOT_ACTIVE) {
            LogUtils.sendError("[Auto Composter] Cookie buff is not active, skipping...");
            return false;
        }

        if (!manual && FarmHelperConfig.pauseAutoComposterDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendError("[Auto Composter] Jacob's contest is active, skipping...");
            return false;
        }

        if (!manual && GameStateHandler.getInstance().getCurrentPurse() < FarmHelperConfig.autoComposterMinMoney * 1_000) {
            LogUtils.sendError("[Auto Composter] The player's purse is too low, skipping...");
            return false;
        }

        if (!manual && GameStateHandler.getInstance().getComposterState() != GameStateHandler.BuffState.NOT_ACTIVE) {
            int organicMatterCount = GameStateHandler.getInstance().getOrganicMatterCount();
            int fuelCount = GameStateHandler.getInstance().getFuelCount();
            if ((organicMatterCount < FarmHelperConfig.autoComposterOrganicMatterLeft) || (fuelCount < FarmHelperConfig.autoComposterFuelLeft)) {
                return true;
            }
            LogUtils.sendWarning("[Auto Composter] Composter is active or unknown, skipping...");
            return false;

        }

        return true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isRunning()) return;
        if (event.phase == TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (delayClock.isScheduled() && !delayClock.passed()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Auto Composter] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            stop();
            return;
        }
        if (stuckClock.isScheduled() && stuckClock.passed()) {
            if (AutoBazaar.getInstance().isRunning() && !AutoBazaar.getInstance().hasFailed()) {
                stuckClock.reset();
                return;
            }
            LogUtils.sendError("[Auto Composter] The player is stuck, restarting the macro...");
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
                setMainState(MainState.COMPOSTER);
                delayClock.schedule(getRandomDelay());
                break;
            case COMPOSTER:
                if (AutoSell.getInstance().isRunning()) {
                    stuckClock.schedule(STUCK_DELAY);
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                onComposterState();
                break;
            case END:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
                if (manuallyStarted) {
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
                if (PlayerUtils.isInBarn()) {
                    travelState = TravelState.GO_TO_COMPOSTER;
                    stuckClock.schedule(30_000L);
                } else {
                    setTravelState(TravelState.TELEPORT_TO_COMPOSTER);
                }
                break;
            case TELEPORT_TO_COMPOSTER:
                positionBeforeTp = mc.thePlayer.getPosition();
                setTravelState(TravelState.WAIT_FOR_TP);
                mc.thePlayer.sendChatMessage("/tptoplot barn");
                delayClock.schedule((long) (600 + Math.random() * 500));
                break;
            case WAIT_FOR_TP:
                if (mc.thePlayer.getDistanceSqToCenter(positionBeforeTp) < 3 || PlayerUtils.isPlayerSuffocating()) {
                    LogUtils.sendDebug("[Auto Composter] Waiting for teleportation...");
                    return;
                }
                setTravelState(TravelState.GO_TO_COMPOSTER);
                delayClock.schedule((long) (600 + Math.random() * 500));
                break;
            case GO_TO_COMPOSTER:
                if (isComposterPosSet()) {
                    if (mc.thePlayer.getDistanceSqToCenter(composterPos()) < 3) {
                        setTravelState(TravelState.END);
                        break;
                    }
                    if (!FarmHelperConfig.autoComposterTravelMethod) {
                        FlyPathFinderExecutor.getInstance().setSprinting(false);
                        FlyPathFinderExecutor.getInstance().setDontRotate(false);
                        FlyPathFinderExecutor.getInstance().findPath(new Vec3(composterPos()).subtract(0.5f, 0.1f, 0.5f), true, true);
                    } else {
                        BaritoneHandler.walkToBlockPos(composterPos());
                    }
                    setTravelState(TravelState.END);
                    break;
                }

                setTravelState(TravelState.FIND_COMPOSTER);
                stuckClock.schedule(30_000);
                break;
            case FIND_COMPOSTER:
                composter = getComposter();
                if (composter == null) {
                    if (!FlyPathFinderExecutor.getInstance().isRunning() && !FarmHelperConfig.autoComposterTravelMethod) {
                        FlyPathFinderExecutor.getInstance().setSprinting(false);
                        FlyPathFinderExecutor.getInstance().setDontRotate(false);
                        FlyPathFinderExecutor.getInstance().findPath(new Vec3(initialComposterPos).addVector(0.5f, 0.5f, 0.5f), true, true);
                    }
                    if (!BaritoneHandler.isPathing() && FarmHelperConfig.autoComposterTravelMethod) {
                        BaritoneHandler.isWalkingToGoalBlock(0.5);
                    }
                    LogUtils.sendDebug("[Auto Composter] Composter not found! Looking for it.");
                    break;
                }
                Vec3 closestVec = PlayerUtils.getClosestVecAround(composter, 1.75);
                if (closestVec == null) {
                    LogUtils.sendError("[Auto Composter] Can't find a valid position around the Composter!");
                    setMainState(MainState.END);
                    break;
                }
                FlyPathFinderExecutor.getInstance().stop();
                BaritoneHandler.stopPathing();
                BlockPos closestPos = new BlockPos(closestVec);
                FarmHelperConfig.composterX = closestPos.getX();
                FarmHelperConfig.composterY = closestPos.getY();
                FarmHelperConfig.composterZ = closestPos.getZ();
                LogUtils.sendSuccess("[Auto Composter] Found Composter! " + composter.getPosition().toString());
                setTravelState(TravelState.GO_TO_COMPOSTER);
                delayClock.schedule((long) (300 + Math.random() * 300));
                stuckClock.schedule(30_000L);
                break;
            case END:
                if (FlyPathFinderExecutor.getInstance().isRunning() || BaritoneHandler.isWalkingToGoalBlock(0.5)) {
                    break;
                }
                KeyBindUtils.stopMovement();
                if (FarmHelperConfig.autoComposterAutosellBeforeFilling) {
                    setMainState(MainState.AUTO_SELL);
                } else {
                    setMainState(MainState.COMPOSTER);
                }
                setTravelState(TravelState.NONE);
                break;
        }
    }

    public void onComposterState() {
        switch (composterState) {
            case NONE:
                setComposterState(ComposterState.ROTATE_TO_COMPOSTER);
                break;
            case ROTATE_TO_COMPOSTER:
                if (mc.currentScreen != null) {
                    KeyBindUtils.stopMovement();
                    PlayerUtils.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                if (composter == null) {
                    composter = getComposter();
                }
                RotationHandler.getInstance().easeTo(
                        new RotationConfiguration(
                                new Target(composter),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        )
                );
                if (FlyPathFinderExecutor.getInstance().isRunning() || BaritoneHandler.isWalkingToGoalBlock(0.5)) {
                    break;
                }
                setComposterState(ComposterState.OPEN_COMPOSTER);
                break;
            case OPEN_COMPOSTER:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(getRandomDelay());
                    return;
                }
                MovingObjectPosition mop = mc.objectMouseOver;
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    Entity entity = mop.entityHit;
                    if (entity.equals(composter) || entity.getDistanceToEntity(composter) < 1) {
                        KeyBindUtils.leftClick();
                        if (!composterChecked) {
                            setComposterState(ComposterState.CHECK_COMPOSTER);
                        } else {
                            setComposterState(ComposterState.FILL_COMPOSTER);
                        }
                        RotationHandler.getInstance().reset();
                        stuckClock.schedule(10_000L);
                        delayClock.schedule(600L + Math.random() * 400L);
                        break;
                    }
                } else {
                    if (RotationHandler.getInstance().isRotating()) break;
                    if (mc.thePlayer.getDistanceToEntity(composter) > 3) {
                        setMainState(MainState.TRAVEL);
                        setComposterState(ComposterState.NONE);
                    } else {
                        RotationHandler.getInstance().easeTo(
                                new RotationConfiguration(
                                        new Target(composter),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        () -> {
                                            KeyBindUtils.onTick(mc.gameSettings.keyBindForward);
                                            RotationHandler.getInstance().reset();
                                        }
                                )
                        );
                    }
                    delayClock.schedule(1_000 + Math.random() * 500);
                }
                break;
            case CHECK_COMPOSTER:
                String invName = InventoryUtils.getInventoryName();
                if (invName == null) {
                    setComposterState(ComposterState.OPEN_COMPOSTER);
                    break;
                }
                if (invName.contains("Composter")) {
                    LogUtils.sendDebug("[Auto Composter] Checking Composter");
                    Slot organicMatterSlot = InventoryUtils.getSlotOfIdInContainer(37);
                    Slot fuelSlot = InventoryUtils.getSlotOfIdInContainer(43);
                    if (organicMatterSlot == null || fuelSlot == null) break;
                    ItemStack organicMatterItemStack = organicMatterSlot.getStack();
                    ItemStack fuelItemStack = fuelSlot.getStack();
                    if (organicMatterItemStack == null || fuelItemStack == null) break;
                    ArrayList<String> organicMatterLore = InventoryUtils.getItemLore(organicMatterItemStack);
                    ArrayList<String> fuelLore = InventoryUtils.getItemLore(fuelItemStack);

                    itemsToBuy.clear();

                    int boxOfSeedMatter = 25_600;
                    int voltaFuel = 10_000;

                    int currentMatter = -1;
                    int maxMatter = -1;

                    for (String line : organicMatterLore) {
                        Matcher matcher = composterResourcePattern.matcher(StringUtils.stripControlCodes(line).trim());
                        if (matcher.matches()) {
                            currentMatter = (int) Double.parseDouble(matcher.group(1).replace(",", ""));
                            maxMatter = Integer.parseInt(matcher.group(2)) * 1_000;
                        }
                    }
                    if (currentMatter == -1) break;
                    int amountBoS = (maxMatter - currentMatter) / boxOfSeedMatter;
                    if (amountBoS > 0) {
                        itemsToBuy.add(Pair.of("Box of Seeds", amountBoS));
                    }

                    int currentFuel = -1;
                    int maxFuel = -1;
                    for (String line : fuelLore) {
                        Matcher matcher = composterResourcePattern.matcher(StringUtils.stripControlCodes(line).trim());
                        if (matcher.matches()) {
                            currentFuel = (int) Double.parseDouble(matcher.group(1).replace(",", ""));
                            maxFuel = Integer.parseInt(matcher.group(2)) * 1_000;
                        }
                    }
                    if (currentFuel == -1) break;
                    int amountFuel = (maxFuel - currentFuel) / voltaFuel;
                    if (amountFuel > 0) {
                        itemsToBuy.add(Pair.of("Volta", amountFuel));
                    }
                    composterChecked = true;
                    PlayerUtils.closeScreen();
                    setComposterState(ComposterState.BUY_STATE);
                } else {
                    PlayerUtils.closeScreen();
                    setComposterState(ComposterState.ROTATE_TO_COMPOSTER);
                    delayClock.schedule(400 + Math.random() * 400);
                    stuckClock.schedule(10_000L);
                }
                break;
            case BUY_STATE:
                onBuyState();
                break;
            case FILL_COMPOSTER:
                String invName2 = InventoryUtils.getInventoryName();
                if (invName2 == null) {
                    setComposterState(ComposterState.OPEN_COMPOSTER);
                    break;
                }
                if (invName2.contains("Composter")) {
                    ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                    int boughtOrganicMatterSlot = InventoryUtils.getSlotIdOfItemInContainer("Box of Seeds");
                    int boughtFuelSlot = InventoryUtils.getSlotIdOfItemInContainer("Volta");
                    if (boughtOrganicMatterSlot != -1) {
                        InventoryUtils.clickSlotWithId(boughtOrganicMatterSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP, chest.windowId);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                        break;
                    }
                    if (boughtFuelSlot != -1) {
                        InventoryUtils.clickSlotWithId(boughtFuelSlot, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP, chest.windowId);
                        delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    }
                    LogUtils.sendWarning("Supplied composter with resources.");
                    if (FarmHelperConfig.logAutoComposterEvents) {
                        LogUtils.webhookLog("Supplied composter with resources.");
                    }
                    setComposterState(ComposterState.END);
                } else {
                    PlayerUtils.closeScreen();
                    setComposterState(ComposterState.ROTATE_TO_COMPOSTER);
                    delayClock.schedule(400 + Math.random() * 400);
                    stuckClock.schedule(10_000L);
                }
                break;
            case END:
                setMainState(MainState.END);
                setComposterState(ComposterState.NONE);
                delayClock.schedule(1_800);
                break;
        }


    }

    private void onBuyState() {
        switch (buyState) {
            case NONE:
                if (itemsToBuy.isEmpty()) {
                    setBuyState(BuyState.END);
                    break;
                }
                if (InventoryUtils.getAmountOfItemInInventory(itemsToBuy.get(0).getLeft()) >= itemsToBuy.get(0).getRight()) {
                    LogUtils.sendDebug("[Auto Composter] Already have " + itemsToBuy.get(0).getLeft() + ", skipping...");
                    itemsToBuy.remove(0);
                    break;
                }
                setBuyState(BuyState.BUY_ITEMS);
                break;
            case BUY_ITEMS:
                AutoBazaar.getInstance().buy(itemsToBuy.get(0).getLeft(), itemsToBuy.get(0).getRight(), FarmHelperConfig.autoComposterMaxSpendLimit * 1_000_000);
                setBuyState(BuyState.WAIT_FOR_AUTOBAZAAR_FINISH);
                break;
            case WAIT_FOR_AUTOBAZAAR_FINISH:
                if (AutoBazaar.getInstance().wasPriceManipulated()) {
                    LogUtils.sendDebug("[Auto Composter] Price manipulation detected, stopping...");
                    stop();
                    break;
                }
                if (AutoBazaar.getInstance().hasFailed()) {
                    LogUtils.sendDebug("[Auto Composter] Couldn't buy " + itemsToBuy.get(0).getLeft() + " amount " + itemsToBuy.get(0).getRight() + ", retrying...");
                    setBuyState(BuyState.BUY_ITEMS);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                    break;
                }
                if (AutoBazaar.getInstance().hasSucceeded()) {
                    LogUtils.sendDebug("[Auto Composter] Bought " + itemsToBuy.get(0).getLeft() + " amount " + itemsToBuy.get(0).getRight());
                    itemsToBuy.remove(0);
                    setBuyState(BuyState.NONE);
                    PlayerUtils.closeScreen();
                    delayClock.schedule(FarmHelperConfig.getRandomGUIMacroDelay());
                }
                break;
            case END:
                setBuyState(BuyState.NONE);
                setComposterState(ComposterState.OPEN_COMPOSTER);
                break;
        }
    }

    @Nullable
    private Entity getComposter() {
        return mc.theWorld.getLoadedEntityList().
                stream()
                .filter(entity -> {
                    if (!(entity instanceof EntityArmorStand)) return false;
                    return entity.getCustomNameTag().contains("COMPOSTER");
                })
                .min(Comparator.comparingDouble(entity -> entity.getDistanceSqToCenter(mc.thePlayer.getPosition()))).orElse(null);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelperConfig.streamerMode) return;
        if (!FarmHelperConfig.highlightComposterLocation) return;
        if (!isComposterPosSet()) return;
        RenderUtils.drawBlockBox(composterPos(), new Color(0, 155, 255, 50));
    }

    private long getRandomDelay() {
        return (long) (500 + Math.random() * 500);
    }

    enum MainState {
        NONE,
        TRAVEL,
        AUTO_SELL,
        COMPOSTER,
        END
    }

    private void setMainState(AutoComposter.MainState state) {
        mainState = state;
        LogUtils.sendDebug("[Auto Composter] Main state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    enum TravelState {
        NONE,
        TELEPORT_TO_COMPOSTER,
        WAIT_FOR_TP,
        GO_TO_COMPOSTER,
        FIND_COMPOSTER,
        END
    }

    private void setTravelState(AutoComposter.TravelState state) {
        travelState = state;
        LogUtils.sendDebug("[Auto Composter] Travel state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    enum ComposterState {
        NONE,
        ROTATE_TO_COMPOSTER,
        OPEN_COMPOSTER,
        CHECK_COMPOSTER,
        BUY_STATE,
        FILL_COMPOSTER,
        END
    }

    private void setComposterState(AutoComposter.ComposterState state) {
        composterState = state;
        LogUtils.sendDebug("[Auto Composter] Composter state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

    enum BuyState {
        NONE,
        BUY_ITEMS,
        WAIT_FOR_AUTOBAZAAR_FINISH,
        END
    }

    private void setBuyState(AutoComposter.BuyState state) {
        buyState = state;
        LogUtils.sendDebug("[Auto Composter] Buy state: " + state.name());
        stuckClock.schedule(STUCK_DELAY);
    }

}
