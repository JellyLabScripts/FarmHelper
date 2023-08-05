package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.BlockUtils.isWalkable;

public class SShapeCropMacroNew extends Macro<SShapeCropMacroNew.State> {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum State {
        LEFT,
        RIGHT,
        SWITCHING_LANE,
        DROPPING,
        NONE
    }

    private enum ChangeLaneDirection {
        FORWARD,
        BACKWARD
    }

    public ChangeLaneDirection changeLaneDirection = null;
    public State prevState;
    private float yaw;
    private float pitch;

    @Override
    public void onEnable() {
        super.onEnable();
        currentState = State.NONE;
        rotated = false;
        prevState = State.NONE;
        Config.CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;
        CropUtils.getTool();
        if (FarmHelper.config.customPitch) {
            pitch = FarmHelper.config.customPitchLevel;
        } else {
            if (crop == Config.CropEnum.NETHER_WART || crop == Config.CropEnum.CACTUS) {
                pitch = (float) (0f + Math.random() * 0.5f);
            } else if (crop == Config.CropEnum.MELON || crop == Config.CropEnum.PUMPKIN) {
                pitch = 28 + (float) (Math.random() * 2); //28-30
            } else {
                pitch = (float) (2.8f + Math.random() * 0.5f);
            }
        }
        yaw = AngleUtils.getClosest();
        rotation.easeTo(yaw, pitch, 500);
    }

    @Override
    public void triggerTpCooldown() {
        super.triggerTpCooldown();
        if (currentState == State.DROPPING) {
            currentState = calculateDirection();
        }
    }

