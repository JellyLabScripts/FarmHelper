package com.jelly.farmhelperv2.macro;

import cc.polyfrost.oneconfig.utils.Multithreading;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.event.ReceivePacketEvent;
import com.jelly.farmhelperv2.feature.FeatureManager;
import com.jelly.farmhelperv2.feature.impl.*;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.handler.RotationHandler;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@Getter
public abstract class AbstractMacro {
    public static final Minecraft mc = Minecraft.getMinecraft();
    private final RotationHandler rotation = RotationHandler.getInstance();
    private final Clock rewarpDelay = new Clock();
    @Getter
    private final Clock analyticsClock = new Clock();
    @Setter
    public State currentState = State.NONE;
    @Setter
    private boolean enabled = false;
    @Setter
    private Optional<SavedState> savedState = Optional.empty();
    @Setter
    private int layerY = 0;
    @Setter
    private float yaw;
    @Setter
    private float pitch;
    @Setter
    @Getter
    private Optional<Float> closest90Deg = Optional.empty();
    @Setter
    private boolean rotated = false;
    @Setter
    private Optional<BlockPos> beforeTeleportationPos = Optional.empty();

    @Getter
    @Setter
    private RewarpState rewarpState = RewarpState.NONE;

    public boolean isEnabled() {
        return enabled && !FeatureManager.getInstance().shouldPauseMacroExecution();
    }

    public boolean isPaused() {
        return !enabled;
    }

    private final Clock checkOnSpawnClock = new Clock();

