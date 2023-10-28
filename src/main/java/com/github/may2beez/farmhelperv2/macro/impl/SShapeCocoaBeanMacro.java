package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.feature.impl.LagDetector;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.intellij.lang.annotations.Subst;

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
                    && (GameStateHandler.getInstance().isLeftWalkable() || isStandingInDoorOrTrapdoor() || BlockUtils.getRelativeBlock(-1, 0, 0) instanceof BlockDoor)
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
                if (
                    !GameStateHandler.getInstance().isFrontWalkable()
                    && GameStateHandler.getInstance().isBackWalkable()
                    && !GameStateHandler.getInstance().isRightWalkable()
                    && GameStateHandler.getInstance().isLeftWalkable()
                ) {
                    changeState(State.BACKWARD);
                }
                break;
            case SWITCHING_LANE: {
                LogUtils.sendDebug("hasLineChanged(): " + hasLineChanged());
                if ((isStandingInDoorOrTrapdoor() || hasLineChanged())
                    && (GameStateHandler.getInstance().isFrontWalkable() || BlockUtils.getRelativeBlock(0, 1, 1) instanceof BlockTrapDoor)
                    && !GameStateHandler.getInstance().isBackWalkable()
                    && GameStateHandler.getInstance().isLeftWalkable()
                ) {
                    changeState(State.FORWARD);
                    return;
                }
                if (GameStateHandler.getInstance().isRightWalkable() && GameStateHandler.getInstance().isLeftWalkable() && !GameStateHandler.getInstance().isBackWalkable()) {
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
    public void onTick() {
        super.onTick();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (LagDetector.getInstance().isLagging()) return;

        if (getCurrentState() == State.SWITCHING_LANE) {
            LogUtils.sendDebug("hasLineChanged(): " + hasLineChanged());
            if (hasLineChanged()
                    && !GameStateHandler.getInstance().isBackWalkable()
                    && GameStateHandler.getInstance().isLeftWalkable()
            ) {
                changeState(State.FORWARD);
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
        if (GameStateHandler.getInstance().isFrontWalkable() && GameStateHandler.getInstance().isRightWalkable())
            return State.FORWARD;
        if (GameStateHandler.getInstance().isBackWalkable())
            return State.BACKWARD;
        if (GameStateHandler.getInstance().isFrontWalkable())
            return State.FORWARD;
        if (GameStateHandler.getInstance().isBackWalkable() && GameStateHandler.getInstance().isLeftWalkable())
            return State.BACKWARD;
        LogUtils.sendDebug("Cannot find direction. Length > 180");
        return State.NONE;
    }

    private boolean isStandingInDoorOrTrapdoor() {
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