    @Override
    public void onTick() {
        super.onTick();

        if (isTping) {
            return;
        }

        if (rotation.rotating) {
            KeyBindUtils.stopMovement();
            FarmHelper.gameState.scheduleNotMoving();
            return;
        } else {
            if (!rotated) {
                prevState = changeState(calculateDirection());
                rotated = true;
            }
        }

        // Check for rotation after teleporting back to spawn point
        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && !rotated) {
            yaw = AngleUtils.getClosest(yaw);
            if (FarmHelper.config.rotateAfterWarped)
                yaw = AngleUtils.get360RotationYaw(yaw + 180);
            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                rotation.easeTo(yaw, pitch, (long) (500 + Math.random() * 200));
                rotated = true;
            }
        }

        // Don't do anything if macro is after teleportation for few seconds
        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        // Calculate direction after teleportation
        if (lastTp.isScheduled() && lastTp.passed()) {
            lastTp.reset();
            currentState = calculateDirection();
            rotated = false;
        }

        LogUtils.debugFullLog("Current state: " + currentState);

        // Check for rotation check failsafe
        boolean flag = AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FarmHelper.config.rotationCheckSensitivity
                || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity;

        if(!Failsafe.emergency && flag && lastTp.passed() && !rotation.rotating) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }

        if (Antistuck.stuck) {
            if (!Antistuck.unstuckThreadIsRunning) {
                Antistuck.unstuckThreadIsRunning = true;
                LogUtils.debugLog("Stuck!");
                new Thread(Antistuck.unstuckThread).start();
            } else {
                LogUtils.debugLog("Unstuck thread is alive!");
            }
            return;
        }

        CropUtils.getTool();

        // Waiting for teleportation, don't move
        if (beforeTeleportationPos != null) {
            LogUtils.debugLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            return;
        }

        if (Failsafe.emergency) {
            LogUtils.debugLog("Blocking changing movement due to emergency");
            return;
        }

        // Update or invoke state, based on if player is moving or not
        if (FarmHelper.gameState.canChangeDirection()) {
            KeyBindUtils.stopMovement(FarmHelper.config.holdLeftClickWhenChangingRow);
            FarmHelper.gameState.scheduleNotMoving();
            updateState();
            invokeState();
        } else {
            if (!mc.thePlayer.onGround) {
                prevState = changeState(State.DROPPING);
                FarmHelper.gameState.scheduleNotMoving();
                System.out.println("Dropping");
            }
            invokeState();
        }
    }

    private void updateState() {
        switch (currentState) {
            case LEFT:
            case RIGHT: {
                if (FarmHelper.gameState.frontWalkable) {
                    prevState = changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                } else if (FarmHelper.gameState.backWalkable) {
                    prevState = changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.BACKWARD;
                } else {
                    LogUtils.debugLog("Can't go forward or backward!");
                    if (FarmHelper.gameState.leftWalkable) {
                        prevState = changeState(State.LEFT);
                    } else if (FarmHelper.gameState.rightWalkable) {
                        prevState = changeState(State.RIGHT);
                    } else {
                        prevState = changeState(State.NONE);
                    }
                }
                break;
            }
            case SWITCHING_LANE: {
                if (FarmHelper.gameState.leftWalkable) {
                    prevState = changeState(State.LEFT);
                } else if (FarmHelper.gameState.rightWalkable) {
                    prevState = changeState(State.RIGHT);
                }
                break;
            }
            case DROPPING: {
                LogUtils.debugLog("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround) {
                    if (FarmHelper.config.rotateAfterDrop) {
                        LogUtils.debugLog("Rotating 180");
                        rotation.reset();
                        yaw = AngleUtils.get360RotationYaw(yaw + 180);
                        rotation.easeTo(yaw, pitch, (long) (300 + Math.random() * 500));
                        rotated = false;
                        KeyBindUtils.stopMovement();
                        return;
                    }
                    if (FarmHelper.gameState.rightWalkable) {
                        prevState = changeState(State.RIGHT);
                    } else if (FarmHelper.gameState.leftWalkable) {
                        prevState = changeState(State.LEFT);
                    }
                } else {
                    FarmHelper.gameState.scheduleNotMoving();
                }
                break;
            }
            case NONE: {
                prevState = changeState(calculateDirection());
                break;
            }
        }
    }

    private void invokeState() {
        switch (currentState) {
            case LEFT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack,
                        MacroHandler.crop == Config.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? mc.gameSettings.keyBindBack : null,
                        MacroHandler.crop != Config.CropEnum.CACTUS && MacroHandler.crop != Config.CropEnum.PUMPKIN && MacroHandler.crop != Config.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case RIGHT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack,
                        MacroHandler.crop == Config.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? mc.gameSettings.keyBindBack : null,
                        MacroHandler.crop != Config.CropEnum.CACTUS && MacroHandler.crop != Config.CropEnum.PUMPKIN && MacroHandler.crop != Config.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case SWITCHING_LANE:
                switch (changeLaneDirection) {
                    case FORWARD:
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, FarmHelper.config.holdLeftClickWhenChangingRow ? mc.gameSettings.keyBindAttack : null);
                        break;
                    case BACKWARD:
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, FarmHelper.config.holdLeftClickWhenChangingRow ? mc.gameSettings.keyBindAttack : null);
                        break;
                    default: {
                        LogUtils.scriptLog("I can't decide which direction to go!");
                        currentState = State.NONE;
                    }
                }
                break;
            case DROPPING:
//                KeyBindUtils.stopMovement();
                break;
            case NONE:
                break;
        }
    }

    private State calculateDirection() {

        if (rightCropIsReady()) {
            return State.RIGHT;
        } else if (leftCropIsReady()) {
            return State.LEFT;
        }


        for (int i = 1; i < 180; i++) {
            if (!isWalkable(BlockUtils.getRelativeBlock(i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(i - 1, -1, 1)) || isWalkable(BlockUtils.getRelativeBlock(i - 1, -1, 0))) {
                    return State.RIGHT;
                } else {
                    LogUtils.debugFullLog("Failed right: " + BlockUtils.getRelativeBlock(i - 1, 0, 1));
                    return State.LEFT;
                }
            } else if (!isWalkable(BlockUtils.getRelativeBlock(-i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-i + 1, 0, 1)) || isWalkable(BlockUtils.getRelativeBlock(-i + 1, -1, 0))) {
                    return State.LEFT;
                } else {
                    LogUtils.debugFullLog("Failed left: " + isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1)));
                    return State.RIGHT;
                }
            }
        }
        LogUtils.debugLog("Cannot find direction. Length > 180");
        return State.NONE;
    }
}
