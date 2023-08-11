package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;

import static com.jelly.farmhelper.macros.VerticalCropMacroNew.State.DROPPING;
import static com.jelly.farmhelper.utils.BlockUtils.*;

public class VerticalCropMacroNew extends Macro<VerticalCropMacroNew.State> {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public enum State {
        LEFT,
        RIGHT,
        DROPPING,
        NONE
    }

    public State prevState;

    @Override
    public void onEnable() {
        super.onEnable();
        changeState(State.NONE);
        Config.CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;
        CropUtils.getTool();
        if (FarmHelper.config.customPitch) {
            pitch = FarmHelper.config.customPitchLevel;
        } else {
            if (crop == Config.CropEnum.SUGAR_CANE) {
                pitch = (float) (Math.random() * 2); // 0 - 2
            } else if (crop == Config.CropEnum.COCOA_BEANS) {
                pitch = -90;
            } else {
                pitch = 2.8f + (float) (Math.random() * 0.6); // 2.8-3.4
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
            rotatedAfterStart = true;
        }

        // Check for rotation after teleporting back to spawn point
        checkForRotationAfterTp();

        // Don't do anything if macro is after teleportation for few seconds
        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        LogUtils.debugFullLog("Current state: " + currentState);

        checkForRotationFailsafe();

        if (isStuck()) return;

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
            if (!mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.posY) > 0.75) {
                changeState(State.DROPPING);
                FarmHelper.gameState.scheduleNotMoving();
            }
            invokeState();
        }
    }

    private void updateState() {
        switch (currentState) {
            case LEFT:
            case RIGHT: {
                    LogUtils.debugLog("Can't go forward or backward!");
                    if (FarmHelper.gameState.leftWalkable) {
                        changeState(State.LEFT);
                    } else if (FarmHelper.gameState.rightWalkable) {
                        changeState(State.RIGHT);
                    } else {
                        changeState(State.NONE);
                    }
                }
                break;
            case DROPPING: {
                LogUtils.debugLog("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.getPosition().getY()) > 1.5) {
                    if (FarmHelper.config.rotateAfterDrop && !rotation.rotating) {
                        LogUtils.debugLog("Rotating 180");
                        rotation.reset();
                        yaw = yaw + 180;
                        rotation.easeTo(yaw, pitch, (long) (400 + Math.random() * 300));
                    }
                    KeyBindUtils.stopMovement();
                    layerY = mc.thePlayer.getPosition().getY();
                    changeState(State.NONE);
                } else {
                    FarmHelper.gameState.scheduleNotMoving();
                }
                break;
            }
            case NONE: {
                changeState(calculateDirection());
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
                        PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case RIGHT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack,
                        PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(layerY - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.debugLog("Dropping done, but didn't drop high enough to rotate!");
                    layerY = mc.thePlayer.getPosition().getY();
                    changeState(State.NONE);
                }
                break;
            case NONE:
                break;
        }
    }

    @Override
    public State calculateDirection() {

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
