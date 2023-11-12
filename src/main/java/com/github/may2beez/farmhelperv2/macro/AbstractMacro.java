package com.github.may2beez.farmhelperv2.macro;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.impl.*;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.handler.RotationHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Optional;

@SuppressWarnings("ALL")
@Getter
public abstract class AbstractMacro {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public enum State {
        // Add default values like NONE and DROPPING
        NONE,
        DROPPING,
        LEFT,
        RIGHT,
        BACKWARD,
        FORWARD,
        SWITCHING_SIDE,
        SWITCHING_LANE,

        A,
        D,
        S
    }

    public enum RewarpState {
        NONE,
        TELEPORTING,
        TELEPORTED
    }

    @Setter
    private boolean enabled = false;
    @Setter
    public State currentState = State.NONE;
    @Setter
    private Optional<SavedState> savedState = Optional.empty();
    private final RotationHandler rotation = RotationHandler.getInstance();
    @Setter
    private int layerY = 0;
    @Setter
    private float yaw;
    @Setter
    private float pitch;
    @Setter
    @Getter
    private float closest90Deg = -1337;
    @Setter
    private boolean rotated = false;
    @Setter
    private Optional<BlockPos> beforeTeleportationPos = Optional.empty();

    @Getter
    @Setter
    private RewarpState rewarpState = RewarpState.NONE;

    private final Clock rewarpDelay = new Clock();

    @Getter
    private final Clock analyticsClock = new Clock();

    public boolean isEnabled() {
        return enabled && !FeatureManager.getInstance().shouldPauseMacroExecution();
    }

    public boolean isPaused() {
        return !enabled;
    }

    public void onTick() {
        checkForTeleport();
        LogUtils.webhookStatus();
        if (analyticsClock.passed() && FarmHelperConfig.sendAnalyticData) {
            BanInfoWS.getInstance().sendAnalyticsData();
        }
        if (!PlayerUtils.isRewarpLocationSet()) {
            LogUtils.sendError("Your rewarp position is not set!");
            MacroHandler.getInstance().disableMacro();
            return;
        }
        if (PlayerUtils.isStandingOnRewarpLocation() && !Failsafe.getInstance().isEmergency()) {
            triggerWarpGarden();
            return;
        }
        if (mc.thePlayer.getPosition().getY() < -5) {
            LogUtils.sendError("Build a wall between the rewarp point and the void to prevent falling out of the garden! Disabling the macro...");
            MacroHandler.getInstance().disableMacro();
            triggerWarpGarden(true);
            return;
        }

        if (getRotation().isRotating()) {
            KeyBindUtils.stopMovement();
            GameStateHandler.getInstance().scheduleNotMoving();
            return;
        }

        if (rewarpDelay.isScheduled() && !rewarpDelay.passed()) {
            return;
        }

        if (rewarpState == RewarpState.TELEPORTED) {
            // rotate
            if (rotated && !LagDetector.getInstance().isLagging() && mc.thePlayer.onGround) {
                LogUtils.sendDebug("Rotated after warping.");
                rewarpState = RewarpState.NONE;
                return;
            } else if (rotated && (LagDetector.getInstance().isLagging() || !mc.thePlayer.onGround)) {
                return;
            }

            if (FarmHelperConfig.rotateAfterWarped) {
                doAfterRewarpRotation();
            }

            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                if (mc.thePlayer.rotationYaw == yaw) {
                    if (FarmHelperConfig.dontFixAfterWarping) {
                        LogUtils.sendDebug("Not rotating after warping.");
                        rotated = true;
                        return;
                    }
                }
                if (shouldRotateAfterWarp())
                    rotation.easeTo(new RotationConfiguration(
                            new Rotation(yaw, pitch), FarmHelperConfig.getRandomRotationTime() * 2, null
                    ));
            }
            rotated = true;
            LogUtils.sendDebug("Rotating");
            setLayerY(mc.thePlayer.getPosition().getY());
            rewarpDelay.schedule(FarmHelperConfig.getRandomTimeBetweenChangingRows());
            return;
        } else if (rewarpState == RewarpState.TELEPORTING) {
            // teleporting
            return;
        }

        if (Failsafe.getInstance().isEmergency()) {
            LogUtils.sendDebug("Blocking changing movement due to emergency!");
            return;
        }

        if (LagDetector.getInstance().isLagging()) {
            LogUtils.sendDebug("Blocking changing movement due to lag!");
            return;
        }

        PlayerUtils.getTool();

