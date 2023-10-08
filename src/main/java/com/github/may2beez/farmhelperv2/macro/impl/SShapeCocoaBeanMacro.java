package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
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
        getRotation().easeTo(getYaw(), getPitch(), FarmHelperConfig.getRandomRotationTime());
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case BACKWARD: {
                if (GameStateHandler.getInstance().isFrontWalkable() && !GameStateHandler.getInstance().isBackWalkable()) {
                    changeState(State.SWITCHING_LANE);
                    return;
                }
                break;
            }
            case FORWARD: {
                if (!GameStateHandler.getInstance().isFrontWalkable() && GameStateHandler.getInstance().isBackWalkable()) {
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
                if (!GameStateHandler.getInstance().isRightWalkable() && GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.BACKWARD);
                }
                break;
            case SWITCHING_LANE: {
                if (shouldPushForward() || hasLineChanged()) {
                    changeState(State.FORWARD);
                    return;
                }
                if (GameStateHandler.getInstance().isRightWalkable() && GameStateHandler.getInstance().isLeftWalkable()) {
                    if (!GameStateHandler.getInstance().isFrontWalkable()) {
                        LogUtils.sendDebug("Both sides are walkable, switching lane");
                    } else
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
                        ((!isHuggingAWall() || BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)) && BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.air)) ? mc.gameSettings.keyBindLeft : null
                );
                break;
            case SWITCHING_SIDE:
            case SWITCHING_LANE:
                KeyBindUtils.holdThese(mc.gameSettings.keyBindRight);
                break;
            case NONE:
                break;
        }
    }

    @Override
    public State calculateDirection() {
        LogUtils.sendDebug("Calculating direction");
        for (int i = 1; i < 180; i++) {
            if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, i))) {
                if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(1, 1, i - 1))) {
                    return State.FORWARD;
                } else {
                    LogUtils.sendDebug("Failed forward: " + BlockUtils.getRelativeBlock(1, 1, i - 1));
                    return State.BACKWARD;
                }
            } else if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(0, 0, -i))) {
                if (BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-1, 0, -i + 1))) {
                    return State.BACKWARD;
                } else {
                    LogUtils.sendDebug("Failed backward: " + BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(-1, 0, i - 1)));
                    return State.FORWARD;
                }
            }
        }
        LogUtils.sendDebug("Cannot find direction. Length > 180");
        return State.NONE;
    }

    private boolean shouldPushForward() {
        return (mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0,0,0)).getBlock() instanceof BlockDoor
                || mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0,0,0)).getBlock() instanceof BlockTrapDoor);
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

    private boolean isHuggingAWall() {
        if (BlockUtils.getRelativeBlock(-1, 0, 0).getMaterial().isSolid() && !BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)) {
            double decimalPartX = Math.abs(mc.thePlayer.getPositionVector().xCoord) % 1;
            double decimalPartZ = Math.abs(mc.thePlayer.getPositionVector().zCoord) % 1;
            float yaw = AngleUtils.getClosest(mc.thePlayer.rotationYaw);
            yaw = (yaw % 360 + 360) % 360;
            if (yaw == 180f && decimalPartX < 0.5) {
                return true;
            } else if (yaw == 270f && decimalPartZ < 0.5) {
                return true;
            } else if (yaw == 90f && decimalPartZ > 0.5) {
                return true;
            } else return yaw == 0f && decimalPartX > 0.5;
        }
        return false;
    }
}
