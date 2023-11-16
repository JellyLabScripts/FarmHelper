package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.init.Blocks;

public class SShapeCocoaBeanMacro extends AbstractMacro {

    @Override
    public void onEnable() {
        super.onEnable();
        if (getCurrentState() == null)
            changeState(State.NONE);
        FarmHelperConfig.CropEnum crop = PlayerUtils.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        if (FarmHelperConfig.customPitch) {
            setPitch(FarmHelperConfig.customPitchLevel);
        } else {
            setPitch(-70f + (float) (Math.random() * 0.6));
        }
        if (FarmHelperConfig.customYaw) {
            setYaw(FarmHelperConfig.customYawLevel);
        } else {
            setYaw(AngleUtils.getClosest());
        }
        getRotation().reset();
        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation(getYaw(), getPitch()),
                        FarmHelperConfig.getRandomRotationTime(), null
                )
        );
    }

    @Override
    public void actionAfterTeleport() {

    }

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case BACKWARD: {
                if (
                        GameStateHandler.getInstance().isFrontWalkable()
                                && !GameStateHandler.getInstance().isBackWalkable()
                                && GameStateHandler.getInstance().isRightWalkable()
                ) {
                    changeState(State.SWITCHING_LANE);
                    return;
                }
                break;
            }
            case FORWARD: {
                if (
                        !GameStateHandler.getInstance().isFrontWalkable()
                                && GameStateHandler.getInstance().isBackWalkable()
                                && GameStateHandler.getInstance().isRightWalkable()
                                && !GameStateHandler.getInstance().isLeftWalkable()
                ) {
                    changeState(State.SWITCHING_SIDE);
                    return;
                } else {
                    LogUtils.sendDebug("Can't go forward or backward!");
                    if (GameStateHandler.getInstance().isBackWalkable()) {
                        changeState(State.BACKWARD);
                    } else if (GameStateHandler.getInstance().isFrontWalkable()) {
                        changeState(State.FORWARD);
                    } else {
                        changeState(State.NONE);
                    }
                }
                break;
            }
            case SWITCHING_SIDE:
                if (GameStateHandler.getInstance().isBackWalkable()
                        && !GameStateHandler.getInstance().isRightWalkable()
                        && GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.BACKWARD);
                }
                break;
            case SWITCHING_LANE: {
                if (!GameStateHandler.getInstance().isBackWalkable()
                        && !GameStateHandler.getInstance().isRightWalkable()
                        && GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.FORWARD);
                    return;
                }
                break;
            }
            case NONE: {
                changeState(calculateDirection());
                break;
            }
        }
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case BACKWARD:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindBack,
                        mc.gameSettings.keyBindAttack
                );
                break;
            case FORWARD:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindAttack,
                        shouldHugWall() ? mc.gameSettings.keyBindLeft : null
                );
                break;
            case SWITCHING_LANE:
                if (hasLineChanged()
                        && !GameStateHandler.getInstance().isBackWalkable()
                        && GameStateHandler.getInstance().isLeftWalkable()
                ) {
                    changeState(State.FORWARD);
                    break;
                }
            case SWITCHING_SIDE:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                break;
            case NONE:
                break;
        }
    }

    @Override
    public State calculateDirection() {
        LogUtils.sendDebug("Calculating direction...");
        if (GameStateHandler.getInstance().isFrontWalkable() && GameStateHandler.getInstance().isRightWalkable())
            return State.FORWARD;
        if (GameStateHandler.getInstance().isBackWalkable())
            return State.BACKWARD;
        if (GameStateHandler.getInstance().isFrontWalkable())
            return State.FORWARD;
        if (GameStateHandler.getInstance().isBackWalkable() && GameStateHandler.getInstance().isLeftWalkable())
            return State.BACKWARD;
        LogUtils.sendDebug("Couldn't find a direction!");
        return State.NONE;
    }

    private boolean hasLineChanged() {
        if (BlockUtils.getRelativeBlock(-1, 0, 1).getMaterial().isSolid() && GameStateHandler.getInstance().isFrontWalkable()) {
            double decimalPartX = Math.abs(mc.thePlayer.getPositionVector().xCoord) % 1;
            double decimalPartZ = Math.abs(mc.thePlayer.getPositionVector().zCoord) % 1;
            float yaw = AngleUtils.getClosest(mc.thePlayer.rotationYaw);
            yaw = (yaw % 360 + 360) % 360;
            if (yaw == 180f && decimalPartX > 0.488) {
                return true;
            } else if (yaw == 270f && decimalPartZ > 0.488) {
                return true;
            } else if (yaw == 90f && decimalPartZ < 0.512) {
                return true;
            } else return yaw == 0f && decimalPartX < 0.512;
        }
        return false;
    }

    private boolean shouldHugWall() {
        if (BlockUtils.getRelativeBlock(-1, 0, 0).getMaterial().isSolid() && !BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)) {
            double x = mc.thePlayer.getPositionVector().xCoord % 1;
            double z = mc.thePlayer.getPositionVector().zCoord % 1;
            float yaw = AngleUtils.getClosest(mc.thePlayer.rotationYaw);
            yaw = (yaw % 360 + 360) % 360;
            if (yaw == 0) {
                return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
            } else if (yaw == 90) {
                return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
            } else if (yaw == 180) {
                return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
            } else if (yaw == 270) {
                return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
            }
        }
        return false;
    }
}
