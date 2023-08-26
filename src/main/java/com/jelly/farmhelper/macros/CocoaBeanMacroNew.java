package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config;
import com.jelly.farmhelper.features.FailsafeNew;
import com.jelly.farmhelper.hud.DebugHUD;
import com.jelly.farmhelper.utils.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.BlockUtils.*;

public class CocoaBeanMacroNew extends Macro<CocoaBeanMacroNew.State> {

    public enum State {
        BACKWARD,
        FORWARD,
        SWITCHING_SIDE,
        SWITCHING_LANE,
        NONE
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (currentState == null)
            changeState(State.NONE);
        Config.CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugFullLog("Crop: " + crop);
        MacroHandler.crop = crop;
        CropUtils.getTool();
        if (FarmHelper.config.customPitch) {
            pitch = FarmHelper.config.customPitchLevel;
        } else {
            pitch = -70f + (float) (Math.random() * 0.6);
        }
        yaw = AngleUtils.getClosest();
        rotation.easeTo(yaw, pitch, 500);
    }

    @Override
    public void triggerTpCooldown() {
        super.triggerTpCooldown();
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

        checkForRotationFailsafe();

        if (isStuck()) return;

        CropUtils.getTool();

        // Waiting for teleportation, don't move
        if (beforeTeleportationPos != null) {
            LogUtils.debugFullLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            return;
        }

        if (FailsafeNew.emergency) {
            LogUtils.debugFullLog("Blocking changing movement due to emergency");
            return;
        }

        DebugHUD.hasLineChanged = hasLineChanged();
        DebugHUD.isHuggingAWall = isHuggingAWall();

        // Update or invoke state, based on if player is moving or not
        if (FarmHelper.gameState.canChangeDirection() || hasLineChanged()) {
            KeyBindUtils.stopMovement(FarmHelper.config.holdLeftClickWhenChangingRow);
            FarmHelper.gameState.scheduleNotMoving();
            updateState();
            invokeState();
        }
    }

    private void updateState() {
        if (currentState == null)
            changeState(State.NONE);
        switch (currentState) {
            case BACKWARD: {
                if (FarmHelper.gameState.frontWalkable && !FarmHelper.gameState.backWalkable) {
                    changeState(State.SWITCHING_LANE);
                    return;
                }
                break;
            }
            case FORWARD: {
                if (!FarmHelper.gameState.frontWalkable && FarmHelper.gameState.backWalkable) {
                    changeState(State.SWITCHING_SIDE);
                    return;
                } else {
                    LogUtils.debugLog("Can't go forward or backward!");
                    if (FarmHelper.gameState.backWalkable) {
                        changeState(State.BACKWARD);
                    } else if (FarmHelper.gameState.frontWalkable) {
                        changeState(State.FORWARD);
                    } else {
                        changeState(State.NONE);
                    }
                }
                break;
            }
            case SWITCHING_SIDE:
                if (!FarmHelper.gameState.rightWalkable && FarmHelper.gameState.leftWalkable) {
                    changeState(State.BACKWARD);
                }
                break;
            case SWITCHING_LANE: {
                if (shouldPushForward() || hasLineChanged()) {
                    changeState(State.FORWARD);
                    return;
                }
                if (FarmHelper.gameState.rightWalkable && FarmHelper.gameState.leftWalkable) {
                    if (!FarmHelper.gameState.frontWalkable) {
                        LogUtils.debugFullLog("Both sides are walkable, switching lane");
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

    private void invokeState() {
        if (currentState == null) return;
        switch (currentState) {
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
        LogUtils.debugFullLog("Calculating direction");
        for (int i = 1; i < 180; i++) {
            if (isWalkable(BlockUtils.getRelativeBlock(0, 0, i))) {
                if (isWalkable(BlockUtils.getRelativeBlock(1, 1, i - 1))) {
                    return State.FORWARD;
                } else {
                    LogUtils.debugFullLog("Failed forward: " + BlockUtils.getRelativeBlock(1, 1, i - 1));
                    return State.BACKWARD;
                }
            } else if (isWalkable(BlockUtils.getRelativeBlock(0, 0, -i))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-1, 0, -i + 1))) {
                    return State.BACKWARD;
                } else {
                    LogUtils.debugFullLog("Failed backward: " + isWalkable(BlockUtils.getRelativeBlock(-1, 0, i - 1)));
                    return State.FORWARD;
                }
            }
        }
        LogUtils.debugLog("Cannot find direction. Length > 180");
        return State.NONE;
    }

    private boolean shouldPushForward() {
        return (mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0,0,0)).getBlock() instanceof BlockDoor
                || mc.theWorld.getBlockState(BlockUtils.getRelativeBlockPos(0,0,0)).getBlock() instanceof BlockTrapDoor);
    }

    private boolean hasLineChanged() {
        if (BlockUtils.getRelativeBlock(-1, 0, 1).getMaterial().isSolid() && gameState.frontWalkable) {
            double decimalPartX = Math.abs(mc.thePlayer.getPositionVector().xCoord) % 1;
            double decimalPartZ = Math.abs(mc.thePlayer.getPositionVector().zCoord) % 1;
            float yaw = AngleUtils.getClosest(mc.thePlayer.rotationYaw);
            yaw = (yaw % 360 + 360) % 360;
            if (yaw == 180f && decimalPartX > 0.488) { // North: X > 488
//                LogUtils.debugFullLog("North");
                return true;
            } else if (yaw == 270f && decimalPartZ > 0.488) { // East: Z > 488
//                LogUtils.debugFullLog("East");
                return true;
            } else if (yaw == 90f && decimalPartZ < 0.512) { // West: Z < 512
//                LogUtils.debugFullLog("West");
                return true;
            } else if (yaw == 0f && decimalPartX < 0.512) { // South: X < 512
//                LogUtils.debugFullLog("South");
                return true;
            }
        }
        return false;
    }

    private boolean isHuggingAWall() {
        if (BlockUtils.getRelativeBlock(-1, 0, 0).getMaterial().isSolid() && !BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)) {
            double decimalPartX = Math.abs(mc.thePlayer.getPositionVector().xCoord) % 1;
            double decimalPartZ = Math.abs(mc.thePlayer.getPositionVector().zCoord) % 1;
            float yaw = AngleUtils.getClosest(mc.thePlayer.rotationYaw);
            yaw = (yaw % 360 + 360) % 360;
            if (yaw == 180f && decimalPartX < 0.5) { // North: X > 488
//                LogUtils.debugFullLog("North");
                return true;
            } else if (yaw == 270f && decimalPartZ < 0.5) { // East: Z > 488
//                LogUtils.debugFullLog("East");
                return true;
            } else if (yaw == 90f && decimalPartZ > 0.5) { // West: Z < 512
//                LogUtils.debugFullLog("West");
                return true;
            } else if (yaw == 0f && decimalPartX > 0.5) { // South: X < 512
//                LogUtils.debugFullLog("South");
                return true;
            }
        }
        return false;
    }
}
