package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.FarmEnum;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.config.interfaces.MiscConfig;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.BlockUtils.isWalkable;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

import com.jelly.farmhelper.world.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;

public class CropMacro extends Macro {
    private final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        DROPPING,
        TP_PAD,
        SWITCH_START,
        SWITCH_MID,
        SWITCH_END,
        RIGHT,
        LEFT,
        STONE_THROW,
        NONE
    }

    enum StoneThrowState {
        ROTATE_AWAY,
        LIFT,
        SWITCH,
        DROP,
        ROTATE_BACK,
        NONE
    }

    private final Rotation rotation = new Rotation();
    private State currentState;
    private StoneThrowState stoneState;
    private double layerY;
    private boolean pushedSide;
    private boolean pushedFront;
    private boolean antistuckActive;
    private boolean tpFlag;
    private float yaw;
    private float pitch;
    private final Clock rotateWait = new Clock();
    private final Clock stoneDropTimer = new Clock();
    private int hoeSlot;


    @Override
    public void onEnable() {
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        pushedFront = false;
        pushedSide = false;
        antistuckActive = false;
        Antistuck.stuck = false;
        tpFlag = false;
        Antistuck.cooldown.schedule(1000);
        if (FarmConfig.cropType == CropEnum.NETHERWART) {
            pitch = 0f;
        } else {
            pitch = 2.8f;
        }
        yaw = AngleUtils.getClosest();
        rotation.easeTo(yaw, pitch, 500);
    }

    @Override
    public void onDisable() {
        updateKeys(false, false, false, false, false);
    }

    @Override
    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @Override
    public void onTick() {
        if (gameState.currentLocation != GameState.location.ISLAND) {
            updateKeys(false, false, false, false, false);
            enabled = false;
            return;
        }

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }

        if (currentState != State.DROPPING && currentState != State.TP_PAD && currentState != State.STONE_THROW && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > 5 || Math.abs(mc.thePlayer.rotationPitch - pitch) > 5)) {
            LogUtils.debugFullLog("Staff rotate");
            rotation.reset();
            if (rotateWait.passed() && rotateWait.isScheduled()) {
                LogUtils.webhookLog("Rotate Checked");
                LogUtils.debugLog("Rotate Checked");
                rotation.easeTo(yaw, pitch, 2000);
                rotateWait.reset();
                updateKeys(false, false, false, false, false);
                return;
            } else if (rotateWait.passed()) {
                rotateWait.schedule(500);
            }
        }

        if (Antistuck.stuck && currentState != State.STONE_THROW) {
            LogUtils.debugFullLog("Antistuck");
            if (!antistuckActive) {
                LogUtils.webhookLog("Stuck, trying to fix");
                LogUtils.debugLog("Stuck, trying to fix");
                antistuckActive = true;
                switch (currentState) {
                    case RIGHT:
                    case LEFT:
                        new Thread(fixRowStuck).start();
                        break;
                    case SWITCH_START:
                    case SWITCH_MID:
                        new Thread(fixSwitchStuck).start();
                        break;
                    case SWITCH_END:
                        new Thread(fixCornerStuck).start();
                }
            }
            return;
        }

        updateState();

        switch (currentState) {
            case TP_PAD:
                if (mc.thePlayer.posY - layerY > 1 || tpFlag) {
                    if (mc.gameSettings.keyBindRight.isKeyDown()) {
                        if (mc.thePlayer.posY % 1 == 0) tpFlag = true;
                        LogUtils.debugFullLog("On top of pad, keep going right");
                        updateKeys(false, false, true, false, true);
                    } else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                        if (mc.thePlayer.posY % 1 == 0) tpFlag = true;
                        LogUtils.debugFullLog("On top of pad, keep going left");
                        updateKeys(false, false, false, true, true);
                    } else if (BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) {
                        if (mc.thePlayer.posY % 1 == 0.8125) {
                            if (!rotation.completed) {
                                if (!rotation.rotating) {
                                    LogUtils.debugLog("Fixing pitch");
                                    rotation.reset();
                                    yaw = AngleUtils.get360RotationYaw();
                                    rotation.easeTo(yaw, pitch, 500);
                                }
                                LogUtils.debugFullLog("Waiting fix pitch");
                                updateKeys(false, false, false, false, false);
                            } else if (isWalkable(getRelativeBlock(1, 0.1875f, 0))) {
                                LogUtils.debugLog("On top of pad, go right");
                                updateKeys(false, false, true, false, true);
                            } else if (isWalkable(getRelativeBlock(-1, 0.1875f, 0))) {
                                LogUtils.debugLog("On top of pad, go left");
                                updateKeys(false, false, false, true, true);
                            } else {
                                LogUtils.debugLog("On top of pad, cant detect where to go");
                                updateKeys(false, false, false, false, false);
                            }
                        } else if (mc.thePlayer.posY % 1 < 0.8125) {
                            LogUtils.debugLog("Stuck in teleport pad, holding space");
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
                    LogUtils.debugLog("Waiting for teleport, at pad");
                    updateKeys(false, false, false, false, false);
                }
                return;
            case DROPPING:
                if (layerY - mc.thePlayer.posY > 1) {
                    if (!rotation.completed && FarmConfig.farmType == FarmEnum.LAYERED) {
                        if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating 180");
                            rotation.reset();
                            yaw = AngleUtils.get360RotationYaw(yaw + 180);
                            rotation.easeTo(yaw, pitch, 2000);
                        }
                        LogUtils.debugFullLog("Waiting Rotating 180");
                        updateKeys(false, false, false, false, false);
                    } else {
                        if (mc.thePlayer.posY % 1 == 0) {
                            LogUtils.debugLog("Dropped, resuming");
                            layerY = mc.thePlayer.posY;
                            rotation.reset();
                            currentState = State.NONE;
                            mc.thePlayer.sendChatMessage("/setspawn");
                        } else {
                            LogUtils.debugFullLog("Falling");
                        }
                        updateKeys(false, false, false, false, false);
                    }
                } else {
                    if (BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(0, 1, 1).equals(Blocks.air)) {
                        updateKeys(true, false, false, false, false);
                    } else if (BlockUtils.getRelativeBlock(1, -1, 0).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.air)
                            && BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.air)) {
                        updateKeys(false, false, true, false, true);
                    } else if
                    (BlockUtils.getRelativeBlock(-1, -1, 0).equals(Blocks.air)
                                    && BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)
                                    && BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.air)) {
                        updateKeys(false, false, false, true, true);
                    }
                    LogUtils.debugLog("Not at drop yet, keep going");
                }

                return;
            case RIGHT:
                LogUtils.debugLog("Middle of row, going right");
                updateKeys(false, false, true, false, true);
                return;
            case LEFT:
                LogUtils.debugLog("Middle of row, going left");
                updateKeys(false, false, false, true, true);
                return;
            case SWITCH_START:
                if (mc.gameSettings.keyBindForward.isKeyDown()) {
                    LogUtils.debugFullLog("Continue forwards");
                    updateKeys(true, false, false, false, false);
                } else if (pushedSide) {
                    if (gameState.dx < 0.01 && gameState.dz < 0.01) {
                        LogUtils.debugLog("Stopped, go forwards");
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
                LogUtils.debugLog("Middle of switch, keep going forwards");
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
                        if (MiscConfig.dropStone && InventoryUtils.getSlotForItem("Stone") != -1) {
                            currentState = State.STONE_THROW;
                            stoneState = StoneThrowState.ROTATE_AWAY;
                            rotation.reset();
                            LogUtils.debugLog("Found stone, switching state");
                            updateKeys(false, false, true, false, true);
                        } else if (gameState.rightWalkable) {
                            LogUtils.debugLog("Stopped, go right");
                            updateKeys(false, false, true, false, true);
                        } else {
                            LogUtils.debugLog("Stopped, go left");
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
            case STONE_THROW:
                updateKeys(false, false, false, false, false);
                int stoneSlot = InventoryUtils.getSlotForItem("Stone");
                switch (stoneState) {
                    case ROTATE_AWAY:
                        if (rotation.completed) {
                            stoneState = StoneThrowState.LIFT;
                            rotation.reset();
                            return;
                        } else if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating away");
                            hoeSlot = mc.thePlayer.inventory.currentItem;
                            mc.thePlayer.inventory.currentItem = 6;
                            if (gameState.rightWalkable) {
                                rotation.easeTo(yaw - 90, pitch, 1000);
                            } else {
                                rotation.easeTo(yaw + 90, pitch, 1000);
                            }
                        }
                        return;
                    case LIFT:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Lifting");
                            InventoryUtils.clickOpenContainerSlot(stoneSlot);
                            InventoryUtils.clickOpenContainerSlot(stoneSlot, 0, 6);
                            stoneDropTimer.schedule(200);
                        } else if (stoneDropTimer.passed()) {
                            stoneState = StoneThrowState.SWITCH;
                            stoneDropTimer.reset();
                        }
                        return;
                    case SWITCH:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Switching");
                            stoneDropTimer.schedule(1000);
                            InventoryUtils.clickOpenContainerSlot(35 + 7, 0, 0);
                        } else if (stoneDropTimer.passed()) {
                            stoneState = StoneThrowState.DROP;
                            stoneDropTimer.reset();
                        }
                        return;
                    case DROP:
                        if (!stoneDropTimer.isScheduled()) {
                            LogUtils.debugLog("Dropping");
                            mc.thePlayer.dropOneItem(true);
                            stoneDropTimer.schedule(1000);
                        } else if (stoneDropTimer.passed()) {
                            rotation.reset();
                            stoneState = StoneThrowState.ROTATE_BACK;
                            stoneDropTimer.reset();
                        }
                        return;
                    case ROTATE_BACK:
                        if (rotation.completed) {
                            currentState = State.SWITCH_END;
                            rotation.reset();
                            Antistuck.stuck = false;
                            Antistuck.cooldown.schedule(2000);
                            return;
                        } else if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating back");
                            mc.thePlayer.inventory.currentItem = hoeSlot;
                            rotation.easeTo(yaw, pitch, 1000);
                        }
                        return;
                }
            case NONE:
                LogUtils.debugFullLog("idk");
        }
    }


    private void updateState() {
        State lastState = currentState;
        if (currentState == State.STONE_THROW) {
            currentState = State.STONE_THROW;
        } else if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) || (currentState == State.TP_PAD && !tpFlag)) {
            currentState = State.TP_PAD;
        } else if (layerY - mc.thePlayer.posY > 1
                || currentState == State.DROPPING
                || (BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 1).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(1, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(1, 1, 0).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(-1, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(-1, 1, 0).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 0).equals(Blocks.air))) {
            currentState = State.DROPPING;
        } else if (gameState.leftWalkable && gameState.rightWalkable) {
            // layerY = mc.thePlayer.posY;
            if (currentState != State.RIGHT && currentState != State.LEFT) {
                mc.thePlayer.sendChatMessage("/setspawn");
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

        if (lastState == State.TP_PAD && lastState != currentState) {
            layerY = mc.thePlayer.posY;
        }

        if (lastState != currentState) {
            rotation.reset();
            pushedFront = false;
            pushedSide = false;
            tpFlag = false;
        }
    }

    private State calculateDirection() {
        for (int i = 1; i < 180; i++) {
            if (!isWalkable(BlockUtils.getRelativeBlock(i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(i - 1, 0, 1)) || isWalkable(BlockUtils.getRelativeBlock(i - 1, -1, 0))) {
                    return State.RIGHT;
                } else {
                    LogUtils.debugFullLog("Failed right: " + BlockUtils.getRelativeBlock(i - 1, 0, 1));
                    return State.LEFT;
                }
            } else if (!isWalkable(BlockUtils.getRelativeBlock(-i, 0, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-i + 1, 0, 1)) || isWalkable(BlockUtils.getRelativeBlock(-i + 1, -1, 0))) {
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

    public Runnable fixRowStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            updateKeys(true, false, false, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            antistuckActive = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public Runnable fixSwitchStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, false, false, true, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, true, false, false);
            Thread.sleep(500);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            antistuckActive = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    public Runnable fixCornerStuck = () -> {
        try {
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            if (gameState.rightWalkable) {
                updateKeys(false, false, true, false, false);
            } else {
                updateKeys(false, false, false, true, false);
            }
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            Thread.sleep(200);
            antistuckActive = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

}