package com.jelly.farmhelperv2.feature.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.failsafe.FailsafeManager;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.IFeature;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.pathfinder.FlyPathFinderExecutor;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/* Credits to OsamaBeingLagging */
public class PestFarmer implements IFeature {

    private static PestFarmer instance;

    public static PestFarmer getInstance() {
        if (instance == null) {
            instance = new PestFarmer();
        }

        return instance;
    }

    @Getter
    @Setter
    private long pestSpawnTime = 0L;
    private boolean rodSwap = false;
    private boolean enabled = false;
    private Clock clock = new Clock();
    private int swapTo = -1;
    private State state = State.SWAPPING;
    public ReturnState returnState = ReturnState.SUSPEND;
    private boolean pestSpawned = false;
    private Minecraft mc = Minecraft.getMinecraft();

    @Override
    public String getName() {
        return "PestFarmer";
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
    public void resetStatesAfterMacroDisabled() {
        state = State.SWAPPING;
        pestSpawned = false;
        swapTo = -1;
    }

    @Override
    public boolean isToggled() {
        return FarmHelperConfig.pestFarming;
    }

    @Override
    public boolean shouldCheckForFailsafes() {
        return false;
    }

    @Override
    public void start() {
        if (enabled) {
            return;
        }

        MacroHandler.getInstance().pauseMacro();
        enabled = true;
    }

    @Override
    public void stop() {
        if (!enabled) {
            return;
        }

        enabled = false;
        teleporting = false;
        rodSwap = false;
        state = State.SWAPPING;
        if (MacroHandler.getInstance().isMacroToggled()) {
            MacroHandler.getInstance().resumeMacro();
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        if (!this.isToggled() || !MacroHandler.getInstance().isCurrentMacroEnabled() || MacroHandler.getInstance().getCurrentMacro().get().currentState.ordinal() < 4 || enabled)
            return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) return;
        if (!Scheduler.getInstance().isFarming()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;


        if ((System.currentTimeMillis() - pestSpawnTime) >= (FarmHelperConfig.pestFarmingWaitTime * 1000L) && GameStateHandler.getInstance().getPestsCount() < FarmHelperConfig.startKillingPestsAt) {
            if (AutoWardrobe.activeSlot != FarmHelperConfig.pestFarmingSet1Slot) {
                swapTo = FarmHelperConfig.pestFarmingSet1Slot;
                start();
            }

            return;
        }

        if (pestSpawned && (System.currentTimeMillis() - pestSpawnTime) >= (2000) && AutoWardrobe.activeSlot != FarmHelperConfig.pestFarmingSet0Slot) {
            LogUtils.sendDebug("Swapping to " + FarmHelperConfig.pestFarmingSet0Slot);
            swapTo = FarmHelperConfig.pestFarmingSet0Slot;
            rodSwap = true;
            pestSpawned = false;
            start();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0) return;
        String message = event.message.getUnformattedText();

        if (message.startsWith("§6§lYUCK!") || message.startsWith("§6§lEWW!") || message.startsWith("§6§lGROSS!")) {
            pestSpawnTime = System.currentTimeMillis();
            pestSpawned = true;
            LogUtils.sendDebug("[PestFarmer] Pest Spawned.");
        }
    }

    @SubscribeEvent
    public void onTickSwap(ClientTickEvent event) {
        if (!enabled) return;
        if (event.phase != Phase.START) return;
        switch (state) {
            case SWAPPING:
                AutoWardrobe.getInstance().swapTo(swapTo);
                state = (rodSwap) ? State.FIND_ROD : State.ENDING;
                rodSwap = false;
                break;
            case FIND_ROD:
                if (!AutoWardrobe.getInstance().isRunning()) {
                    InventoryUtils.holdItem("Rod");
                    clock.schedule(500);
                    state = State.THROW_ROD;
                }
                break;
            case THROW_ROD:
                if (clock.passed()) {
                    KeyBindUtils.rightClick();
                    clock.schedule(500);
                    state = State.FIND_TOOL;
                }
                break;
            case FIND_TOOL:
                if (clock.passed()) {
                    PlayerUtils.getTool();
                    clock.schedule(100);
                    state = State.ENDING;
                }

                break;
            case ENDING:
                if (AutoWardrobe.getInstance().isRunning()) {
                    return;
                }

                stop();
                break;
        }
    }

    @SubscribeEvent
    public void onTickReturn(ClientTickEvent event) {
        if (returnState == ReturnState.SUSPEND) return;
        if (event.phase != Phase.START) return;
        switch (returnState) {
            case FIND_ROD:
                if (clock.passed()) {
                    if (InventoryUtils.holdItem("Rod")) {
                        clock.schedule(500);
                        returnState = ReturnState.THROW_ROD;
                    } else {
                        LogUtils.sendError("[Pest Farmer] Unable to find rod. Disabling");
                        FeatureManager.getInstance().disableAll();
                        MacroHandler.getInstance().disableMacro();
                    }
                }
                break;
            case THROW_ROD:
                if (clock.passed()) {
                    KeyBindUtils.rightClick();
                    clock.schedule(100);
                    returnState = ReturnState.TP_TO_PLOT;
                }
                break;
            case TP_TO_PLOT:
                if (clock.passed()) {
                    if (PlotUtils.getPlotNumberBasedOnLocation().number != lastPlot) {
                        teleporting = true;
                        mc.thePlayer.sendChatMessage("/tptoplot " + lastPlot);
                    }

                    clock.schedule(PlotUtils.getPlotNumberBasedOnLocation().number != lastPlot ? 500 : 100);
                    returnState = ReturnState.FLY_TO_POSITION;
                }
                break;
            case FLY_TO_POSITION:
                if (clock.passed()) {
                    teleporting = false;
                    FlyPathFinderExecutor.getInstance().setUseAOTV(FarmHelperConfig.useAoteVInPestsDestroyer && InventoryUtils.hasItemInHotbar("Aspect of the"));
                    FlyPathFinderExecutor.getInstance().findPath(lastPosition, false, true);
                    clock.schedule(100);
                    returnState = ReturnState.SNEAK;
                }
                break;
            case SNEAK:
                if (!FlyPathFinderExecutor.getInstance().isRunning()) {
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, true);
                    clock.schedule(100);
                    returnState = ReturnState.ROTATE;
                }

                break;
            case ROTATE:
                if (clock.passed() && mc.thePlayer.onGround) {
                    KeyBindUtils.stopMovement(true);
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindSneak, false);
                    Rotation newRotation = new Rotation(lastYaw, lastPitch);
                    Rotation curretnRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                    Rotation neededChange = RotationHandler.getInstance().getNeededChange(newRotation, curretnRotation);
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                            newRotation,
                            FarmHelperConfig.getRandomRotationTime() * ((Math.abs(neededChange.getYaw()) > 90) ? 2 : 1),
                            null
                    ).easeOutBack(true));
                    returnState = ReturnState.HOLD_ITEM;
                }
                break;
            case HOLD_ITEM:
                if (!RotationHandler.getInstance().isRotating()) {
                    PlayerUtils.getTool();
                    clock.schedule(100);
                    returnState = ReturnState.FINISH;
                }

