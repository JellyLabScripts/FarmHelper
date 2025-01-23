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
import com.jelly.farmhelperv2.util.helper.Target;
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
    private boolean cooldownOver = false;
    private long lastPestSpawn = 0L;
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
        returnState = ReturnState.SUSPEND;
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
        if (!this.isToggled() || !MacroHandler.getInstance().isCurrentMacroEnabled() || MacroHandler.getInstance().isCurrentMacroPaused() || enabled || returnState != ReturnState.SUSPEND)
            return;
        if (!GameStateHandler.getInstance().inGarden()) return;
        if (GameStateHandler.getInstance().getServerClosingSeconds().isPresent()) return;
        if (!Scheduler.getInstance().isFarming()) return;
        if (FailsafeManager.getInstance().triggeredFailsafe.isPresent()) return;
        if (FeatureManager.getInstance().isAnyOtherFeatureEnabled(this)) return;


        if (cooldownOver && AutoWardrobe.activeSlot != FarmHelperConfig.pestFarmingSet1Slot) {
            swapTo = FarmHelperConfig.pestFarmingSet1Slot;
            rodSwap = false;
            start();
        } else if (pestSpawned && !cooldownOver && (System.currentTimeMillis() - lastPestSpawn) >= (2000) && AutoWardrobe.activeSlot != FarmHelperConfig.pestFarmingSet0Slot) {
            LogUtils.sendDebug("Swapping to " + FarmHelperConfig.pestFarmingSet0Slot);
            swapTo = FarmHelperConfig.pestFarmingSet0Slot;
            rodSwap = true;
            pestSpawned = false;
            start();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (GameStateHandler.getInstance().getLocation() != GameStateHandler.Location.GARDEN) return;
        String message = event.message.getUnformattedText();

        if (message.startsWith("§6§lYUCK!") || message.startsWith("§6§lEWW!") || message.startsWith("§6§lGROSS!")) {
            lastPestSpawn = System.currentTimeMillis();
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
                    if (InventoryUtils.holdItem("Rod")) {
                        clock.schedule(500);
                        state = State.THROW_ROD;
                    } else {
                        LogUtils.sendError("[Pest Farmer] Unable to find rod. Disabling");
                        returnState = ReturnState.SUSPEND;
                        MacroHandler.getInstance().disableMacro();
                        return;
                    }
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
                        returnState = ReturnState.SUSPEND;
                        MacroHandler.getInstance().disableMacro();
                        return;
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
                    FlyPathFinderExecutor.getInstance().setSprinting(false);
                    FlyPathFinderExecutor.getInstance().setUseAOTV(FarmHelperConfig.useAoteVInPestsDestroyer && InventoryUtils.hasItemInHotbar("Aspect of the"));
                    FlyPathFinderExecutor.getInstance().findPath(lastPosition.addVector(0, 4, 0), true, true);
                    clock.schedule(100);
                    returnState = ReturnState.EQUIP_AOTV;
                }
                break;
            case EQUIP_AOTV:
                if (!FlyPathFinderExecutor.getInstance().isRunning() && mc.thePlayer.motionX + mc.thePlayer.motionZ == 0) {
                    if (InventoryUtils.holdItem("Aspect of the")) {
                        clock.schedule(500);
                        returnState = ReturnState.LOOK_AT_BLOCK;
                    } else {
                        LogUtils.sendError("[Pest Farmer] Unable to find AOTV/AOTE. Disabling");
                        returnState = ReturnState.SUSPEND;
                        MacroHandler.getInstance().disableMacro();
                        return;
                    }
                }
                break;
            case LOOK_AT_BLOCK:
                if (!FlyPathFinderExecutor.getInstance().isRunning() && mc.thePlayer.motionX < 0.15 && mc.thePlayer.motionZ < 0.15) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(
                        new Target(lastPosition),
                        FarmHelperConfig.getRandomRotationTime(),
                        null
                    ));

                    clock.schedule(100);
                    returnState = ReturnState.RIGHT_CLICK;
                }
                break;
            case RIGHT_CLICK:
                if (!RotationHandler.getInstance().isRotating() && clock.passed()) {
                    teleporting = true;
                    KeyBindUtils.rightClick();
                    clock.schedule(500);
                    returnState = ReturnState.SNEAK;
                }
                break;
            case SNEAK:
                if (clock.passed()) {
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
        if (!InventoryUtils.hasItemInHotbar("Aspect of the")) {
            LogUtils.sendError("No AOTV/AOTE found in hotbar.. Disabling.");
            MacroHandler.getInstance().disableMacro();
        }

        clock.schedule(500);
        returnState = ReturnState.FIND_ROD;
        MacroHandler.getInstance().pauseMacro();
    }

    public void setReturnVariables() {
        if (!BlockUtils.canFlyHigher(5)) {
            LogUtils.sendError("Obstructed place.. Disabling PestFarmer");
            PestsDestroyer.getInstance().stop();
            FarmHelperConfig.pestFarming = false;
            MacroHandler.getInstance().resumeMacro();
            return;
        }

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

    public enum ReturnState {
        FIND_ROD,
        THROW_ROD,
        TP_TO_PLOT,
        FLY_TO_POSITION,
        EQUIP_AOTV,
        LOOK_AT_BLOCK,
        RIGHT_CLICK,
        ROTATE,
        SNEAK,
        HOLD_ITEM,
        FINISH,
        SUSPEND
    }

}
