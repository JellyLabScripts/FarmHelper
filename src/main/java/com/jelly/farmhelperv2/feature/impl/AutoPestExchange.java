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
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class AutoPestExchange implements IFeature {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AutoPestExchange instance;

    public static AutoPestExchange getInstance() {
        if (instance == null) {
            instance = new AutoPestExchange();
        }
        return instance;
    }

    private boolean enabled = false;
    @Setter
    public boolean manuallyStarted = false;
    @Getter
    private State state = State.NONE;
    @Getter
    private final Clock stuckClock = new Clock();
    @Getter
    private final Clock delayClock = new Clock();
    private BlockPos positionBeforeTp;
    private final BlockPos initialDeskPos = new BlockPos(-24, 71, -7);
    private Entity phillip = null;

    private BlockPos deskPos() {
        return new BlockPos(FarmHelperConfig.pestExchangeDeskX, FarmHelperConfig.pestExchangeDeskY, FarmHelperConfig.pestExchangeDeskZ);
    }

    private boolean isDeskPosSet() {
        return FarmHelperConfig.pestExchangeDeskX != 0 && FarmHelperConfig.pestExchangeDeskY != 0 && FarmHelperConfig.pestExchangeDeskZ != 0;
    }

    private static final RotationHandler rotation = RotationHandler.getInstance();

    @Override
    public String getName() {
        return "Auto Pest Exchange";
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
        if (enabled) return;
        if (!canEnableMacro(manuallyStarted)) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().pauseMacro();
            MacroHandler.getInstance().getCurrentMacro().ifPresent(am -> am.setSavedState(Optional.empty()));
            KeyBindUtils.stopMovement();
        }
        resetStatesAfterMacroDisabled();
        enabled = true;
        state = State.NONE;
        LogUtils.sendWarning("[Auto Pest Exchange] Starting...");
    }

    @Override
    public void stop() {
        enabled = false;
        LogUtils.sendWarning("[Auto Pest Exchange] Stopping...");
        KeyBindUtils.stopMovement();
        resetStatesAfterMacroDisabled();
        FlyPathFinderExecutor.getInstance().stop();
        BaritoneHandler.stopPathing();
    }

    @Override
    public void resetStatesAfterMacroDisabled() {
        manuallyStarted = false;
        state = State.NONE;
        stuckClock.reset();
        delayClock.reset();
        positionBeforeTp = null;
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.autoPestExchange;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return state == State.NONE || state == State.GO_BACK || state == State.EMPTY_VACUUM || state == State.GO_TO_PHILLIP;
    }

    enum State {
        NONE,
        TELEPORT_TO_DESK,
        WAIT_FOR_TP,
        FIND_PHILLIP,
        GO_TO_PHILLIP,
        WAIT_UNTIL_REACHED_DESK,
        CLICK_PHILLIP,
        WAIT_FOR_GUI,
        EMPTY_VACUUM,
        WAIT_FOR_VACUUM,
        GO_BACK,
    }

    public boolean canEnableMacro(boolean manual) {
        if (!isToggled()) return false;
        if (isRunning()) return false;
        if (!GameStateHandler.getInstance().inGarden()) return false;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (!MacroHandler.getInstance().isMacroToggled() && !manual) return false;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this, VisitorsMacro.getInstance())) return false;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) {
            LogUtils.sendError("[Auto Pest Exchange] Server is closing in " + GameStateHandler.getInstance().getServerClosingSeconds().get() + " seconds!");
            return false;
        }
        if (!manual && FarmHelperConfig.pauseAutoPestExchangeDuringJacobsContest && GameStateHandler.getInstance().inJacobContest()) {
            LogUtils.sendDebug("[Auto Pest Exchange] Jacob's contest is active, skipping...");
            return false;
        }
        if (GameStateHandler.getInstance().getPestHunterBonus() != GameStateHandler.BuffState.NOT_ACTIVE) {
            LogUtils.sendWarning("[Auto Pest Exchange] Pest Hunter bonus is active or unknown, skipping...");
            return false;
        }
        if (!manual && GameStateHandler.getInstance().inJacobContest() && !FarmHelperConfig.autoPestExchangeIgnoreJacobsContest) {
            for (String line : TablistUtils.getTabList()) {
                Matcher matcher = GameStateHandler.getInstance().jacobsRemainingTimePattern.matcher(line);
                if (matcher.find()) {
                    String minutes = matcher.group(1);
                    if (Integer.parseInt(minutes) > FarmHelperConfig.autoPestExchangeTriggerBeforeContestStarts) {
                        LogUtils.sendDebug("[Auto Pest Exchange] Jacob's contest is starting in " + minutes + " minutes, skipping...");
                        return false;
                    }
                }
            }
        }
        if (!manual && GameStateHandler.getInstance().getPestsFromVacuum() < FarmHelperConfig.autoPestExchangeMinPests) {
            LogUtils.sendDebug("[Auto Pest Exchange] There are not enough pests to start the macro!");
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public void onTickExecution(TickEvent.ClientTickEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (!enabled) return;

        if (stuckClock.isScheduled() && stuckClock.passed()) {
            LogUtils.sendError("[Auto Pest Exchange] The player is stuck!");
            state = State.GO_BACK;
        }

        if (delayClock.isScheduled() && !delayClock.passed()) return;

        switch (state) {
            case NONE:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
                if (FarmHelperConfig.logAutoPestExchangeEvents)
                    LogUtils.webhookLog("[Auto Pest Exchange] Starting the macro!\\n" + GameStateHandler.getInstance().getPestsFromVacuum() + " pests in total!");
                state = State.TELEPORT_TO_DESK;
                delayClock.schedule((long) (1_000 + Math.random() * 500));
                break;
            case WAIT_FOR_VACUUM:
                break;
            case TELEPORT_TO_DESK:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
                if (PlayerUtils.isInBarn()) {
                    state = State.FIND_PHILLIP;
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    stuckClock.schedule(30_000L);
                    break;
                }
                positionBeforeTp = mc.thePlayer.getPosition();
                state = State.WAIT_FOR_TP;
                mc.thePlayer.sendChatMessage("/tptoplot barn");
                delayClock.schedule((long) (1_000 + Math.random() * 500));
                stuckClock.schedule(10_000L);
                break;
            case WAIT_FOR_TP:
                if (mc.thePlayer.getPosition().equals(positionBeforeTp)) {
                    LogUtils.sendDebug("[Auto Pest Exchange] Waiting for teleportation...");
                    break;
                }
                if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                    LogUtils.sendDebug("[Auto Pest Exchange] The player is not on the ground, waiting...");
                    stuckClock.schedule(5_000L);
                    break;
                }
                if (PlayerUtils.isPlayerSuffocating()) {
                    stuckClock.schedule(5_000L);
                    break;
                }
                KeyBindUtils.stopMovement();
                if (positionBeforeTp.distanceSq(mc.thePlayer.getPosition()) > 7) {
                    if (FarmHelperConfig.pestExchangeDeskX == 0 && FarmHelperConfig.pestExchangeDeskY == 0 && FarmHelperConfig.pestExchangeDeskZ == 0) {
                        LogUtils.sendWarning("[Auto Pest Exchange] The desk position is not set! Trying to find Phillip...");
                        state = State.FIND_PHILLIP;
                        FlyPathFinderExecutor.getInstance().setDontRotate(true);
                        FlyPathFinderExecutor.getInstance().findPath(new Vec3(initialDeskPos).addVector(0.5f, 0.5f, 0.5f), false, true);
                    } else {
                        state = State.GO_TO_PHILLIP;
                    }
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    stuckClock.schedule(30_000L);
                    break;
                }
            case FIND_PHILLIP:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
//                if (BaritoneHandler.hasFailed() && initialDeskPos.distanceSq(mc.thePlayer.getPosition()) > 7) {
//                    LogUtils.sendError("[Auto Pest Exchange] Baritone failed to reach the destination!");
//                    state = State.GO_BACK;
//                    break;
//                }
//                if (!BaritoneHandler.isWalkingToGoalBlock()) {
//                    BaritoneHandler.walkToBlockPos(initialDeskPos);
//                    delayClock.schedule(250L);
//                    break;
//                }
                phillip = getPhillip();
                if (phillip == null)
                    break;
                if (BlockUtils.getHorizontalDistance(mc.thePlayer.getPositionVector(), phillip.getPositionVector()) < 7) {
//                    BaritoneHandler.stopPathing();
                    FlyPathFinderExecutor.getInstance().stop();
                    state = State.CLICK_PHILLIP;
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    stuckClock.schedule(30_000L);
                    break;
                }
                LogUtils.sendSuccess("[Auto Pest Exchange] Found Phillip! " + phillip.getPosition().toString());
                FlyPathFinderExecutor.getInstance().stop();
//                BaritoneHandler.stopPathing();
                state = State.GO_TO_PHILLIP;
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                stuckClock.schedule(30_000L);
                break;
            case GO_TO_PHILLIP:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
//                if (BaritoneHandler.hasFailed() && deskPos().distanceSq(mc.thePlayer.getPosition()) > 7) {
//                    LogUtils.sendError("[Auto Pest Exchange] Baritone failed to reach the destination!");
//                    state = State.GO_BACK;
//                    break;
//                }
//                if (BaritoneHandler.isWalkingToGoalBlock())
//                    break;
                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    break;
                }
                if (Math.sqrt(mc.thePlayer.getDistanceSqToCenter(deskPos())) < 3.5) {
//                    BaritoneHandler.stopPathing();
                    FlyPathFinderExecutor.getInstance().stop();
                    RotationHandler.getInstance().reset();
                    state = State.CLICK_PHILLIP;
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    stuckClock.schedule(30_000L);
                    break;
                }
                if (isDeskPosSet()) {
                    LogUtils.sendDebug("[Auto Pest Exchange] Walking to the desk position...");
//                    BaritoneHandler.walkToBlockPos(deskPos());
                    FlyPathFinderExecutor.getInstance().setDontRotate(true);
                    FlyPathFinderExecutor.getInstance().findPath(new Vec3(deskPos()).addVector(0.5f, 0.5f, 0.5f), false, true);
                    RotationHandler.getInstance().easeTo(
                            new RotationConfiguration(
                                    new Target(deskPos().up()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).followTarget(true)
                    );
                    state = State.WAIT_UNTIL_REACHED_DESK;
                } else if (phillip != null) {
                    LogUtils.sendDebug("[Auto Pest Exchange] Phillip is found, but the desk position is not set! Walking to Phillip...");
//                    BaritoneHandler.walkCloserToBlockPos(phillip.getPosition(), 2);
                    Vec3 closestVec = PlayerUtils.getClosestVecAround(phillip, 2.5);
                    if (closestVec == null) {
                        LogUtils.sendError("[Auto Pest Exchange] Can't find a valid position around Phillip!");
                        state = State.GO_BACK;
                        break;
                    }
                    FlyPathFinderExecutor.getInstance().setDontRotate(true);
                    FlyPathFinderExecutor.getInstance().findPath(closestVec, false, true);
                    state = State.WAIT_UNTIL_REACHED_DESK;
                    FarmHelperConfig.pestExchangeDeskX = (int) closestVec.xCoord;
                    FarmHelperConfig.pestExchangeDeskY = (int) (closestVec.yCoord - 1.4f);
                    FarmHelperConfig.pestExchangeDeskZ = (int) closestVec.zCoord;
                } else {
                    LogUtils.sendError("[Auto Pest Exchange] Can't find Phillip!");
                    state = State.GO_BACK;
                    FarmHelperConfig.autoPestExchange = false;
                    break;
                }
                stuckClock.schedule(30_000L);
                delayClock.schedule(1000L);
                break;
            case WAIT_UNTIL_REACHED_DESK:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
//                if (BaritoneHandler.hasFailed()) {
////                    LogUtils.sendError("[Auto Pest Exchange] Baritone failed to reach the destination!");
////                    FlyPathFinderExecutor.getInstance().setDontRotate(true);
////                    FlyPathFinderExecutor.getInstance().findPath(new Vec3(deskPos()).addVector(0.5, 0.5, 0.5), false, true);
////                    break;
////                }
////                if (BaritoneHandler.isWalkingToGoalBlock())
////                    break;
                phillip = getPhillip();
                if (FlyPathFinderExecutor.getInstance().isRunning()) {
                    if (phillip == null) {
                        break;
                    }
                    Entity rotateTo;
                    if (phillip instanceof EntityArmorStand) {
                        rotateTo = PlayerUtils.getEntityCuttingOtherEntity(phillip, e -> !(e instanceof EntityArmorStand));
                    } else {
                        rotateTo = phillip;
                    }
                    if (rotateTo == null) {
                        break;
                    }

                    if (rotation.isRotating()) break;
                    rotation.easeTo(
                            new RotationConfiguration(
                                    new Target(rotateTo),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            ).easeOutBack(true)
                    );
                    break;
                }
                KeyBindUtils.stopMovement();
                LogUtils.sendDebug("[Auto Pest Exchange] Desk position reached!");
//                BaritoneHandler.stopPathing();
                FlyPathFinderExecutor.getInstance().stop();
                state = State.CLICK_PHILLIP;
                stuckClock.schedule(30_000L);
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                break;
            case CLICK_PHILLIP:
                if (BlockUtils.getHorizontalDistance(mc.thePlayer.getPositionVector(), phillip.getPositionVector()) > 7) {
                    LogUtils.sendDebug("[Auto Pest Exchange] Can't click Phillip! Walking to the desk position...");
                    state = State.GO_TO_PHILLIP;
                    delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                    stuckClock.schedule(15_000L);
                    break;
                }
                phillip = getPhillip();
                if (phillip == null) {
                    break;
                }
                if (rotation.isRotating())
                    break;
                MovingObjectPosition mop = mc.objectMouseOver;
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    Entity entity = mop.entityHit;
                    Entity armorStand = PlayerUtils.getEntityCuttingOtherEntity(entity, e -> e instanceof EntityArmorStand);
                    if (entity.getCustomNameTag().contains("Phillip") || armorStand != null && armorStand.getCustomNameTag().contains("Phillip")) {
                        KeyBindUtils.leftClick();
                        state = State.WAIT_FOR_GUI;
                        RotationHandler.getInstance().reset();
                        stuckClock.schedule(10_000L);
                        break;
                    }
                }
                rotation.easeTo(
                        new RotationConfiguration(
                                new Target(phillip),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ).easeOutBack(true)
                );
                delayClock.schedule(FarmHelperConfig.getRandomRotationTime() + 500L);
                break;
            case WAIT_FOR_GUI:
                String invName = InventoryUtils.getInventoryName();
                if (invName != null && !invName.contains("Pesthunter")) {
                    PlayerUtils.closeScreen();
                    state = State.CLICK_PHILLIP;
                } else {
                    state = State.EMPTY_VACUUM;
                }
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                break;
            case EMPTY_VACUUM:
                Slot vacuumSlot = InventoryUtils.getSlotOfItemInContainer("Empty Vacuum Bag");
                if (vacuumSlot == null) {
                    break;
                }
                ItemStack itemLore = vacuumSlot.getStack();
                List<String> lore = InventoryUtils.getItemLore(itemLore);
                if (lore.stream().anyMatch(l -> l.contains("Click to empty"))) { // not necessary but yeah :shrugger:
                    if (FarmHelperConfig.logAutoPestExchangeEvents)
                        LogUtils.webhookLog("[Auto Pest Exchange] Emptied the vacuum!\\n" + GameStateHandler.getInstance().getPestsFromVacuum() + " pests in total!");
                    state = State.WAIT_FOR_VACUUM;
                } else if (lore.stream().anyMatch(l -> l.contains("You've exchanged enough Pests"))) {
                    if (FarmHelperConfig.logAutoPestExchangeEvents)
                        LogUtils.webhookLog("[Auto Pest Exchange] Already emptied the vacuum!");
                    state = State.GO_BACK;
                } else {
                    if (FarmHelperConfig.logAutoPestExchangeEvents)
                        LogUtils.webhookLog("[Auto Pest Exchange] Failed to empty your vacuum!");
                    LogUtils.sendError("[Auto Pest Exchange] Failed to empty your vacuum!");
                    state = State.GO_BACK;
                }
                InventoryUtils.clickContainerSlot(vacuumSlot.slotNumber, InventoryUtils.ClickType.LEFT, InventoryUtils.ClickMode.PICKUP);
                stuckClock.schedule(10_000L);
                delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
                break;
            case GO_BACK:
                if (mc.currentScreen != null) {
                    PlayerUtils.closeScreen();
                    delayClock.schedule(1_000 + Math.random() * 500);
                    break;
                }
                if (!manuallyStarted) {
                    stop();
                    MacroHandler.getInstance().triggerWarpGarden(true, true, false);
                    delayClock.schedule(1_000 + Math.random() * 500);
                }
                break;
        }
    }

    @Nullable
    private Entity getPhillip() {
        return mc.theWorld.getLoadedEntityList().
                stream().
                filter(entity ->
                        entity.hasCustomName() && entity.getCustomNameTag().contains(StringUtils.stripControlCodes("Phillip")))
                .min(Comparator.comparingDouble(entity -> entity.getDistanceSqToCenter(mc.thePlayer.getPosition()))).orElse(null);
    }

    private final String[] dialogueMessages = {
            "Howdy, ",
            "Sorry for the intrusion, but I got a report of a Pest outbreak around here?",
            "Err...vermin? Swine? Buggers? Y'know...Pests!",
            "They seem to pop up when you're breaking crops on your Garden!",
            "Okay, feller. Take this SkyMart Vacuum!",
            "There's a Pest out there somewhere, I'm certain of it.",
            "When you find one, simply aim the vacuum, hold down the button, and you can suck them buggers up real quick!",
            "When you've done that, come back and tell me all about how much fun you had!"
    };

    @SubscribeEvent(receiveCanceled = true)
    public void onChatMessageReceived(ClientChatReceivedEvent event) {
        if (enabled && event.type == 0 && event.message != null && state == State.WAIT_FOR_VACUUM) {
            if (event.message.getFormattedText().contains("§e[NPC] §6Phillip§f: Thanks for the §6Pests§f,"))
                LogUtils.sendSuccess("[Auto Pest Exchange] Successfully emptied the vacuum!");
            else {
                if (event.message.getUnformattedText().startsWith("You've exchanged enough Pests recently! Try emptying your Vacuum Bag later!")) {
                    LogUtils.sendDebug("[Auto Pest Exchange] Already emptied the vacuum.");
                }
                for (String message : dialogueMessages) {
                    if (event.message.getFormattedText().contains("§e[NPC] §6Phillip§f: " + message)) {
                        LogUtils.sendError("[Auto Pest Exchange] You haven't unlocked Phillip yet!");
                        break;
                    }
                }
            }
            state = State.GO_BACK;
            delayClock.schedule((long) (FarmHelperConfig.pestAdditionalGUIDelay + 300 + Math.random() * 300));
        }
    }


    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (FarmHelperConfig.streamerMode) return;
        if (!FarmHelperConfig.highlightPestExchangeDeskLocation) return;
        if (FarmHelperConfig.pestExchangeDeskX == 0
                && FarmHelperConfig.pestExchangeDeskY == 0
                && FarmHelperConfig.pestExchangeDeskZ == 0) return;
        RenderUtils.drawBlockBox(deskPos().add(0, 0, 0), new Color(0, 155, 255, 50));
    }
}
