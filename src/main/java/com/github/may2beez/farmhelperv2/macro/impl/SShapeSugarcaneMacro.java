package com.github.may2beez.farmhelperv2.macro.impl;

import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;

public class SShapeSugarcaneMacro extends AbstractMacro {

    @Override
    public void updateState() {
        if (currentState == null)
            changeState(State.NONE);
        switch (currentState) {
            case S: {
                if (hasWall(0, -1, getYaw() - 45f) &&
                        hasWall(0, -1, getYaw() + 45f)) {

                    if (getNearestSideWall(getYaw() + 45, -1) == -999) {
                        changeState(State.A);
                    }
                    if (getNearestSideWall(getYaw() - 45, 1) == -999) {
                        changeState(State.D);
                    }
                }
                break;
            }
            case A:
            case D: {
                changeState(State.S);
                break;
            }
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
                    changeState(State.NONE);
                    setLayerY(mc.thePlayer.getPosition().getY());
                } else  {
                    GameStateHandler.getInstance().scheduleNotMoving();
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
        if (currentState == null) return;
        switch (currentState) {
            case NONE:
                break;
            case A:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack
                );
                break;
            case D:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack
                );
                break;
            case S:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindBack,
                        mc.gameSettings.keyBindAttack
                );
                break;
            case DROPPING:
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                }
                break;
        }
    }

    @Override
    public void actionAfterTeleport() {
        setLayerY(mc.thePlayer.getPosition().getY());
        rowStartX = mc.thePlayer.posX;
        rowStartZ = mc.thePlayer.posZ;
    }

    public double rowStartX = 0;
    public double rowStartZ = 0;

    @Override
    public void onEnable() {
        if (FarmHelperConfig.customPitch) {
            setPitch(FarmHelperConfig.customPitchLevel);
        } else {
            setPitch((float) (Math.random() * 1) - 0.5f); // -0.5 to 0.5
        }
        if (FarmHelperConfig.customYaw) {
            setYaw(FarmHelperConfig.customYawLevel);
        } else {
            setYaw(AngleUtils.getClosestDiagonal());
        }
        MacroHandler.getInstance().setCrop(PlayerUtils.getFarmingCrop());
        if (currentState == null)
            changeState(State.NONE);
        mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.getInstance().getCrop());
        getRotation().easeTo(getYaw(), getPitch(), 500);
        rowStartX = mc.thePlayer.posX;
        rowStartZ = mc.thePlayer.posZ;
        super.onEnable();
    }
