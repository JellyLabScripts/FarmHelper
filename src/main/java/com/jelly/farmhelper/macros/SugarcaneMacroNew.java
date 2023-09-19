package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.FailsafeNew;
import com.jelly.farmhelper.features.LagDetection;
import com.jelly.farmhelper.utils.*;

import static com.jelly.farmhelper.utils.BlockUtils.*;

public class SugarcaneMacroNew extends Macro<SugarcaneMacroNew.State> {

    enum State {
        NONE,
        A,
        D,
        S,
        DROPPING
    }

    public double rowStartX = 0;
    public double rowStartZ = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        if (FarmHelper.config.customPitch) {
            pitch = FarmHelper.config.customPitchLevel;
        } else {
            pitch = (float) (Math.random() * 1) - 0.5f; // -0.5 to 0.5
        }
        if (FarmHelper.config.customYaw) {
            yaw = FarmHelper.config.customYawLevel;
        } else {
            yaw = AngleUtils.getClosestDiagonal();
        }
        MacroHandler.crop = MacroHandler.getFarmingCrop();
        if (currentState == null)
            changeState(State.NONE);
        Macro.mc.thePlayer.inventory.currentItem = PlayerUtils.getFarmingTool(MacroHandler.crop);
        rotation.easeTo(yaw, pitch, 500);
        rowStartX = Macro.mc.thePlayer.posX;
        rowStartZ = Macro.mc.thePlayer.posZ;
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

        checkForRotationAfterTp();

        // Don't do anything if macro is after teleportation for few seconds
        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (needAntistuck(false)) return;

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
            case S: {
                if (hasWall(0, -1, yaw - 45f) &&
                        hasWall(0, -1, yaw + 45f)) {

                    if (getNearestSideWall(yaw + 45, -1) == -999) {
                        changeState(State.A);
                    }
                    if (getNearestSideWall(yaw - 45, 1) == -999) {
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
                LogUtils.sendDebug("On Ground: " + Macro.mc.thePlayer.onGround);
                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) > 1.5) {
                    if (FarmHelper.config.rotateAfterDrop && !rotation.rotating) {
                        LogUtils.sendDebug("Rotating 180");
                        rotation.reset();
                        yaw = yaw + 180;
                        rotation.easeTo(yaw, pitch, (long) (400 + Math.random() * 300));
                    }
                    KeyBindUtils.stopMovement();
                    changeState(State.NONE);
                    layerY = Macro.mc.thePlayer.getPosition().getY();
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
            case NONE:
                break;
            case A:
                KeyBindUtils.holdThese(
                        Macro.mc.gameSettings.keyBindLeft,
                        Macro.mc.gameSettings.keyBindAttack
                );
                break;
            case D:
                KeyBindUtils.holdThese(
                        Macro.mc.gameSettings.keyBindRight,
                        Macro.mc.gameSettings.keyBindAttack
                );
                break;
            case S:
                KeyBindUtils.holdThese(
                        Macro.mc.gameSettings.keyBindBack,
                        Macro.mc.gameSettings.keyBindAttack
                );
                break;
            case DROPPING:
                if (Macro.mc.thePlayer.onGround && Math.abs(layerY - Macro.mc.thePlayer.getPosition().getY()) <= 1.5) {
                    LogUtils.sendDebug("Dropping done, but didn't drop high enough to rotate!");
                    layerY = Macro.mc.thePlayer.getPosition().getY();
                    changeState(State.NONE);
                }
                break;
        }
    }

    @Override
    public State calculateDirection() {
        if(isWater(getRelativeBlock(2, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw - 45f))
                || isWater(getRelativeBlock(-1, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(-1, 0, 1, yaw - 45f)))
            if(!(hasWall(0, 1, yaw - 45f) && hasWall(-1, 0, yaw - 45f)))
                return State.A;
            else if(isWater(getRelativeBlock(2, -1, 1, yaw + 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw + 45f))
                    || isWater(getRelativeBlock(-1, -1, 1, yaw + 45f)) ||isWater(getRelativeBlock(-1, 0, 1, yaw + 45f)))
                if(!(hasWall(0, 1, yaw + 45f) && hasWall(11, 0, yaw + 45f)))
                    return State.D;
        return State.S;
    }

    boolean hasWall(int rightOffset, int frontOffset, float yaw) {
        boolean f1 = !isWalkable(getRelativeBlock(rightOffset, 1, frontOffset, yaw));
        boolean f2 = !isWalkable(getRelativeBlock(rightOffset, 0, frontOffset, yaw));
        return f1 || f2;
    }
    int getNearestSideWall(float yaw, int dir) { // right = 1; left = -1
        for(int i = 0; i < 8; i++) {
            if(hasWall(i * dir, 0, yaw)) return i;
        }
        return -999;
    }
}
