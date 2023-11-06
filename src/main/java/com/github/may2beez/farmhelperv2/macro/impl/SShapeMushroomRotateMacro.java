package com.github.may2beez.farmhelperv2.macro.impl;

import cc.polyfrost.oneconfig.libs.universal.UMath;
import com.github.may2beez.farmhelperv2.config.FarmHelperConfig;
import com.github.may2beez.farmhelperv2.handler.GameStateHandler;
import com.github.may2beez.farmhelperv2.handler.MacroHandler;
import com.github.may2beez.farmhelperv2.macro.AbstractMacro;
import com.github.may2beez.farmhelperv2.util.*;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;

import static com.github.may2beez.farmhelperv2.util.BlockUtils.getRelativeBlockPos;

@SuppressWarnings("DuplicatedCode")
public class SShapeMushroomRotateMacro extends AbstractMacro {

    @Override
    public void onEnable() {
        FarmHelperConfig.CropEnum crop = PlayerUtils.getFarmingCrop();
        LogUtils.sendDebug("Crop: " + crop);
        MacroHandler.getInstance().setCrop(crop);
        PlayerUtils.getTool();
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
        setYaw(AngleUtils.getClosest());
        setClosest90Deg(AngleUtils.getClosest());

        super.onEnable();
        getRotation().easeTo(
                new RotationConfiguration(
                        new Rotation((float) (getClosest90Deg() + (getCurrentState() == State.LEFT ? -30 : 30) + (Math.random() * 4 - 2)), getPitch()),
                        (long) (400 + Math.random() * 300), null
                )
        );
    }

    @Override
    public void actionAfterTeleport() {
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
    }

    @Override
    public void doAfterRewarpRotation() {
        setClosest90Deg((float) UMath.wrapAngleTo180(AngleUtils.getClosest() + 180));
        setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
    }

    @Override
    public void updateState() {
        if (getCurrentState() == null)
            changeState(State.NONE);
        switch (getCurrentState()) {
            case LEFT:
                if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg() + 30 + (Math.random() * 4 - 2)), getPitch()),
                                    (long) (400 + Math.random() * 300), null
                            )
                    );
                } else if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg() - 30 + (Math.random() * 4 - 2)), getPitch()),
                                    (long) (400 + Math.random() * 300), null
                            )
                    );
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }

                setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
                break;
            case RIGHT:
                if (GameStateHandler.getInstance().isLeftWalkable()) {
                    changeState(State.LEFT);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg() - 30 + (Math.random() * 4 - 2)), getPitch()),
                                    (long) (400 + Math.random() * 300), null
                            )
                    );
                } else if (GameStateHandler.getInstance().isRightWalkable()) {
                    changeState(State.RIGHT);
                    getRotation().easeTo(
                            new RotationConfiguration(
                                    new Rotation((float) (getClosest90Deg() + 30 + (Math.random() * 4 - 2)), getPitch()),
                                    (long) (400 + Math.random() * 300), null
                            )
                    );
                } else {
                    LogUtils.sendDebug("No direction found");
                    changeState(calculateDirection());
                }

                setPitch((float) (Math.random() * 2 - 1)); // -1 - 1
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
                                        new Rotation((float) (getClosest90Deg() + (getCurrentState() == State.LEFT ? -30 : 30) + (Math.random() * 4 - 2)), getPitch()),
                                        (long) (400 + Math.random() * 300),
                                        null
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
                switch (getCurrentState()) {
                    case LEFT:
                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation((float) (getClosest90Deg() - 30 + (Math.random() * 4 - 2)), getPitch()),
                                        (long) (400 + Math.random() * 300), null
                                )
                        );
                        break;
                    case RIGHT:
                        getRotation().easeTo(
                                new RotationConfiguration(
                                        new Rotation((float) (getClosest90Deg() + 30 + (Math.random() * 4 - 2)), getPitch()),
                                        (long) (400 + Math.random() * 300), null
                                )
                        );
                        break;
                }
                break;
        }
    }

    @Override
    public void invokeState() {
        if (getCurrentState() == null) return;
        switch (getCurrentState()) {
            case RIGHT:
            case LEFT: {
                KeyBindUtils.holdThese(
                        mc.gameSettings.keyBindForward,
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
                break;
            }
        }
    }

    @Override
    public boolean shouldRotateAfterWarp() {
        return false;
    }

    @Override
    public State calculateDirection() {
        if (BlockUtils.rightCropIsReady()) {
            return State.RIGHT;
        } else if (BlockUtils.leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 1; i < 180; i++) {
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(i, 0, 0, getClosest90Deg()))) {
                return State.LEFT;
            }
            if (!BlockUtils.canWalkThrough(getRelativeBlockPos(-i, 0, 0, getClosest90Deg())))
                return State.RIGHT;
        }
        LogUtils.sendDebug("No direction found");
        return State.NONE;
    }
}
