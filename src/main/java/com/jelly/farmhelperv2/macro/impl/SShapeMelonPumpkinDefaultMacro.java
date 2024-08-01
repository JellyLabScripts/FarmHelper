package com.jelly.farmhelperv2.macro.impl;

import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.feature.impl.AntiStuck;
import com.jelly.farmhelperv2.handler.GameStateHandler;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.macro.AbstractMacro;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.BlockUtils;
import com.jelly.farmhelperv2.util.KeyBindUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Optional;

import static com.jelly.farmhelperv2.util.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelperv2.util.BlockUtils.getRelativeBlockPos;

public class SShapeMelonPumpkinDefaultMacro extends AbstractMacro {
    private final int ROTATION_DEGREE = 45;
    public ChangeLaneDirection changeLaneDirection = null;

    @Override
    public void onEnable() {
        super.onEnable();
        if (!isPitchSet())
            setPitch(50 + (float) (Math.random() * 6 - 3)); // -3 - 3
        if (!isYawSet()) {
            setYaw(AngleUtils.getClosestDiagonal());
            setClosest90Deg(Optional.of(AngleUtils.getClosest(getYaw())));
        }
        changeState(calculateDirection());
        float additionalRotation;
        switch (getCurrentState()) {
            case LEFT:
                additionalRotation = (float) (-ROTATION_DEGREE - (Math.random() * 2));
                break;
            case RIGHT:
                additionalRotation = (float) (ROTATION_DEGREE + (Math.random() * 2));
                break;
            default:
                additionalRotation = (float) (Math.random() * 2 - 1);
                break;
        }
        changeLaneDirection = null;
        if (MacroHandler.getInstance().isTeleporting()) return;
        setRestoredState(false);
        if (FarmHelperConfig.dontFixAfterWarping && Math.abs(getYaw() - AngleUtils.get360RotationYaw()) < 0.1) return;
        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation((getClosest90Deg().orElse(AngleUtils.getClosest())) + additionalRotation, getPitch()),
                        FarmHelperConfig.getRandomRotationTime(),
                        null
                )
        );
    }

    private boolean isHuggingBackWall() {
        return !GameStateHandler.getInstance().isBackWalkable();
    }

    @Override
    public void doAfterRewarpRotation() {
        if (!getRotation().isRotating()) {
            setPitch(50 + (float) (Math.random() * 6 - 3)); // -1 - 1
            if (getCurrentState() == State.RIGHT) {
                getRotation().easeTo(
                        new RotationConfiguration(
                                new Rotation((float) (getClosest90Deg().orElse(AngleUtils.getClosest()) + (ROTATION_DEGREE + (Math.random() * 2))), getPitch()),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ).easeOutBack(true)
                );
            } else if (getCurrentState() == State.LEFT) {
                getRotation().easeTo(
                        new RotationConfiguration(
                                new Rotation((float) (getClosest90Deg().orElse(AngleUtils.getClosest()) - (ROTATION_DEGREE + (Math.random() * 2))), getPitch()),
                                FarmHelperConfig.getRandomRotationTime(),
                                null
                        ).easeOutBack(true)
                );
            }
        }
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null) {
            changeState(State.NONE);
        }
        switch (getCurrentState()) {
            case RIGHT:
            case LEFT:
                Block blockLeft = BlockUtils.getBlock(getRelativeBlockPos(-1, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest())));
                Block blockRight = BlockUtils.getBlock(getRelativeBlockPos(1, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest())));
                if (blockLeft.equals(Blocks.melon_block) || blockLeft.equals(Blocks.pumpkin)) {
                    changeState(State.LEFT);
                } else if (blockRight.equals(Blocks.melon_block) || blockRight.equals(Blocks.pumpkin)) {
                    changeState(State.RIGHT);
                } else if (GameStateHandler.getInstance().isFrontWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.BACKWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeLaneDirection = ChangeLaneDirection.FORWARD;
                    setPitch(50 + (float) (Math.random() * 6 - 3)); // -3 - 3
                    float additionalRotation = 0;
                    if (getCurrentState() == State.RIGHT) {
                        additionalRotation = -((float) (Math.random() * 0.4 + 0.2));
                    } else if (getCurrentState() == State.LEFT) {
                        additionalRotation = ((float) (Math.random() * 0.4 + 0.2));
                    }
                    changeState(State.SWITCHING_LANE);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation(getClosest90Deg().orElse(AngleUtils.getClosest()) + additionalRotation, getPitch()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            )
                    );
                } else if (GameStateHandler.getInstance().isBackWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.FORWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeLaneDirection = ChangeLaneDirection.BACKWARD;
                    setPitch(50 + (float) (Math.random() * 6 - 3)); // -3 - 3
                    float additionalRotation = 0;
                    if (getCurrentState() == State.RIGHT) {
                        additionalRotation = -((float) (Math.random() * 0.4 + 0.2));
                    } else if (getCurrentState() == State.LEFT) {
                        additionalRotation = ((float) (Math.random() * 0.4 + 0.2));
                    }
                    changeState(State.SWITCHING_LANE);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation(getClosest90Deg().orElse(AngleUtils.getClosest()) + additionalRotation, getPitch()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            )
                    );
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
            case SWITCHING_LANE:
                if (GameStateHandler.getInstance().isFrontWalkable() && changeLaneDirection == ChangeLaneDirection.FORWARD && (GameStateHandler.getInstance().isRightWalkable() || GameStateHandler.getInstance().isLeftWalkable())) {
                    AntiStuck.getInstance().start();
                    changeState(State.SWITCHING_LANE);
                } else if (GameStateHandler.getInstance().isBackWalkable() && changeLaneDirection == ChangeLaneDirection.BACKWARD && (GameStateHandler.getInstance().isRightWalkable() || GameStateHandler.getInstance().isLeftWalkable())) {
                    AntiStuck.getInstance().start();
                    changeState(State.SWITCHING_LANE);
                } else if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                    setPitch(50 + (float) (Math.random() * 6 - 3)); // -3 - 3
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg().orElse(AngleUtils.getClosest()) + (ROTATION_DEGREE + (Math.random() * 2))), getPitch()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            )
                    );
                } else if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                    setPitch(50 + (float) (Math.random() * 6 - 3)); // -3 - 3
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg().orElse(AngleUtils.getClosest()) - (ROTATION_DEGREE + (Math.random() * 2))), getPitch()),
                                    FarmHelperConfig.getRandomRotationTime(),
                                    null
                            )
                    );
                } else if (GameStateHandler.getInstance().isFrontWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.BACKWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                } else if (GameStateHandler.getInstance().isBackWalkable()) {
                    if (changeLaneDirection == ChangeLaneDirection.FORWARD) {
                        // Probably stuck in dirt
                        AntiStuck.getInstance().start();
                        return;
                    }
                    changeState(State.SWITCHING_LANE);
                } else {
                    changeState(State.NONE);
                    LogUtils.sendDebug("This shouldn't happen, but it did...");
                    LogUtils.sendDebug("Can't go forward or backward!");
                }
                break;
            case DROPPING: {
                LogUtils.sendDebug("On Ground: " + mc.thePlayer.onGround);
                if (mc.thePlayer.onGround && Math.abs(getLayerY() - mc.thePlayer.getPosition().getY()) > 1.5) {
                    changeLaneDirection = null;
                    if (FarmHelperConfig.rotateAfterDrop && !getRotation().isRotating()) {
                        LogUtils.sendDebug("Rotating 180");
                        getRotation().reset();
                        setYaw(AngleUtils.getClosest(getYaw() + 180));
                        setClosest90Deg(Optional.of(AngleUtils.getClosest(getYaw())));
                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation(getYaw(), getPitch()),
                                        FarmHelperConfig.getRandomRotationTime(),
                                        null
                                )
                        );
                    }
                    KeyBindUtils.stopMovement();
                    setLayerY(mc.thePlayer.getPosition().getY());
                    changeState(State.NONE);
                } else {
                    if (mc.thePlayer.onGround) {
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

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case RIGHT:
                KeyBindUtils.holdThese(
                        isHuggingBackWall() ? mc.gameSettings.keyBindForward : null,
                        mc.gameSettings.keyBindRight,
                        mc.gameSettings.keyBindAttack
                );
                break;
            case LEFT: {
                KeyBindUtils.holdThese(
                        isHuggingBackWall() ? mc.gameSettings.keyBindForward : null,
                        mc.gameSettings.keyBindLeft,
                        mc.gameSettings.keyBindAttack
                );
                break;
            }
            case SWITCHING_LANE:
                double velocity = Math.abs(mc.thePlayer.motionX) + Math.abs(mc.thePlayer.motionZ);
                if (velocity < 0.15 && !GameStateHandler.getInstance().isFrontWalkable()) {
                    KeyBindUtils.stopMovement(FarmHelperConfig.holdLeftClickWhenChangingRow);
                    break;
                }
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindForward,
                        mc.gameSettings.keyBindSprint
                );
                GameStateHandler.getInstance().scheduleNotMoving(25);
                break;
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

    @Override
    public void actionAfterTeleport() {
        changeLaneDirection = null;
    }

    @Override
    public State calculateDirection() {
        for (int i = 0; i < 180; i++) {
            Block blockRight = getRelativeBlock(i, 0, 0);
            Block blockLeft = getRelativeBlock(-i, 0, 0);
            if (blockRight.equals(Blocks.pumpkin) || blockRight.equals(Blocks.melon_block)) {
                return State.RIGHT;
            }
            if (blockLeft.equals(Blocks.pumpkin) || blockLeft.equals(Blocks.melon_block)) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(i, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest())))) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(-i, 0, 0, getClosest90Deg().orElse(AngleUtils.getClosest())))) {
                return State.RIGHT;
            }
        }
        LogUtils.sendDebug("No direction found");
        return State.NONE;
    }

    public enum ChangeLaneDirection {
        FORWARD,
        BACKWARD
    }
}