    public void onTick() {
        if (Failsafe.getInstance().isRunning() || Failsafe.getInstance().getChooseEmergencyDelay().isScheduled()) {
            LogUtils.sendWarning("Failsafe is running! Blocking main onTick event!");
            return;
        }
        if (mc.thePlayer.capabilities.isFlying) {
            KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
            return;
        }
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
        if (PlayerUtils.isStandingOnRewarpLocation() && !Failsafe.getInstance().isEmergency() && GameStateHandler.getInstance().notMoving()) {
            if (checkOnSpawnClock.passed()) {
                if (PestsDestroyer.getInstance().canEnableMacro()) {
                    PestsDestroyer.getInstance().start();
                } else if (VisitorsMacro.getInstance().canEnableMacro(false, true)) {
                    VisitorsMacro.getInstance().start();
                }
                checkOnSpawnClock.schedule(5000);
            } else {
                triggerWarpGarden();
            }
            return;
        }
        if (PlayerUtils.isStandingOnSpawnPoint() && !Failsafe.getInstance().isEmergency() && GameStateHandler.getInstance().notMoving() && checkOnSpawnClock.passed()) {
            if (PestsDestroyer.getInstance().canEnableMacro()) {
                PestsDestroyer.getInstance().start();
                rotated = true;
                return;
            }
            if (VisitorsMacro.getInstance().canEnableMacro(false, true)) {
                VisitorsMacro.getInstance().start();
                rotated = true;
                return;
            }
            checkOnSpawnClock.schedule(5000);
        }
        if (mc.thePlayer.getPosition().getY() < -5) {
            LogUtils.sendError("Build a wall between the rewarp point and the void to prevent falling out of the garden! Disabling the macro...");
            MacroHandler.getInstance().disableMacro();
            triggerWarpGarden(true);
            return;
        }

        if (getRotation().isRotating()) {
            if (!mc.gameSettings.keyBindSneak.isKeyDown())
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
                rewarpState = RewarpState.POST_REWARP;
                Multithreading.schedule(() -> rewarpState = RewarpState.NONE, 1000, TimeUnit.MILLISECONDS);
                rewarpDelay.schedule(FarmHelperConfig.getRandomTimeBetweenChangingRows());
                return;
            } else if (rotated && (LagDetector.getInstance().isLagging() || !mc.thePlayer.onGround)) {
                LogUtils.sendDebug("Rotated after warping, but player is not on ground or lagging.");
                KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                return;
            } else if (rotated) {
                LogUtils.sendDebug("Rotated after warping, but player is not on ground or lagging.");
                return;
            }

            if (FarmHelperConfig.rotateAfterWarped) {
                doAfterRewarpRotation();
            }

            if (shouldFixRotation() && shouldRotateAfterWarp()) {
                rotation.easeTo(new RotationConfiguration(
                        new Rotation(yaw, pitch), FarmHelperConfig.getRandomRotationTime() * 2, null
                ));
                LogUtils.sendDebug("Rotating");
            }
            rotated = true;
            setLayerY(mc.thePlayer.getPosition().getY());
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

        FarmHelperConfig.CropEnum crop = PlayerUtils.getCropBasedOnMouseOver();
        if (crop != FarmHelperConfig.CropEnum.NONE && crop != MacroHandler.getInstance().getCrop()) {
            LogUtils.sendWarning("Crop changed from " + MacroHandler.getInstance().getCrop() + " to " + crop);
            MacroHandler.getInstance().setCrop(crop);
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
        checkOnSpawnClock.reset();
        FarmHelperConfig.CropEnum crop = PlayerUtils.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        GameStateHandler.getInstance().scheduleRewarp();
        if (FarmHelperConfig.customPitch) {
            setPitch(FarmHelperConfig.customPitchLevel);
        }
        if (FarmHelperConfig.customYaw) {
            setYaw(FarmHelperConfig.customYawLevel);
        }
        if (savedState.isPresent()) {
            LogUtils.sendDebug("Restoring state: " + savedState.get());
            changeState(savedState.get().getState());
            setYaw(savedState.get().getYaw());
            setPitch(savedState.get().getPitch());
            setClosest90Deg(Optional.ofNullable(savedState.get().getClosest90Deg()));
            savedState = Optional.empty();
        } else if (currentState == State.NONE || currentState == null) {
            changeState(calculateDirection());
            setClosest90Deg(Optional.of(AngleUtils.getClosest()));
        }
        setEnabled(true);
        setLayerY(mc.thePlayer.getPosition().getY());
        analyticsClock.schedule(60_000);
        if (getCurrentState() == null)
            changeState(State.NONE);
    }

    public void onDisable() {
        DesyncChecker.getInstance().getClickedBlocks().clear();
        KeyBindUtils.stopMovement();
        changeState(State.NONE);
        setRewarpState(RewarpState.NONE);
        setClosest90Deg(Optional.empty());
        rewarpDelay.reset();
        setEnabled(false);
    }

    public void saveState() {
        if (!savedState.isPresent()) {
            LogUtils.sendDebug("Saving state: " + currentState);
            savedState = Optional.of(new SavedState(currentState, yaw, pitch, closest90Deg.orElse(AngleUtils.getClosest())));
        }
    }

    public void clearSavedState() {
        savedState = Optional.empty();
    }

    public void changeState(State state) {
        LogUtils.sendDebug("Changing state from " + currentState + " to " + state);
        setCurrentState(state);
    }

    private void checkForTeleport() {
        if (!beforeTeleportationPos.isPresent()) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos.get()) > 2) {
            LogUtils.sendDebug("Teleported!");
            changeState(State.NONE);
            checkOnSpawnClock.reset();
            beforeTeleportationPos = Optional.empty();
            rewarpState = RewarpState.TELEPORTED;
            rotated = false;
            GameStateHandler.getInstance().scheduleNotMoving(750);
            if (!mc.thePlayer.onGround && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                KeyBindUtils.holdThese(mc.gameSettings.keyBindSneak);
                Multithreading.schedule(() -> KeyBindUtils.stopMovement(), (long) (450 + Math.random() * 600), TimeUnit.MILLISECONDS);
            }
            rewarpDelay.schedule(FarmHelperConfig.getRandomTimeBetweenChangingRows());
            if (!PlayerUtils.isSpawnLocationSet() || (mc.thePlayer.getPositionVector().distanceTo(PlayerUtils.getSpawnLocation()) > 1)) {
                PlayerUtils.setSpawnLocation();
            }
            actionAfterTeleport();
        }
    }


    public boolean shouldFixRotation() {
        double absYaw = Math.abs(Math.abs(AngleUtils.get360RotationYaw()) - Math.abs(yaw));
        double absPitch = Math.abs(Math.abs(mc.thePlayer.rotationPitch) - Math.abs(pitch));
        if (absYaw > 1 || absPitch > 1) {
            LogUtils.sendDebug("Fixing rotation after warping.");
            return true;
        }
        return false;
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
        setClosest90Deg(Optional.of(AngleUtils.getClosest(yaw)));
    }

    public boolean shouldRotateAfterWarp() {
        return true;
    }

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
        TELEPORTED,
        POST_REWARP
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
