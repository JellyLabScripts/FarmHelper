package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;

import static com.github.may2beez.farmhelperv2.util.BlockUtils.getRelativeBlockPos;

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

        getRotation().easeTo(getYaw(), getPitch(), 500);
        super.onEnable();
    }

    @Override
    public void actionAfterTeleport() {

    }

    @Override
    public void doAfterRewarpRotation() {
        if (!getRotation().rotating) {
            setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
            getRotation().easeTo(getYaw(), getPitch(), (long) (600 + Math.random() * 200));
        }
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
                }
                break;
            case RIGHT:
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else if (!GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else {
                    LogUtils.sendDebug("No direction found");
                }
                break;
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
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
        boolean f1 = true, f2 = true;

        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (BlockUtils.canWalkThrough(getRelativeBlockPos(i, 0, 0, AngleUtils.getClosest())) && f1) {
                return State.RIGHT;
            }
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(i, 1, 0, AngleUtils.getClosest())))
                f1 = false;
            if (BlockUtils.canWalkThrough(getRelativeBlockPos(-i, 0, 0, AngleUtils.getClosest())) && f2) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(-i, 1, 0, AngleUtils.getClosest())))
                f2 = false;
        }
        LogUtils.sendDebug("No direction found");
        return State.NONE;
    }
}
