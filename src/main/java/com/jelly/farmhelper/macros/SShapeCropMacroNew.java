package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.features.FailsafeNew;
import com.jelly.farmhelper.features.LagDetection;
import com.jelly.farmhelper.utils.*;

import static com.jelly.farmhelper.utils.BlockUtils.*;

public class SShapeCropMacroNew extends Macro<SShapeCropMacroNew.State> {

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

    @Override
    public void onEnable() {
        super.onEnable();
        changeLaneDirection = null;
        if (currentState == null)
            changeState(State.NONE);
        Config.CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
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
        if (FarmHelper.config.customYaw) {
            yaw = FarmHelper.config.customYawLevel;
        } else {
            yaw = AngleUtils.getClosest();
        }
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
            changeLaneDirection = null;
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

        if (needAntistuck(changeLaneDirection != null && changeLaneDirection == ChangeLaneDirection.BACKWARD)) return;

        CropUtils.getTool();

        // Waiting for teleportation, don't move
        if (beforeTeleportationPos != null) {
            LogUtils.sendDebug("Waiting for tp...");
            KeyBindUtils.stopMovement();
            return;
        }

        if (FailsafeNew.emergency && FailsafeNew.findHighestPriorityElement() != FailsafeNew.FailsafeType.DESYNC) {
            LogUtils.sendDebug("Blocking changing movement due to emergency");
            return;
        }

        if (LagDetection.isLagging()) return;

        // Update or invoke state, based on if player is moving or not
        if (FarmHelper.gameState.canChangeDirection()) {
            KeyBindUtils.stopMovement(FarmHelper.config.holdLeftClickWhenChangingRow);
            FarmHelper.gameState.scheduleNotMoving();
            updateState();
            invokeState();
        } else {
            if (!Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.posY) > 0.75 && Macro.mc.thePlayer.posY < 80) {
                changeState(State.DROPPING);
                FarmHelper.gameState.scheduleNotMoving();
            }
            invokeState();
        }
    }

    private void updateState() {
        if (currentState == null)
            changeState(State.NONE);
        switch (currentState) {
            case LEFT:
            case RIGHT: {
                if (FarmHelper.gameState.leftWalkable && currentState == State.LEFT) {
                    // Probably stuck in dirt, continue going left
                    unstuck(false);
                    changeState(State.LEFT);
                    return;
                }
                if (FarmHelper.gameState.rightWalkable && currentState == State.RIGHT) {
                    // Probably stuck in dirt, continue going right
                    unstuck(true);
                    changeState(State.RIGHT);
                    return;
                }
                if (FarmHelper.gameState.frontWalkable) {
                    if (changeLaneDirection == ChangeLaneDirection.BACKWARD) {
                        // Probably stuck in dirt
                        unstuck(true);
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                } else if (FarmHelper.gameState.backWalkable) {
                    if (changeLaneDirection == ChangeLaneDirection.FORWARD) {
                        // Probably stuck in dirt
                        unstuck(false);
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.BACKWARD;
                } else {
                    LogUtils.sendDebug("This shouldn't happen, but it did...");
                    LogUtils.sendDebug("Can't go forward or backward!");
                    if (FarmHelper.gameState.leftWalkable) {
                        changeState(State.LEFT);
                    } else if (FarmHelper.gameState.rightWalkable) {
                        changeState(State.RIGHT);
                    } else {
                        changeState(State.NONE);
                    }
                }
                break;
            }
            case SWITCHING_LANE: {
                if (FarmHelper.gameState.leftWalkable) {
                    changeState(State.LEFT);
                } else if (FarmHelper.gameState.rightWalkable) {
                    changeState(State.RIGHT);
                } else {
                    unstuck(changeLaneDirection == ChangeLaneDirection.BACKWARD);
                }
                break;
            }
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + Macro.mc.thePlayer.onGround);
                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) > 1.5) {
                    changeLaneDirection = null;
                    if (FarmHelper.config.rotateAfterDrop && !rotation.rotating) {
                        LogUtils.sendDebug("Rotating 180");
                        rotation.reset();
                        yaw = yaw + 180;
                        rotation.easeTo(yaw, pitch, (long) (400 + Math.random() * 300));
                    }
                    KeyBindUtils.stopMovement();
                    layerY = Macro.mc.thePlayer.getPosition().getY();
                    changeState(State.NONE);
                } else  {
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
        if (currentState == null) return;
        switch (currentState) {
            case LEFT:
                KeyBindUtils.holdThese(
                        Macro.mc.gameSettings.keyBindLeft,
                        Macro.mc.gameSettings.keyBindAttack,
                        MacroHandler.crop == Config.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? Macro.mc.gameSettings.keyBindBack : null,
                        MacroHandler.crop != Config.CropEnum.CACTUS && MacroHandler.crop != Config.CropEnum.PUMPKIN && MacroHandler.crop != Config.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? Macro.mc.gameSettings.keyBindForward : null
                );
                break;
            case RIGHT:
                KeyBindUtils.holdThese(
                        Macro.mc.gameSettings.keyBindRight,
                        Macro.mc.gameSettings.keyBindAttack,
                        MacroHandler.crop == Config.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? Macro.mc.gameSettings.keyBindBack : null,
                        MacroHandler.crop != Config.CropEnum.CACTUS && MacroHandler.crop != Config.CropEnum.PUMPKIN && MacroHandler.crop != Config.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? Macro.mc.gameSettings.keyBindForward : null
                );
                break;
            case SWITCHING_LANE:
                if (changeLaneDirection == null) {
                    if (FarmHelper.gameState.frontWalkable) {
                        changeLaneDirection = ChangeLaneDirection.FORWARD;
                    } else if (FarmHelper.gameState.backWalkable) {
                        changeLaneDirection = ChangeLaneDirection.BACKWARD;
                    } else {
                        unstuck(false);
                        return;
                    }
                }
                switch (changeLaneDirection) {
                    case FORWARD:
                        KeyBindUtils.holdThese(Macro.mc.gameSettings.keyBindForward, FarmHelper.config.holdLeftClickWhenChangingRow ? Macro.mc.gameSettings.keyBindAttack : null);
                        break;
                    case BACKWARD:
                        KeyBindUtils.holdThese(Macro.mc.gameSettings.keyBindBack, FarmHelper.config.holdLeftClickWhenChangingRow ? Macro.mc.gameSettings.keyBindAttack : null);
                        break;
                    default: {
                        LogUtils.sendDebug("I can't decide which direction to go!");
                        currentState = State.NONE;
                    }
                }
                break;
            case DROPPING:
                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    layerY = Macro.mc.thePlayer.getPosition().getY();
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
            if (!canWalkThrough(BlockUtils.getRelativeBlockPos(i, 0, 0))) {
                if (canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, -1, 1)) || canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, -1, 0))) {
                    return State.RIGHT;
                } else {
                    LogUtils.sendDebug("Failed right: " + BlockUtils.getRelativeBlockPos(i - 1, 0, 1));
                    return State.LEFT;
                }
            } else if (!canWalkThrough(BlockUtils.getRelativeBlockPos(-i, 0, 0))) {
                if (canWalkThrough(BlockUtils.getRelativeBlockPos(-i + 1, 0, 1)) || canWalkThrough(BlockUtils.getRelativeBlockPos(-i + 1, -1, 0))) {
                    return State.LEFT;
                } else {
                    LogUtils.sendDebug("Failed left: " + canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, 0, 1)));
                    return State.RIGHT;
                }
            }
        }
        LogUtils.sendDebug("Cannot find direction. Length > 180");
        return State.NONE;
    }
}
