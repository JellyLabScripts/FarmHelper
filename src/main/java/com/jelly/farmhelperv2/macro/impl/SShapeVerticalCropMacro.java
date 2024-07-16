package com.jelly.farmhelperv2.macro.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.impl.AntiStuck;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.*;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Optional;

public class SShapeVerticalCropMacro extends AbstractMacro {
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
                    setCurrentState(State.LEFT);
                    AntiStuck.getInstance().setDirectionBlockPos(BlockUtils.getRelativeBlockPos(0, 0, -1, getYaw()));
                    AntiStuck.getInstance().start();
                    return;
                }
                if (GameStateHandler.getInstance().isRightWalkable() && getCurrentState() == State.RIGHT) {
                    // Probably stuck in dirt, continue going right
                    setCurrentState(State.RIGHT);
                    AntiStuck.getInstance().setDirectionBlockPos(BlockUtils.getRelativeBlockPos(0, 0, -1, getYaw()));
                    AntiStuck.getInstance().start();
                    return;
                }
                if (GameStateHandler.getInstance().isFrontWalkable() && !FarmHelperConfig.alwaysHoldW) {
                    if (stuckInMelonsOrPumpkins()) {
                        AntiStuck.getInstance().setDirectionBlockPos(BlockUtils.getRelativeBlockPos(0, 0, -1, getYaw()));
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    setWalkingDirection();
                } else if (GameStateHandler.getInstance().isBackWalkable() && !FarmHelperConfig.alwaysHoldW) {
                    if (stuckInMelonsOrPumpkins()) {
                        AntiStuck.getInstance().setDirectionBlockPos(BlockUtils.getRelativeBlockPos(0, 0, -1, getYaw()));
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                    setWalkingDirection();
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
                if (getWalkingDirection() == WalkingDirection.X) {
                    int currentZ = mc.thePlayer.getPosition().getZ();
                    if (lagBackDetected(currentZ)) break;
                } else if (getWalkingDirection() == WalkingDirection.Z) {
                    int currentX = mc.thePlayer.getPosition().getX();
                    if (lagBackDetected(currentX)) break;
                }
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                } else if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                } else {
                    changeState(State.NONE);
                }
                break;
            }
            case DROPPING: {
                LogUtils.sendDebug("onGround: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
                    changeLaneDirection = null;
                    if (FarmHelperConfig.rotateAfterDrop) {
                        LogUtils.sendDebug("Rotating 180...");
                        setYaw(AngleUtils.getClosest(getYaw() + 180));
                        setClosest90Deg(Optional.of(AngleUtils.getClosest(getYaw())));
                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation(getYaw(), getPitch()),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                ).easeOutBack(true)
                        );
                    }
                    KeyBindUtils.stopMovement(FarmHelperConfig.holdLeftClickWhenChangingRow);
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                } else {
                    if (mc.thePlayer.onGround) {
                        setLayerY(mc.thePlayer.getPosition().getY());
                        changeState(State.NONE);
                    } else {
                        GameStateHandler.getInstance().scheduleNotMoving();
                    }
                }
                break;
            }
            case NONE: {
                changeState(calculateDirection());
                break;
            }
            default: {
                LogUtils.sendDebug("This shouldn't happen, but it did...");
                changeState(State.NONE);
            }
        }
    }

    private boolean stuckInMelonsOrPumpkins() {
        Block leftBlock = BlockUtils.getBlock(BlockUtils.getRelativeBlockPos(-1, 0, 0, getYaw()));
        Block rightBlock = BlockUtils.getBlock(BlockUtils.getRelativeBlockPos(1, 0, 0, getYaw()));
        return leftBlock.equals(Blocks.pumpkin) || leftBlock.equals(Blocks.melon_block) || rightBlock.equals(Blocks.pumpkin) || rightBlock.equals(Blocks.melon_block);
    }

    private boolean lagBackDetected(int currentCoord) {
        if (Math.abs(currentCoord - getPreviousWalkingCoord()) < 1) {
            if (getPreviousState() == State.LEFT) {
                AntiStuck.getInstance().setIntersectingBlockPos(BlockUtils.getRelativeBlockPos(-1, 0, 0, getYaw()));
            } else if (getPreviousState() == State.RIGHT) {
                AntiStuck.getInstance().setIntersectingBlockPos(BlockUtils.getRelativeBlockPos(1, 0, 0, getYaw()));
            } else {
                if (getCurrentState() == State.LEFT) {
                    AntiStuck.getInstance().setIntersectingBlockPos(BlockUtils.getRelativeBlockPos(-1, 0, 0, getYaw()));
                } else if (getCurrentState() == State.RIGHT) {
                    AntiStuck.getInstance().setIntersectingBlockPos(BlockUtils.getRelativeBlockPos(1, 0, 0, getYaw()));
                }
            }
            AntiStuck.getInstance().start();
            LogUtils.sendWarning("Probability of lag back detected!");
            AntiStuck.getInstance().setLagBackCounter(AntiStuck.getInstance().getLagBackCounter() + 1);
            if (AntiStuck.getInstance().getLagBackCounter() >= 3) {
                LogUtils.sendWarning("Lag back stuck detected! Resetting state...");
                changeState(State.NONE);
                AntiStuck.getInstance().setLagBackCounter(0);
                AntiStuck.getInstance().stop();
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case LEFT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack,
                        PlayerUtils.shouldWalkForwards() || FarmHelperConfig.alwaysHoldW ? mc.gameSettings.keyBindForward : null
                );
                break;
            case RIGHT:
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack,
                        PlayerUtils.shouldWalkForwards() || FarmHelperConfig.alwaysHoldW ? mc.gameSettings.keyBindForward : null
                );
                break;
            case SWITCHING_LANE:
                if (changeLaneDirection == null) {
                    if (GameStateHandler.getInstance().isFrontWalkable()) {
                        changeLaneDirection = ChangeLaneDirection.FORWARD;
                    } else if (GameStateHandler.getInstance().isBackWalkable()) {
                        changeLaneDirection = ChangeLaneDirection.BACKWARD;
                    } else {
                        changeState(State.NONE);
                        return;
                    }
                }
                switch (changeLaneDirection) {
                    case FORWARD:
                        KeyBindUtils.holdThese(mc.gameSettings.keyBindForward, mc.gameSettings.keyBindSprint, FarmHelperConfig.holdLeftClickWhenChangingRow ? mc.gameSettings.keyBindAttack : null);
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
            case NONE: {
                changeState(calculateDirection());
                break;
            }
            default: {
                LogUtils.sendDebug("This shouldn't happen, but it did...");
                changeState(State.NONE);
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        changeLaneDirection = null;
        if (!isPitchSet()) {
            if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_V_NORMAL_TYPE) {
                setPitch((float) (2.8f + Math.random() * 0.5f));
            } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_PUMPKIN_MELON) {
                setPitch(28 + (float) (Math.random() * 2)); // 28 - 30
            } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_PUMPKIN_MELON_MELONGKINGDE) { // melonkingdebil
                setPitch((float) (-59.2f + Math.random() * 1f)); // -59.2 - -58.2
            } else if (MacroHandler.getInstance().getCrop() == FarmHelperConfig.CropEnum.NETHER_WART || FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_CACTUS) {
                setPitch((float) (0f + Math.random() * 0.5f)); // 0 - 0.5
            } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_CACTUS_SUNTZU) {
                setPitch((float) (-38 - Math.random() * 1.5f)); // -38 - -39.5
            } else if (FarmHelperConfig.getMacro() == FarmHelperConfig.MacroEnum.S_COCOA_BEANS_LEFT_RIGHT) {
                setPitch(-90f);
            }
        }
        if (!isYawSet()) {
            setYaw(AngleUtils.getClosest());
            setClosest90Deg(Optional.of(AngleUtils.getClosest(getYaw())));
        }
        setRestoredState(false);
        if (MacroHandler.getInstance().isTeleporting()) return;
        if (FarmHelperConfig.dontFixAfterWarping && Math.abs(getYaw() - AngleUtils.get360RotationYaw()) < 0.1) return;
        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation(getYaw(), getPitch()),
                        FarmHelperConfig.getRandomRotationTime(), null
                ).easeOutBack(!MacroHandler.getInstance().isResume() && FarmHelperConfig.getMacro() != FarmHelperConfig.MacroEnum.S_COCOA_BEANS_LEFT_RIGHT)
        );
    }

    @Override
    public void actionAfterTeleport() {
        changeLaneDirection = null;
    }

    @Override
    public State calculateDirection() {
        State voidCheck = super.calculateDirection();
        if (voidCheck != State.NONE) {
            return voidCheck;
        }

        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        float yaw;
        if (MacroHandler.getInstance().getCurrentMacro().isPresent() && MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().isPresent()) {
            yaw = MacroHandler.getInstance().getCurrentMacro().get().getClosest90Deg().get();
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
        LogUtils.sendDebug("Couldn't find a direction! Length > 180. Defaulting to RIGHT...");
        return State.RIGHT;
    }

    public enum ChangeLaneDirection {
        FORWARD,
        BACKWARD
    }
}