                break;
            case FINISH:
                if (clock.passed()) {
                    MacroHandler.getInstance().getCurrentMacro().get().setCurrentState(direction);
                    MacroHandler.getInstance().resumeMacro();
                    returnState = ReturnState.SUSPEND;
                }
                break;
        }
    }

    @Getter
    private boolean teleporting = false;
    public int lastPlot = -1;
    public Vec3 lastPosition = new Vec3(0, 0, 0);
    public float lastYaw = -1;
    public float lastPitch = -1;
    public AbstractMacro.State direction = AbstractMacro.State.NONE;

    public void returnBack() {
        if (!MacroHandler.getInstance().isMacroToggled()) return;
        clock.schedule(500);
        returnState = ReturnState.FIND_ROD;
        MacroHandler.getInstance().pauseMacro();
    }

    public void setReturnVariables() {
        lastPlot = PlotUtils.getPlotNumberBasedOnLocation().number;
        lastPosition = BlockUtils.getBlockPosCenter(new BlockPos(mc.thePlayer.getPositionVector()).add(0, 1, 0));
        direction = MacroHandler.getInstance().getCurrentMacro().get().currentState;
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
    }

    // bleh, its only for the tracker basically
    enum State {
        SWAPPING,
        FIND_ROD,
        THROW_ROD,
        FIND_TOOL,
        ENDING
    }

    enum ReturnState {
        FIND_ROD,
        THROW_ROD,
        TP_TO_PLOT,
        FLY_TO_POSITION,
        ROTATE,
        SNEAK,
        HOLD_ITEM,
        FINISH,
        SUSPEND
    }

}