//
//    @Override
//    public void onTick() {
//        super.onTick();
//
//        if (isTping) {
//            return;
//        }
//
//        if (rotation.rotating) {
//            KeyBindUtils.stopMovement();
//            FarmHelper.gameState.scheduleNotMoving();
//            return;
//        } else {
//            rotatedAfterStart = true;
//        }
//
//        checkForRotationAfterTp();
//
//        // Don't do anything if macro is after teleportation for few seconds
//        if (lastTp.isScheduled() && !lastTp.passed()) {
//            KeyBindUtils.stopMovement();
//            return;
//        }
//
//        if (needAntistuck(false)) return;
//
//        PlayerUtils.getTool();
//
//        // Waiting for teleportation, don't move
//        if (beforeTeleportationPos != null) {
//            LogUtils.sendDebug("Waiting for tp...");
//            KeyBindUtils.stopMovement();
//            return;
//        }
//
//        if (FailsafeNew.emergency && FailsafeNew.findHighestPriorityElement() != FailsafeNew.FailsafeType.DESYNC) {
//            LogUtils.sendDebug("Blocking changing movement due to emergency");
//            return;
//        }
//
//        if (LagDetection.isLagging()) return;
//
//        // Update or invoke state, based on if player is moving or not
//        if (FarmHelper.gameState.canChangeDirection()) {
//            KeyBindUtils.stopMovement(FarmHelperConfig.holdLeftClickWhenChangingRow);
//            FarmHelper.gameState.scheduleNotMoving();
//            updateState();
//            invokeState();
//        } else {
//            if (!Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.posY) > 0.75 && Macro.mc.thePlayer.posY < 80) {
//                changeState(State.DROPPING);
//                FarmHelper.gameState.scheduleNotMoving();
//            }
//            invokeState();
//        }
//    }
//
//    private void updateState() {
//        if (currentState == null)
//            changeState(State.NONE);
//        switch (currentState) {
//            case S: {
//                if (hasWall(0, -1, yaw - 45f) &&
//                        hasWall(0, -1, yaw + 45f)) {
//
//                    if (getNearestSideWall(yaw + 45, -1) == -999) {
//                        changeState(State.A);
//                    }
//                    if (getNearestSideWall(yaw - 45, 1) == -999) {
//                        changeState(State.D);
//                    }
//                }
//                break;
//            }
//            case A:
//            case D: {
//                changeState(State.S);
//                break;
//            }
//            case DROPPING: {
//                LogUtils.sendDebug("On Ground: " + Macro.mc.thePlayer.onGround);
//                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) > 1.5) {
//                    if (FarmHelperConfig.rotateAfterDrop && !rotation.rotating) {
//                        LogUtils.sendDebug("Rotating 180");
//                        rotation.reset();
//                        yaw = yaw + 180;
//                        rotation.easeTo(yaw, pitch, (long) (400 + Math.random() * 300));
//                    }
//                    KeyBindUtils.stopMovement();
//                    changeState(State.NONE);
//                    layerY = Macro.mc.thePlayer.getPosition().getY();
//                } else  {
//                    FarmHelper.gameState.scheduleNotMoving();
//                }
//                break;
//            }
//            case NONE: {
//                changeState(calculateDirection());
//                break;
//            }
//        }
//    }
//
//    private void invokeState() {
//        if (currentState == null) return;
//        switch (currentState) {
//            case NONE:
//                break;
//            case A:
//                KeyBindUtils.holdThese(
//                        Macro.mc.gameSettings.keyBindLeft,
//                        Macro.mc.gameSettings.keyBindAttack
//                );
//                break;
//            case D:
//                KeyBindUtils.holdThese(
//                        Macro.mc.gameSettings.keyBindRight,
//                        Macro.mc.gameSettings.keyBindAttack
//                );
//                break;
//            case S:
//                KeyBindUtils.holdThese(
//                        Macro.mc.gameSettings.keyBindBack,
//                        Macro.mc.gameSettings.keyBindAttack
//                );
//                break;
//            case DROPPING:
//                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) <= 1.5) {
//                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
//                    layerY = Macro.mc.thePlayer.getPosition().getY();
//                    changeState(State.NONE);
//                }
//                break;
//        }
//    }
//
    @Override
    public State calculateDirection() {
        if(BlockUtils.isWater(BlockUtils.getRelativeBlock(2, -1, 1, getYaw() - 45f)) || BlockUtils.isWater(BlockUtils.getRelativeBlock(2, 0, 1, getYaw() - 45f))
                || BlockUtils.isWater(BlockUtils.getRelativeBlock(-1, -1, 1, getYaw() - 45f)) || BlockUtils.isWater(BlockUtils.getRelativeBlock(-1, 0, 1, getYaw() - 45f)))
            if(!(hasWall(0, 1, getYaw() - 45f) && hasWall(-1, 0, getYaw() - 45f)))
                return State.A;
            else if(BlockUtils.isWater(BlockUtils.getRelativeBlock(2, -1, 1, getYaw() + 45f)) || BlockUtils.isWater(BlockUtils.getRelativeBlock(2, 0, 1, getYaw() + 45f))
                    || BlockUtils.isWater(BlockUtils.getRelativeBlock(-1, -1, 1, getYaw() + 45f)) ||BlockUtils.isWater(BlockUtils.getRelativeBlock(-1, 0, 1, getYaw() + 45f)))
                if(!(hasWall(0, 1, getYaw() + 45f) && hasWall(11, 0, getYaw() + 45f)))
                    return State.D;
        return State.S;
    }

    boolean hasWall(int rightOffset, int frontOffset, float yaw) {
        return !BlockUtils.canWalkThrough(BlockUtils.getRelativeBlockPos(rightOffset, 0, frontOffset, yaw));
    }
    int getNearestSideWall(float yaw, int dir) { // right = 1; left = -1
        for(int i = 0; i < 8; i++) {
            if(hasWall(i * dir, 0, yaw)) return i;
        }
        return -999;
    }
}
