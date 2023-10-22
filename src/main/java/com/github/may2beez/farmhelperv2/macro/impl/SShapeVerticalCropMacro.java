package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.AntiStuck;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;

public class SShapeVerticalCropMacro extends AbstractMacro {
    public enum ChangeLaneDirection {
        FORWARD,
        BACKWARD
    }

    public ChangeLaneDirection changeLaneDirection = null;

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case LEFT:
            case RIGHT: {
                if (GameStateHandler.getInstance().isLeftWalkable() && getCurrentState() == State.LEFT) {
                    // Probably stuck in dirt, continue going left
                    AntiStuck.getInstance().start();
                    changeState(State.LEFT);
                    return;
                }
                if (GameStateHandler.getInstance().isRightWalkable() && getCurrentState() == State.RIGHT) {
                    // Probably stuck in dirt, continue going right
                    AntiStuck.getInstance().start();
                    changeState(State.RIGHT);
                    return;
                }
                if (GameStateHandler.getInstance().isFrontWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.BACKWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                } else if (GameStateHandler.getInstance().isBackWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.FORWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    changeLaneDirection = ChangeLaneDirection.BACKWARD;
                } else {
                    if (GameStateHandler.getInstance().isLeftWalkable()) {
                        changeState(State.LEFT);
                    } else if (GameStateHandler.getInstance().isRightWalkable()) {
                        changeState(State.RIGHT);
                    } else {
                        changeState(State.NONE);
                        LogUtils.sendDebug("This shouldn't happen, but it did...");
                        LogUtils.sendDebug("Can't go forward or backward!");
                    }
                }
                break;
            }
            case SWITCHING_LANE: {
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else {
                    AntiStuck.getInstance().start();
                }
                break;
            }
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
                    changeLaneDirection = null;
                    if (FarmHelperConfig.rotateAfterDrop && !getRotation().rotating) {
                        LogUtils.sendDebug("Rotating 180");
                        getRotation().reset();
                        setYaw(getYaw() + 180);
                        getRotation().easeTo(getYaw(), getPitch(), (long) (400 + Math.random() * 300));
                    }
                    KeyBindUtils.stopMovement();
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                } else  {
                    GameStateHandler.getInstance().scheduleNotMoving();
                }
                break;
            }
            case NONE: {
                changeState(calculateDirection());
                break;
            }
            default:
                LogUtils.sendDebug("This shouldn't happen, but it did...");
                changeState(State.NONE);
        }
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case LEFT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack,
                        MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? mc.gameSettings.keyBindBack : null,
                        MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.CACTUS && MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.PUMPKIN && MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case RIGHT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack,
                        MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.CACTUS && PlayerUtils.shouldPushBack() ? mc.gameSettings.keyBindBack : null,
                        MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.CACTUS && MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.PUMPKIN && MacroHandler.getInstance().getCrop() != FarmHelperConfig.CropEnum.MELON && PlayerUtils.shouldWalkForwards() ? mc.gameSettings.keyBindForward : null
                );
                break;
            case SWITCHING_LANE:
                if (changeLaneDirection == null) {
                    if (GameStateHandler.getInstance().isFrontWalkable()) {
                        changeLaneDirection = ChangeLaneDirection.FORWARD;
                    } else if (GameStateHandler.getInstance().isBackWalkable()) {
                        changeLaneDirection = ChangeLaneDirection.BACKWARD;
                    } else {
                        AntiStuck.getInstance().start();
                        return;
                    }
                }
                switch (changeLaneDirection) {
                    case FORWARD:
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, FarmHelperConfig.holdLeftClickWhenChangingRow ? mc.gameSettings.keyBindAttack : null);
                        break;
                    case BACKWARD:
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindBack, FarmHelperConfig.holdLeftClickWhenChangingRow ? mc.gameSettings.keyBindAttack : null);
                        break;
                    default: {
                        LogUtils.sendDebug("I can't decide which direction to go!");
                        currentState = State.NONE;
                    }
                }
                break;
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                }
                break;
            case NONE:
                break;
        }
    }

    @Override
    public void onEnable() {
        changeLaneDirection = null;
        FarmHelperConfig.CropEnum crop = PlayerUtils.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        if (FarmHelperConfig.customPitch) {
            setPitch(FarmHelperConfig.customPitchLevel);
        } else {
            if (FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_PUMPKIN_MELON_MELONGKINGDE.ordinal()) {
                setPitch((float) (-58.5f + Math.random() * 1f));
            } else if (crop == FarmHelperConfig.CropEnum.NETHER_WART || crop == FarmHelperConfig.CropEnum.CACTUS) {
                setPitch((float) (0f + Math.random() * 0.5f));
            } else if (FarmHelperConfig.macroType == FarmHelperConfig.MacroEnum.S_PUMPKIN_MELON.ordinal()) {
                setPitch(28 + (float) (Math.random() * 2)); //28-30
            } else {
                setPitch((float) (2.8f + Math.random() * 0.5f));
            }
        }
        if (FarmHelperConfig.customYaw) {
            setYaw(FarmHelperConfig.customYawLevel);
        } else {
            setYaw(AngleUtils.getClosest());
        }
        getRotation().reset();
        super.onEnable();
        getRotation().easeTo(getYaw(), getPitch(), FarmHelperConfig.getRandomRotationTime());
    }

    @Override
    public boolean additionalCheck() {
        return changeLaneDirection != null && changeLaneDirection == ChangeLaneDirection.BACKWARD;
    }

    @Override
    public void actionAfterTeleport() {
        changeLaneDirection = null;
    }

    @Override
    public State calculateDirection() {

        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg() != -1337) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg();
        } else {
            yaw = mc.thePlayer.rotationYaw;
        }

        for (int i = 1; i < 180; i++) {
            if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i, 0, 0, yaw))) {
                if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, -1, 1, yaw)) || BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, -1, 0, yaw))) {
                    return State.RIGHT;
                } else {
                    LogUtils.sendDebug("Failed right: " + BlockUtils.getRelativeBlockPos(i - 1, 0, 1, yaw));
                    return State.LEFT;
                }
            } else if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-i, 0, 0, yaw))) {
                if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-i + 1, 0, 1, yaw)) || BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-i + 1, -1, 0, yaw))) {
                    return State.LEFT;
                } else {
                    LogUtils.sendDebug("Failed left: " + BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i - 1, 0, 1, yaw)));
                    return State.RIGHT;
                }
            }
        }
        LogUtils.sendDebug("Cannot find direction. Length > 180. Defaulting to RIGHT");
        return State.RIGHT;
    }
}
