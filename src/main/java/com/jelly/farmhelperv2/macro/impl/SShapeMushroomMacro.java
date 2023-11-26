package com.jelly.farmhelperv2.macro.impl;

import cc.polyfrost.oneconfig.libs.universal.UMath;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;

public class SShapeMushroomMacro extends AbstractMacro {

    @Override
    public void onEnable() {
        FarmHelperConfig.CropEnum crop = PlayerUtils.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        if (FarmHelperConfig.customPitch) {
            setPitch(FarmHelperConfig.customPitchLevel);
        } else {
            setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
        }
        if (FarmHelperConfig.customYaw) {
            setYaw(FarmHelperConfig.customYawLevel);
        } else {
            setYaw(AngleUtils.getClosestDiagonal());
        }
        setClosest90Deg(AngleUtils.getClosest());

        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation(getYaw(), getPitch()),
                        500, null
                )
        );
        super.onEnable();
    }

    @Override
    public void actionAfterTeleport() {
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
    }

    @Override
    public void doAfterRewarpRotation() {
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
        setClosest90Deg((float) UMath.wrapAngleTo180(AngleUtils.getClosest() + 180));
        setYaw(AngleUtils.getClosestDiagonal(getYaw() + 180));
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case LEFT:
                if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else if (!GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }
                break;
            case RIGHT:
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else if (!GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }
                break;
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
                    if (FarmHelperConfig.rotateAfterDrop && !getRotation().isRotating()) {
                        LogUtils.sendDebug("Rotating 180");
                        getRotation().reset();
                        setYaw(getYaw() + 180);

                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation(getYaw(), getPitch()),
                                        (long) (400 + Math.random() * 300), null
                                )
                        );
                    }
                    KeyBindUtils.stopMovement();
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                } else {
                    GameStateHandler.getInstance().scheduleNotMoving();
                }
                break;
            }
            case NONE:
                changeState(calculateDirection());
                break;
        }
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case RIGHT: {
                KeyBindUtils.holdThese(
                        mushroom45DegreeLeftSide() ? mc.gameSettings.keyBindRight : mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindAttack
                );
                break;
            }
            case LEFT: {
                KeyBindUtils.holdThese(
                        mushroom45DegreeLeftSide() ? mc.gameSettings.keyBindForward : mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack
                );
                break;
            }
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                }
                break;
            case NONE: {
                LogUtils.sendDebug("No direction found");
                break;
            }
        }
    }

    private boolean mushroom45DegreeLeftSide() {
        float targetAngle = 45f; // Angle around 45 degrees
        float counterClockwiseThreshold = -315f; // Angle around -315 degrees
        float tolerance = 2f; // Tolerance of 2 degrees

        float angleDifference = AngleUtils.normalizeAngle(AngleUtils.getClosest() - getYaw());

        return Math.abs(angleDifference - targetAngle) < tolerance ||
                Math.abs(angleDifference - counterClockwiseThreshold) < tolerance;
    }


    @Override
    public State calculateDirection() {
        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 1; i < 180; i++) {
            if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(i, 0, 0, getClosest90Deg()))) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-i, 0, 0, getClosest90Deg())))
                return State.RIGHT;
        }
        LogUtils.sendDebug("No direction found");
        return State.NONE;
    }
}