        // Update or invoke state, based on if player is moving or not
        if (GameStateHandler.getInstance().canChangeDirection()) {
            KeyBindUtils.stopMovement(FarmHelperConfig.holdLeftClickWhenChangingRow);
            GameStateHandler.getInstance().scheduleNotMoving();
            updateState();
        } else {
            if (!mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.posY) > 0.75 && mc.thePlayer.posY < 80) {
                changeState(State.DROPPING);
                GameStateHandler.getInstance().scheduleNotMoving();
            }
            invokeState();
        }
    }

    public void onLastRender() {
    }

    public void onChatMessageReceived(String msg) {
    }

    public void onOverlayRender(RenderGameOverlayEvent.Post event) {
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    public abstract void updateState();

    public abstract void invokeState();

    public void onEnable() {
        GameStateHandler.getInstance().scheduleRewarp();
        setClosest90Deg(AngleUtils.getClosest());
        if (savedState.isPresent()) {
            LogUtils.sendDebug("Restoring state: " + savedState.get());
            changeState(savedState.get().getState());
            setYaw(savedState.get().getYaw());
            setPitch(savedState.get().getPitch());
            setClosest90Deg(savedState.get().getClosest90Deg());
            savedState = Optional.empty();
        } else if (currentState == State.NONE || currentState == null) {
            changeState(calculateDirection());
        }
        setEnabled(true);
        if (VisitorsMacro.getInstance().isToggled()) {
            VisitorsMacro.getInstance().start();
        }
        setLayerY(mc.thePlayer.getPosition().getY());
        analyticsClock.schedule(60_000);
    }

    public void onDisable() {
        DesyncChecker.getInstance().getClickedBlocks().clear();
        KeyBindUtils.stopMovement();
        changeState(State.NONE);
        setClosest90Deg(-1337);
        rewarpDelay.reset();
        setEnabled(false);
    }

    public void saveState() {
        if (!savedState.isPresent()) {
            LogUtils.sendDebug("Saving state: " + currentState);
            savedState = Optional.of(new SavedState(currentState, yaw, pitch, closest90Deg));
        }
    }

    public void changeState(State state) {
        LogUtils.sendDebug("Changing state from " + currentState + " to " + state);
        setCurrentState(state);
    }

    private void checkForTeleport() {
        if (!beforeTeleportationPos.isPresent()) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos.get()) > 2) {
            LogUtils.sendDebug("Teleported!");
            if (yaw == -2137 && pitch == -2137) {
                onEnable();
            }
            changeState(State.NONE);
            beforeTeleportationPos = Optional.empty();
            rewarpState = RewarpState.TELEPORTED;
            rotated = false;
            GameStateHandler.getInstance().scheduleNotMoving(750);
            rewarpDelay.schedule(1_250);
            KeyBindUtils.holdThese(mc.thePlayer.capabilities.isFlying ? mc.gameSettings.keyBindSneak : null);
            if (!PlayerUtils.isSpawnLocationSet() || (mc.thePlayer.getPositionVector().distanceTo(PlayerUtils.getSpawnLocation()) > 1)) {
                PlayerUtils.setSpawnLocation();
            }
            actionAfterTeleport();
            if (VisitorsMacro.getInstance().isToggled())
                VisitorsMacro.getInstance().start();
        }
    }

    public abstract void actionAfterTeleport();

    public void triggerWarpGarden() {
        triggerWarpGarden(false);
    }

    public void triggerWarpGarden(boolean force) {
        if (GameStateHandler.getInstance().notMoving()) {
            KeyBindUtils.stopMovement();
        }
        if (force || GameStateHandler.getInstance().canRewarp() && !beforeTeleportationPos.isPresent()) {
            rewarpState = RewarpState.TELEPORTING;
            LogUtils.sendDebug("Warping to spawn point");
            mc.thePlayer.sendChatMessage("/warp garden");
            GameStateHandler.getInstance().scheduleRewarp();
            setBeforeTeleportationPos(Optional.ofNullable(mc.thePlayer.getPosition()));
        }
    }

    public boolean additionalCheck() {
        return false;
    }

    public State calculateDirection() {
        if (BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air) && BlockUtils.getRelativeBlock(-1, -1, 0).equals(Blocks.air)) {
            return State.RIGHT;
        }
        if (BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.air) && BlockUtils.getRelativeBlock(1, -1, 0).equals(Blocks.air)) {
            return State.LEFT;
        }
        return State.NONE;
    }

    public void doAfterRewarpRotation() {
        yaw = AngleUtils.get360RotationYaw(yaw + 180);
        setClosest90Deg(AngleUtils.getClosest(yaw));
    }

    public boolean shouldRotateAfterWarp() {
        return true;
    }

    @Getter
    private static class SavedState {
        private final State state;
        private final float yaw;
        private final float pitch;
        private final float closest90Deg;

        public SavedState(State state, float yaw, float pitch, float closest90Deg) {
            this.state = state;
            this.yaw = yaw;
            this.pitch = pitch;
            this.closest90Deg = closest90Deg;
        }

        @Override
        public String toString() {
            return "SavedState{" +
                    "state=" + state +
                    ", yaw=" + yaw +
                    ", pitch=" + pitch +
                    ", closest90Deg=" + closest90Deg +
                    '}';
        }
    }
}
