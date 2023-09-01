package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.features.FailsafeNew;
import com.jelly.farmhelper.utils.*;

import static com.jelly.farmhelper.utils.BlockUtils.*;

public class MushroomMacroNew extends Macro<MushroomMacroNew.State> {
    enum State {
        LEFT,
        RIGHT,
        DROPPING,
        NONE
    }

    public static float closest90Yaw = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        if (FarmHelper.config.customPitch) {
            pitch = FarmHelper.config.customPitchLevel;
        } else {
            pitch = (float) (Math.random() * 2 - 1); // -1 - 1
        }
        Config.CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugFullLog("Crop: " + crop);
        MacroHandler.crop = crop;
        yaw = AngleUtils.getClosestDiagonal();
        closest90Yaw = AngleUtils.getClosest();
        if (currentState == null)
            currentState = calculateDirection();
        if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal()) {
            rotation.easeTo(yaw, pitch, 500);
        } else {
            rotation.easeTo(closest90Yaw + (currentState == State.LEFT ? -30 : 30), pitch, 400);
        }
        mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(Config.CropEnum.MUSHROOM);
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
            rotatedAfterStart = true;
        }

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating) {
            yaw = AngleUtils.getClosestDiagonal();
            closest90Yaw = AngleUtils.getClosest();
            pitch = (float) (Math.random() * 2 - 1); // -1 - 1
            if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal())
                rotation.easeTo(yaw, pitch, (long) (600 + Math.random() * 200));
            else if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                if (currentState == State.RIGHT) {
                    rotation.easeTo(closest90Yaw + 30, pitch, 400);
                } else if (currentState == State.LEFT) {
                    rotation.easeTo(closest90Yaw - 30, pitch, 400);
                }
            }
        }

        // Don't do anything if macro is after teleportation for few seconds
        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (currentState != State.NONE && currentState != State.DROPPING && !FailsafeNew.emergency && !rotation.rotating && FarmHelper.config.oldRotationCheck) {
            System.out.println("AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) = " + AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw));
            System.out.println("Math.abs(mc.thePlayer.rotationPitch - pitch) = " + Math.abs(mc.thePlayer.rotationPitch - pitch));
            System.out.println("FarmHelper.config.rotationCheckSensitivity = " + FarmHelper.config.rotationCheckSensitivity);
            if ((FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal()) && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FarmHelper.config.rotationCheckSensitivity || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity)) {
                rotation.reset();
                FailsafeNew.emergencyFailsafe(FailsafeNew.FailsafeType.ROTATION);
                return;
            } else if ((FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), closest90Yaw + (currentState == State.LEFT ? -30 : 30)) > FarmHelper.config.rotationCheckSensitivity || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity)) {
                rotation.reset();
                FailsafeNew.emergencyFailsafe(FailsafeNew.FailsafeType.ROTATION);
                return;
            }
        }

        if (isStuck()) return;

        CropUtils.getTool();

        // Waiting for teleportation, don't move
        if (beforeTeleportationPos != null) {
            LogUtils.debugFullLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            return;
        }

        if (FailsafeNew.emergency && FailsafeNew.findHighestPriorityElement() != FailsafeNew.FailsafeType.DESYNC) {
            LogUtils.debugFullLog("Blocking changing movement due to emergency");
            return;
        }

        // Update or invoke state, based on if player is moving or not
        if (FarmHelper.gameState.canChangeDirection()) {
            KeyBindUtils.stopMovement(FarmHelper.config.holdLeftClickWhenChangingRow);
            FarmHelper.gameState.scheduleNotMoving();
            updateState();
            invokeState();
        } else {
            if (!mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.posY) > 0.75) {
                changeState(State.DROPPING);
                FarmHelper.gameState.scheduleNotMoving();
                System.out.println("Dropping");
            }
            invokeState();
        }
    }

    private void updateState() {
        if (currentState == null)
            changeState(State.NONE);
        switch (currentState) {
            case LEFT:
                if (FarmHelper.gameState.rightWalkable) {
                    currentState = changeState(State.RIGHT);
                } else if (!FarmHelper.gameState.leftWalkable) {
                    currentState = changeState(State.LEFT);
                } else {
                    LogUtils.debugLog("No direction found");
                }

                if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                    pitch = (float) (Math.random() * 2 - 1); // -1 - 1
                    rotation.easeTo(closest90Yaw + 30, pitch, 400);
                }
                break;
            case RIGHT:
                if (FarmHelper.gameState.leftWalkable) {
                    currentState = changeState(State.LEFT);
                } else if (!FarmHelper.gameState.rightWalkable) {
                    currentState = changeState(State.RIGHT);
                } else {
                    LogUtils.debugLog("No direction found");
                }
                if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                    pitch = (float) (Math.random() * 2 - 1); // -1 - 1
                    rotation.easeTo(closest90Yaw - 30, pitch, 400);
                }
                break;
            case DROPPING: {
                LogUtils.debugFullLog("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.getPosition().getY()) > 1.5) {
                    if (FarmHelper.config.rotateAfterDrop && !rotation.rotating) {
                        LogUtils.debugLog("Rotating 180");
                        rotation.reset();
                        yaw = yaw + 180;
                        closest90Yaw = AngleUtils.getClosest(closest90Yaw + 180);
                        if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal()) {
                            rotation.easeTo(yaw, pitch, 500);
                        } else {
                            rotation.easeTo(closest90Yaw + (currentState == State.LEFT ? -30 : 30), pitch, 500);
                        }
                        KeyBindUtils.stopMovement();
                    }
                    changeState(State.NONE);
                    layerY = mc.thePlayer.getPosition().getY();
                } else  {
                    FarmHelper.gameState.scheduleNotMoving();
                }
                break;
            }
            case NONE:
                changeState(calculateDirection());
                break;
        }
    }

    private void invokeState() {
        if (currentState == null) return;
        switch (currentState) {
            case RIGHT: {
                if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                    KeyBindUtils.holdThese(
                            mc.gameSettings.keyBindForward,
                            mc.gameSettings.keyBindAttack
                    );
                } else if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal()) {
                    KeyBindUtils.holdThese(
                            mushroom45DegreeLeftSide() ? mc.gameSettings.keyBindRight : mc.gameSettings.keyBindForward,
                            mc.gameSettings.keyBindAttack
                    );
                }
                break;
            }
            case LEFT: {
                if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM_ROTATE.ordinal()) {
                    KeyBindUtils.holdThese(
                            mc.gameSettings.keyBindForward,
                            mc.gameSettings.keyBindAttack
                    );
                } else if (FarmHelper.config.SShapeMacroType == Config.SMacroEnum.MUSHROOM.ordinal()) {
                    KeyBindUtils.holdThese(
                            mushroom45DegreeLeftSide() ? mc.gameSettings.keyBindForward : mc.gameSettings.keyBindLeft,
                            mc.gameSettings.keyBindAttack
                    );
                }
                break;
            }
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.debugLog("Dropping done, but didn't drop high enough to rotate!");
                    layerY = mc.thePlayer.getPosition().getY();
                    changeState(State.NONE);
                }
                break;
            case NONE: {
                LogUtils.debugLog("No direction found");
                break;
            }
        }
    }

    private boolean mushroom45DegreeLeftSide() {
        float targetAngle = 45f; // Angle around 45 degrees
        float counterClockwiseThreshold = -315f; // Angle around -315 degrees
        float tolerance = 2f; // Tolerance of 2 degrees

        float angleDifference = AngleUtils.normalizeAngle(closest90Yaw - yaw);

        return Math.abs(angleDifference - targetAngle) < tolerance ||
                Math.abs(angleDifference - counterClockwiseThreshold) < tolerance;
    }


    @Override
    public State calculateDirection() {
        boolean f1 = true, f2 = true;

        if (rightCropIsReady()) {
            return State.RIGHT;
        } else if (leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, 0, 0, closest90Yaw)) && f1) {
                return State.RIGHT;
            }
            if (!isWalkable(getRelativeBlock(i, 1, 0, closest90Yaw)))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, 0, 0, closest90Yaw)) && f2) {
                return State.LEFT;
            }
            if (!isWalkable(getRelativeBlock(-i, 1, 0, closest90Yaw)))
                f2 = false;
        }
        System.out.println("No direction found");
        return State.NONE;
    }
}
