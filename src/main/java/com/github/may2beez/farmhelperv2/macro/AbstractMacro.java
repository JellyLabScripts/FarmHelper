package com.github.may2beez.farmhelperv2.macro;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.event.ReceivePacketEvent;
import com.github.may2beez.farmhelperv2.feature.FeatureManager;
import com.github.may2beez.farmhelperv2.feature.impl.Failsafe;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.Optional;

@SuppressWarnings("unused")
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
    private Optional<State> savedState = Optional.empty();
    private final RotationUtils rotation = new RotationUtils();
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

    public boolean isEnabled() {
        return enabled && !FeatureManager.getInstance().shouldPauseMacroExecution();
    }

    public void onTick() {
        checkForTeleport();
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
            LogUtils.sendError("Build a wall between rewarp point and the void to prevent falling out of the garden! Disabling the macro...");
            MacroHandler.getInstance().disableMacro();
            triggerWarpGarden(true);
            return;
        }

        if (getRotation().rotating) {
            KeyBindUtils.stopMovement();
            GameStateHandler.getInstance().scheduleNotMoving();
            return;
        }

        if (rewarpDelay.isScheduled() && !rewarpDelay.passed()) {
            return;
        }

        if (rewarpState == RewarpState.TELEPORTED) {
            // rotate
            if (rotated) {
                LogUtils.sendDebug("Rotated");
                rewarpState = RewarpState.NONE;
                return;
            }

            if (FarmHelperConfig.rotateAfterWarped) {
                yaw = AngleUtils.get360RotationYaw(yaw + 180);
                setClosest90Deg(AngleUtils.getClosest(yaw));
            }

            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                if (mc.thePlayer.rotationYaw == yaw) {
                    if (FarmHelperConfig.dontFixAfterWarping) {
                        LogUtils.sendDebug("Not rotating after warping");
                        rewarpState = RewarpState.NONE;
                        return;
                    }
                }
                rotation.easeTo(yaw, pitch, FarmHelperConfig.getRandomRotationTime() * 2);
            }
            rotated = true;
            LogUtils.sendDebug("Rotating");
            rewarpDelay.schedule(FarmHelperConfig.getRandomTimeBetweenChangingRows());
            return;
        } else if (rewarpState == RewarpState.TELEPORTING) {
            // teleporting
            return;
        }

        if (needAntistuck(additionalCheck())) {
            return;
        }

//        if (Failsafe.getInstance().isEmergency() && FailsafeNew.findHighestPriorityElement() != FailsafeNew.FailsafeType.DESYNC) {
//            LogUtils.sendDebug("Blocking changing movement due to emergency");
//            return false;
//        }

        if (Failsafe.getInstance().isEmergency()) {
            LogUtils.sendDebug("Blocking changing movement due to emergency");
            return;
        }

//        if (LagDetection.isLagging()) return false;

        PlayerUtils.getTool();

        // Update or invoke state, based on if player is moving or not
        if (GameStateHandler.getInstance().canChangeDirection()) {
            KeyBindUtils.stopMovement(FarmHelperConfig.holdLeftClickWhenChangingRow);
            GameStateHandler.getInstance().scheduleNotMoving();
            updateState();
            invokeState();
        } else {
            if (!mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.posY) > 0.75 && mc.thePlayer.posY < 80) {
                changeState(State.DROPPING);
                GameStateHandler.getInstance().scheduleNotMoving();
            }
            invokeState();
        }

    }

    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    public void onChatMessageReceived(String msg) {
    }

    public void onOverlayRender(RenderGameOverlayEvent event) {
    }

    public void onPacketReceived(ReceivePacketEvent event) {
    }

    public abstract void updateState();
    public abstract void invokeState();

    public void onEnable() {
        setClosest90Deg(AngleUtils.getClosest());
        if (savedState.isPresent()) {
            LogUtils.sendDebug("Restoring state: " + savedState.get());
            changeState(savedState.get());
            savedState = Optional.empty();
        } else {
            changeState(calculateDirection());
        }
        setEnabled(true);
    }

    public void onDisable() {
        KeyBindUtils.stopMovement();
        changeState(State.NONE);
        setClosest90Deg(-1337);
        rewarpDelay.reset();
        setEnabled(false);
    }

    public void saveState() {
        if (!savedState.isPresent()) {
            LogUtils.sendDebug("Saving state: " + currentState);
            savedState = Optional.ofNullable(currentState);
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
//            Antistuck.stuck = false;
//            Antistuck.notMovingTimer.schedule();
            if (!PlayerUtils.isSpawnLocationSet() || (mc.thePlayer.getPositionVector().distanceTo(PlayerUtils.getSpawnLocation()) > 1)) {
                PlayerUtils.setSpawnLocation();
            }
//            if (VisitorsMacro.canEnableMacro(false)) {
//                VisitorsMacro.enableMacro(false);
//            }
        }
    }

    public void triggerWarpGarden() {
        triggerWarpGarden(false);
    }

    public void triggerWarpGarden(boolean force) {
        if (GameStateHandler.getInstance().notMoving()) {
            KeyBindUtils.stopMovement();
        }
        if (force || GameStateHandler.getInstance().canChangeDirection() && !beforeTeleportationPos.isPresent()) {
            rewarpState = RewarpState.TELEPORTING;
            LogUtils.sendDebug("Warping to spawn point");
            mc.thePlayer.sendChatMessage("/warp garden");
            setBeforeTeleportationPos(Optional.ofNullable(mc.thePlayer.getPosition()));
        }
    }

    public boolean additionalCheck() {
        return false;
    }

    public State calculateDirection() {
        return State.NONE;
    }

    public boolean needAntistuck(boolean lastMoveBack) {
//        if (LagDetection.isLagging() || LagDetection.wasJustLagging()) return false;
//        if (Antistuck.stuck && !FailsafeNew.emergency) {
//            unstuck(lastMoveBack);
//            return true;
//        }
        return false;
    }

    public void unstuck(boolean lastMoveBack) {
//        if (!Antistuck.unstuckThreadIsRunning) {
//            Antistuck.stuck = true;
//            Antistuck.unstuckLastMoveBack = lastMoveBack;
//            Antistuck.unstuckThreadIsRunning = true;
//            if (Antistuck.unstuckTries >= 2 && FarmHelper.config.rewarpAt3FailesAntistuck) {
//                LogUtils.sendWarning("Macro was continuously getting stuck! Warping to garden...");
//                triggerWarpGarden(true);
//                yaw = -2137;
//                pitch = -2137;
//                Antistuck.unstuckTries = 0;
//                return;
//            }
//            LogUtils.sendWarning("Macro is stuck! Turning on antistuck procedure...");
//            Antistuck.unstuckThreadInstance = new Thread(Antistuck.unstuckRunnable, "antistuck");
//            KeyBindUtils.stopMovement();
//            Antistuck.unstuckThreadInstance.start();
//        } else {
//            LogUtils.sendDebug("Unstuck thread is alive!");
//        }
    }
}
