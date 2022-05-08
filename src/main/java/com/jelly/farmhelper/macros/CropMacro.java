package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.features.Resync;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.AngleUtils;
import com.jelly.farmhelper.utils.BlockUtils;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.macros.MacroHandler.resync;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.BlockUtils.isWalkable;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

import com.jelly.farmhelper.utils.LogUtils;
import jline.internal.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CropMacro implements Macro {
    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean enabled = false;
    private final Rotation rotation = new Rotation();

    enum State {
        DROPPING,
        TP_PAD,
        SWITCH_START,
        SWITCH_MID,
        SWITCH_END,
        RIGHT,
        LEFT,
        NONE
    }

    private State currentState;
    private double layerY;
    private boolean pushedSide;
    private boolean pushedFront;
    private float pitch;

    @Override
    public void enable() {
        enabled = true;
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        if (FarmConfig.cropType == CropEnum.NETHERWART) {
            pitch = 0f;
        } else {
            pitch = 2.8f;
        }
        rotation.easeTo(AngleUtils.getClosest(), pitch, 500);
        resync.enable();
        LogUtils.scriptLog("Starting script");
    }

    @Override
    public void disable() {
        enabled = false;
        updateKeys(false, false, false, false, false);
        resync.disable();
        LogUtils.scriptLog("Stopping script");
    }

    @SubscribeEvent
    public final void tick(TickEvent.ClientTickEvent event) {
        if (!enabled || event.phase == TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }

        updateState();
        switch (currentState) {
            case TP_PAD:
                if (mc.thePlayer.posY - layerY > 1) {
                    if (mc.gameSettings.keyBindRight.isKeyDown()) {
                        LogUtils.debugFullLog("On top of pad, keep going right");
                        updateKeys(false, false, true, false, true);
                    } else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                        LogUtils.debugFullLog("On top of pad, keep going left");
                        updateKeys(false, false, false, true, true);
                    } else if (BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) {
                        if (mc.thePlayer.posY % 1 == 0.8125) {
                            if (!rotation.completed) {
                                if (!rotation.rotating) {
                                    LogUtils.debugFullLog("Fixing pitch");
                                    rotation.reset();
                                    rotation.easeTo(mc.thePlayer.rotationYaw, pitch, 2000);
                                }
                                LogUtils.debugFullLog("Waiting fix pitch");
                                updateKeys(false, false, false, false, false);
                            } else if (isWalkable(getRelativeBlock(1, 0.1875f, 0))) {
                                LogUtils.debugFullLog("On top of pad, go right" );
                                updateKeys(false, false, true, false, true);
                            } else if (isWalkable(getRelativeBlock(-1, 0.1875f, 0))) {
                                LogUtils.debugFullLog("On top of pad, go left");
                                updateKeys(false, false, false, true, true);
                            } else {
                                LogUtils.debugFullLog("On top of pad, cant detect where to go");
                                updateKeys(false, false, false, false, false);
                            }
                        } else if (mc.thePlayer.posY % 1 < 0.8125) {
                            LogUtils.debugFullLog("Stuck in teleport pad, holding space");
                            updateKeys(false, false, false, false, false, false, true);
                        } else {
                            LogUtils.debugFullLog("Waiting for teleport land (close)");
                            updateKeys(false, false, false, false, false);
                        }
                    } else {
                        LogUtils.debugFullLog("Waiting for teleport land");
                        updateKeys(false, false, false, false, false);
                    }
                } else {
                    LogUtils.debugFullLog("Waiting for teleport, at pad");
                    updateKeys(false, false, false, false, false);
                }
                return;
            case DROPPING:
                if (layerY - mc.thePlayer.posY > 1) {
                    if (!rotation.completed) {
                        if (!rotation.rotating) {
                            LogUtils.debugFullLog("Rotating 180");
                            rotation.reset();
                            rotation.easeTo(mc.thePlayer.rotationYaw + 180, pitch, 2000);
                        }
                        LogUtils.debugFullLog("Waiting Rotating 180");
                        updateKeys(false, false, false, false, false);
                    } else {
                        if (mc.thePlayer.posY % 1 == 0) {
                            LogUtils.debugFullLog("Dropped, resuming");
                            layerY = mc.thePlayer.posY;
                            rotation.reset();
                            currentState = State.NONE;
                        } else {
                            LogUtils.debugFullLog("Falling");
                        }
                        updateKeys(false, false, false, false, false);
                    }
                } else {
                    LogUtils.debugFullLog("Not at drop yet, keep going");
                    updateKeys(true, false, false, false, false);
                }

                return;
            case RIGHT:
                LogUtils.debugFullLog("Middle of row, going right");
                updateKeys(false, false, true, false, true);
                return;
            case LEFT:
                LogUtils.debugFullLog("Middle of row, going left");
                updateKeys(false, false, false, true, true);
                return;
            case SWITCH_START:
                if (mc.gameSettings.keyBindForward.isKeyDown()) {
                    LogUtils.debugFullLog("Continue forwards");
                    updateKeys(true, false, false, false, false);
                } else if (pushedSide) {
                    if (gameState.dx < 0.01 && gameState.dz < 0.01) {
                        LogUtils.debugFullLog("Stopped, go forwards");
                        updateKeys(true, false, false, false, false);
                    } else {
                        LogUtils.debugFullLog("Pushed, waiting till stopped");
                        updateKeys(false, false, false, false, false, true, false);
                    }
                } else {
                    if (gameState.dx < 0.01 && gameState.dz < 0.01) {
                        if (gameState.leftWalkable) {
                            LogUtils.debugFullLog("Stopped, edge left");
                            updateKeys(false, false, false, true, false, true, false);
                        } else {
                            LogUtils.debugFullLog("Stopped, edge right");
                            updateKeys(false, false, true, false, false, true, false);
                        }
                        pushedSide = true;
                    } else {
                        if (gameState.leftWalkable) {
                            LogUtils.debugFullLog("Going to edge, right");
                            updateKeys(false, false, true, false, true);
                        } else {
                            LogUtils.debugFullLog("Going to edge, left");
                            updateKeys(false, false, false, true, true);
                        }
                    }
                }
                return;
            case SWITCH_MID:
                LogUtils.debugFullLog("Middle of switch, keep going forwards");
                updateKeys(true, false, false, false, false);
                return;
            case SWITCH_END:
                if (mc.gameSettings.keyBindRight.isKeyDown()) {
                    LogUtils.debugFullLog("Continue going right");
                    updateKeys(false, false, true, false, true);
                } else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                    LogUtils.debugFullLog("Continue going left");
                    updateKeys(false, false, false, true, true);
                } else if (pushedFront) {
                    if (gameState.dx < 0.01 && gameState.dz < 0.01) {
                        if (gameState.rightWalkable) {
                            LogUtils.debugFullLog("Stopped, go right");
                            updateKeys(false, false, true, false, true);
                        } else {
                            LogUtils.debugFullLog("Stopped, go left");
                            updateKeys(false, false, false, true, true);
                        }
                    } else {
                        LogUtils.debugFullLog("Pushed, waiting till stopped");
                        updateKeys(false, false, false, false, false, true, false);
                    }
                } else {
                    if (gameState.dx < 0.01 && gameState.dz < 0.01) {
                        LogUtils.debugFullLog("Stopped, edge backwards");
                        updateKeys(false, true, false, false, false, true, false);
                        pushedFront = true;
                    } else {
                        LogUtils.debugFullLog("Going to lane, forwards");
                        updateKeys(true, false, false, false, false);
                    }
                }
                return;
            case NONE:
                LogUtils.debugFullLog("idk");
        }
    }

    @SubscribeEvent
    public void onRenderWorld(final RenderWorldLastEvent event) {
        if (enabled && rotation.rotating) {
            LogUtils.debugFullLog("onRenderWorld - Rotating");
            rotation.update();
        }
    }

    private void updateState() {
        LogUtils.debugFullLog("updateState");
        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) || currentState == State.TP_PAD) {
            currentState = State.TP_PAD;
        } else if (layerY - mc.thePlayer.posY > 1 || BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air) || currentState == State.DROPPING) {
            currentState = State.DROPPING;
        } else if (gameState.leftWalkable && gameState.rightWalkable) {
            layerY = mc.thePlayer.posY;
            if (currentState != State.RIGHT && currentState != State.LEFT) {
                currentState = calculateDirection();
            }
        } else if (gameState.frontWalkable && !gameState.backWalkable) {
            currentState = State.SWITCH_START;
        } else if (gameState.frontWalkable) {
            currentState = State.SWITCH_MID;
        } else if (gameState.backWalkable) {
            currentState = State.SWITCH_END;
        } else if (gameState.leftWalkable) {
            currentState = State.LEFT;
        } else if (gameState.rightWalkable) {
            currentState = State.RIGHT;
        } else {
            currentState = State.NONE;
        }

        if (currentState != State.SWITCH_START) {
            pushedSide = false;
        }
        if (currentState != State.SWITCH_END) {
            pushedFront = false;
        }
        if (currentState != State.DROPPING && currentState != State.TP_PAD) {
            rotation.reset();
        }
    }

    private State calculateDirection() {
        for (int i = 1; i < 180; i++) {
            if (!isWalkable(BlockUtils.getRelativeBlock(i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1))) {
                    return State.RIGHT;
                } else {
                    LogUtils.debugFullLog("Failed right: " + BlockUtils.getRelativeBlock(i - 1, 0, 1));
                    return State.LEFT;
                }
            } else if (!isWalkable(BlockUtils.getRelativeBlock(-i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-i + 1, 0, 1))) {
                    return State.LEFT;
                } else {
                    LogUtils.debugFullLog("Failed left: " + isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1)));
                    return State.RIGHT;
                }
            }
        }
        LogUtils.debugLog("Cannot find direction. Length > 180");
        return State.NONE;
    }
}
