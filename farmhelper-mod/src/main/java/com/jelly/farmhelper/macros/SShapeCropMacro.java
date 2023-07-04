package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.VerticalMacroEnum;
import com.jelly.farmhelper.config.Config.SMacroEnum;
import com.jelly.farmhelper.config.Config.CropEnum;

import com.jelly.farmhelper.events.ReceivePacketEvent;
import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;

import static com.jelly.farmhelper.FarmHelper.gameState;
import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

//TODO: Add drop rotation detection
public class SShapeCropMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();

    enum State {
        DROPPING,
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
    public State currentState;
    public State prevState;
    private StoneThrowState stoneState;
    private double layerY;
    private boolean antistuckActive;
    private float yaw;
    private float pitch;
    private final Clock stoneDropTimer = new Clock();
    private int hoeSlot;
    private int notmovingticks = 0;

    private final Clock lastTp = new Clock();
    private boolean isTping = false;
    private final Clock waitForChangeDirection = new Clock();

    private boolean rotated = false;

    private boolean switchBackwardsDirection = false;

    @Override
    public void onEnable() {
        layerY = mc.thePlayer.posY;
        currentState = State.NONE;
        antistuckActive = false;
        Antistuck.stuck = false;
        prevState = null;
        Antistuck.cooldown.schedule(1000);
        CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;
        CropUtils.getTool();
        if (crop == CropEnum.NETHER_WART || crop == CropEnum.CACTUS) {
            pitch = (float) (0f + Math.random() * 0.5f);
        } else if (crop == CropEnum.MELON || crop == CropEnum.PUMPKIN) {
            pitch = 28 + (float) (Math.random() * 2); //28-30
        } else {
            pitch = (float) (2.8f + Math.random() * 0.5f);
        }
        yaw = AngleUtils.getClosest();
        lastTp.reset();
        waitForChangeDirection.reset();
        rotation.easeTo(yaw, pitch, 500);
        isTping = false;
    }

    @Override
    public void onDisable() {
        updateKeys(false, false, false, false, false);
    }

    private State stateBeforeFailsafe = null;

    @Override
    public void saveLastStateBeforeDisable() {
        stateBeforeFailsafe = currentState;
        super.saveLastStateBeforeDisable();
    }

    @Override
    public void restoreState() {
        currentState = stateBeforeFailsafe;
        super.restoreState();
    }

    @Override
    public void onLastRender() {
        if (rotation.rotating) {
            rotation.update();
        }
    }

    @Override
    public void triggerTpCooldown() {
        lastTp.schedule(1500);
        if (currentState == State.DROPPING) {
            currentState = calculateDirection();
        }
    }

    @Override
    public void onPacketReceived(ReceivePacketEvent event) {
        if(rotation.rotating && event.packet instanceof S08PacketPlayerPosLook &&
                (currentState == State.DROPPING || (!isTping && (!lastTp.isScheduled() || lastTp.passed())))) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
        }
    }

    private BlockPos beforeTeleportationPos = null;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 1) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.schedule(1_000);
            layerY = mc.thePlayer.posY;
            currentState = State.NONE;
            waitForChangeDirection.reset();
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
    }

    @Override
    public void onTick() {

        if(mc.thePlayer == null || mc.theWorld == null)
            return;

        checkForTeleport();

        if (isTping) return;

        if(rotation.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && !rotated) {
            yaw = AngleUtils.getClosest(yaw);
            if (FarmHelper.config.rotateAfterWarped)
                yaw = AngleUtils.get360RotationYaw(yaw + 180);
            if (mc.thePlayer.rotationPitch != pitch || mc.thePlayer.rotationYaw != yaw) {
                rotation.easeTo(yaw, pitch, (long) (500 + Math.random() * 200));
                rotated = true;
            }
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (lastTp.isScheduled() && lastTp.passed()) {
            lastTp.reset();
            currentState = calculateDirection();
            rotated = false;
        }

        LogUtils.debugLog("Current state: " + currentState);

        if(currentState != State.DROPPING && currentState != State.STONE_THROW) {

            boolean flag = AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FarmHelper.config.rotationCheckSensitivity
                    || Math.abs(mc.thePlayer.rotationPitch - pitch) > FarmHelper.config.rotationCheckSensitivity;

            if(!Failsafe.emergency && flag && lastTp.passed() && !rotation.rotating) {
                rotation.reset();
                Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
                return;
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
            } else {
                if (notmovingticks > 500) {
                    antistuckActive = false;
                    notmovingticks = 0;
                }
                notmovingticks++;
            }
            return;
        }

        CropUtils.getTool();
        updateState();
        prevState = currentState;
        System.out.println(currentState);

        if (beforeTeleportationPos != null) {
            LogUtils.debugLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            triggerWarpGarden();
            return;
        }

        if (Failsafe.emergency) {
            LogUtils.debugLog("Blocking changing movement due to emergency");
            return;
        }

        switch (currentState) {
            case DROPPING:
                if (layerY - mc.thePlayer.posY >= 2 && mc.thePlayer.onGround && FarmHelper.config.rotateAfterDrop) {
                    if (!rotation.completed) {
                        if (!rotation.rotating) {
                            LogUtils.debugLog("Rotating 180");
                            rotation.reset();
                            yaw = AngleUtils.get360RotationYaw(yaw + 180);
                            rotation.easeTo(yaw, pitch, (long) (300 + Math.random() * 500));
                        }
                        LogUtils.debugFullLog("Waiting: Rotating 180");
                        updateKeys(false, false, false, false, false);
                    } else {
                        if (mc.thePlayer.posY % 1 == 0) {
                            LogUtils.debugLog("Dropped, resuming");
                            layerY = mc.thePlayer.posY;
                            rotation.reset();
                            currentState = State.NONE;
                            PlayerUtils.attemptSetSpawn();
                        } else {
                            LogUtils.debugFullLog("Falling");
                        }
                        updateKeys(false, false, false, false, false);
                    }
                } else if (layerY - mc.thePlayer.posY >= 2 && mc.thePlayer.onGround) {
                    if (mc.thePlayer.posY % 1 == 0) {
                        LogUtils.debugLog("Dropped, resuming");
                        layerY = mc.thePlayer.posY;
                        rotation.reset();
                        currentState = State.NONE;
                        PlayerUtils.attemptSetSpawn();
                    } else {
                        LogUtils.debugFullLog("Falling");
                    }
                    updateKeys(false, false, false, false, false);
                }

                return;
            case RIGHT:
                if (!waitForChangeDirection.isScheduled()) {
                    LogUtils.debugLog("Middle of row, going right");
                    updateKeys((FarmHelper.config.VerticalMacroType != SMacroEnum.PUMPKIN_MELON.ordinal()) &&
                                (FarmHelper.config.SShapeMacroType != SMacroEnum.CACTUS.ordinal()) && shouldWalkForwards(), (FarmHelper.config.SShapeMacroType == SMacroEnum.CACTUS.ordinal()) && shouldPushBack(), true, false, true);
                }
                return;
            case LEFT:
                if (!waitForChangeDirection.isScheduled()) {
                    LogUtils.debugLog("Middle of row, going left");
                    updateKeys((FarmHelper.config.VerticalMacroType != SMacroEnum.PUMPKIN_MELON.ordinal()) &&
                        (FarmHelper.config.SShapeMacroType != SMacroEnum.CACTUS.ordinal()) && shouldWalkForwards(), (FarmHelper.config.SShapeMacroType == SMacroEnum.CACTUS.ordinal()) && shouldPushBack(), false, true, true);
                }
                return;
            case SWITCH_START:
                if (!waitForChangeDirection.isScheduled()) {
                    if (switchBackwardsDirection) {
                        LogUtils.debugFullLog("Continue backwards");
                        updateKeys(false, true, prevState == State.RIGHT, prevState == State.LEFT, FarmHelper.config.holdLeftClickWhenChangingRow);
                        return;
                    } else {
                        LogUtils.debugFullLog("Continue forwards");
                        updateKeys(true, false, prevState == State.RIGHT, prevState == State.LEFT, FarmHelper.config.holdLeftClickWhenChangingRow);
                        return;
                    }
                }
            case SWITCH_MID:
                LogUtils.debugLog("Middle of switch, keep going");
                if (switchBackwardsDirection)
                    updateKeys(false, true, false, false, FarmHelper.config.holdLeftClickWhenChangingRow);
                else
                    updateKeys(true, false, false, false, FarmHelper.config.holdLeftClickWhenChangingRow);
                return;
            case SWITCH_END:
                if (gameState.rightWalkable) {
                    LogUtils.debugFullLog("Row switched, continue going right");
                    updateKeys(false, false, true, false, true);
                } else if (gameState.leftWalkable) {
                    LogUtils.debugFullLog("Row switched, continue going left");
                    updateKeys(false, false, false, true, true);
                }
                return;
            case STONE_THROW:
                updateKeys(false, false, false, false, false);
                int stoneSlot = PlayerUtils.getSlotForItem("Stone");
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
                            PlayerUtils.clickOpenContainerSlot(stoneSlot);
                            PlayerUtils.clickOpenContainerSlot(stoneSlot, 0, 6);
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
                            PlayerUtils.clickOpenContainerSlot(35 + 7, 0, 0);
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
                LogUtils.debugFullLog("NONE");
                if (!waitForChangeDirection.isScheduled()) {
                    LogUtils.debugFullLog("idk");
                    LogUtils.debugFullLog("currentState: " + currentState);
                    LogUtils.debugFullLog("gameState.leftWalkable: " + gameState.leftWalkable);
                    LogUtils.debugFullLog("gameState.rightWalkable: " + gameState.rightWalkable);
                    LogUtils.debugFullLog("gameState.frontWalkable: " + gameState.frontWalkable);
                    LogUtils.debugFullLog("gameState.backWalkable: " + gameState.backWalkable);
                    KeyBindUtils.stopMovement();
                }
        }
    }


    private void updateState() {
        State lastState = currentState;

        if (currentState == State.STONE_THROW) {
            currentState = State.STONE_THROW;
        } else if (!isRewarpLocationSet()) {
            LogUtils.scriptLog("Your rewarp position is not set!");
        } else if (isRewarpLocationSet() && isStandingOnRewarpLocation()) {
            triggerWarpGarden();
        } else if (!isTping && (Math.abs(layerY - mc.thePlayer.posY) > 2 || currentState == State.DROPPING || isDropping())) {
            currentState = State.DROPPING;
        } else if (gameState.leftWalkable && gameState.rightWalkable) {
            // LogUtils.debugLog("Left and right walkable");
            PlayerUtils.attemptSetSpawn();
            if (currentState != State.RIGHT && currentState != State.LEFT) {
                LogUtils.debugLog("Calculating direction");
                currentState = calculateDirection();
            }
        } else if (gameState.frontWalkable && gameState.backWalkable && (currentState == State.SWITCH_START || currentState == State.SWITCH_MID) && !waitForChangeDirection.isScheduled()) {
            currentState = State.SWITCH_MID;
            LogUtils.debugLog("SWITCH_MID");
        } else if (((gameState.frontWalkable && (!gameState.backWalkable || BlockUtils.getRelativeBlock(0, 0, -1).equals(Blocks.water))) || ((!gameState.frontWalkable || BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.water)) && gameState.backWalkable)) && currentState != State.SWITCH_MID && currentState != State.DROPPING && (FarmHelper.config.SShapeMacroType != SMacroEnum.CACTUS.ordinal() || !BlockUtils.getRelativeBlock(0, 0, 2).equals(Blocks.cactus)) && (FarmHelper.config.VerticalMacroType != SMacroEnum.PUMPKIN_MELON.ordinal() || (!BlockUtils.isRelativeBlockPassable(0, -1, 2) || !BlockUtils.isRelativeBlockPassable(0, -1, -2)))) {
            if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                if (gameState.frontWalkable)
                    switchBackwardsDirection = false;
                else if (gameState.backWalkable)
                    switchBackwardsDirection = true;
                currentState = State.SWITCH_START;
                waitForChangeDirection.reset();
                LogUtils.debugLog("SWITCH_START");
                return;
            }
            if (!waitForChangeDirection.isScheduled() && currentState != State.SWITCH_START && (gameState.dx < 0.1 && gameState.dz < 0.1)) {
                updateKeys(false, false, false, false, true);
                long waitTime = (long) (Math.random() * 300 + 150);
                LogUtils.debugLog("SWITCH_START: Waiting " + waitTime + "ms");
                waitForChangeDirection.schedule(waitTime);
            }
        } else if (((gameState.backWalkable && !gameState.frontWalkable && !switchBackwardsDirection) || (gameState.frontWalkable && !gameState.backWalkable && switchBackwardsDirection)) && currentState == State.SWITCH_MID) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    currentState = State.SWITCH_END;
                    waitForChangeDirection.reset();
                    LogUtils.debugLog("SWITCH_END");
                    return;
                }
                if (!waitForChangeDirection.isScheduled() && (gameState.dx < 0.1 && gameState.dz < 0.1)) {
                    updateKeys(false, false, false, false, true);
                    long waitTime = (long) (Math.random() * 300 + 150);
                    LogUtils.debugLog("SWITCH_END: Waiting " + waitTime + "ms");
                    waitForChangeDirection.schedule(waitTime);
                }
        } else if ((getRelativeBlock(0, -1, 0).equals(Blocks.air) || getRelativeBlock(-1, -1, 0).equals(Blocks.air) && getRelativeBlock(-1, 0, 0).equals(Blocks.air)) && gameState.rightWalkable) {
            currentState = State.DROPPING;
        } else if ((getRelativeBlock(0, -1, 0).equals(Blocks.air) || getRelativeBlock(1, -1, 0).equals(Blocks.air) && getRelativeBlock(1, 0, 0).equals(Blocks.air)) && gameState.leftWalkable) {
            currentState = State.DROPPING;
        } else if (gameState.leftWalkable && currentState != State.SWITCH_START && currentState != State.SWITCH_MID && currentState != State.SWITCH_END) {
            currentState = State.LEFT;
        } else if (gameState.rightWalkable && currentState != State.SWITCH_START && currentState != State.SWITCH_MID && currentState != State.SWITCH_END) {
            currentState = State.RIGHT;
        } else {
            currentState = State.NONE;
        }

        if (lastState != currentState) {
            waitForChangeDirection.reset();
            rotation.reset();
        }
    }

    private void triggerWarpGarden() {
        KeyBindUtils.stopMovement();
        if (waitForChangeDirection.isScheduled() && beforeTeleportationPos == null) {
            waitForChangeDirection.reset();
        }
        if (!waitForChangeDirection.isScheduled()) {
            LogUtils.debugLog("Should TP");
            long waitTime = (long) (Math.random() * 750 + 500);
            waitForChangeDirection.schedule(waitTime);
            beforeTeleportationPos = mc.thePlayer.getPosition();
        } else if (waitForChangeDirection.passed()) {
            mc.thePlayer.sendChatMessage(FarmHelper.gameState.wasInGarden ? "/warp garden" : "/is");
            isTping = true;
            waitForChangeDirection.reset();
        }
    }

    private State calculateDirection() {

        if (rightCropIsReady()) {
            return State.RIGHT;
        } else if (leftCropIsReady()) {
            return State.LEFT;
        }

        boolean insideTpPad = BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame);

        for (int i = 1; i < 180; i++) {
            int zeroY = insideTpPad ? 1 : 0;
            int minusY = insideTpPad ? 0 : -1;
            if (!isWalkable(BlockUtils.getRelativeBlock(i, zeroY, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(i - 1, zeroY, 1)) || isWalkable(BlockUtils.getRelativeBlock(i - 1, minusY, 0))) {
                    return State.RIGHT;
                } else {
                    LogUtils.debugFullLog("Failed right: " + BlockUtils.getRelativeBlock(i - 1, zeroY, 1));
                    return State.LEFT;
                }
            } else if (!isWalkable(BlockUtils.getRelativeBlock(-i, zeroY, 0))) {
                if (isWalkable(BlockUtils.getRelativeBlock(-i + 1, zeroY, 1)) || isWalkable(BlockUtils.getRelativeBlock(-i + 1, minusY, 0))) {
                    return State.LEFT;
                } else {
                    LogUtils.debugFullLog("Failed left: " + isWalkable(BlockUtils.getRelativeBlock(i - 1, zeroY, 1)));
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

    private static boolean shouldWalkForwards() {
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        System.out.println(angle);
        if (angle == 0) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 90) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        } else if (angle == 180) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 270) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        }
        return false;
    }

    private boolean shouldPushBack() {
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
        System.out.println(angle);
        Block blockBehind = BlockUtils.getRelativeBlock(0, 0, -1);
        if (!(blockBehind.getMaterial().isSolid() || (blockBehind instanceof BlockSlab) || blockBehind.equals(Blocks.carpet) || (blockBehind instanceof BlockDoor)) || blockBehind.getMaterial().isLiquid())
            return false;
        if (angle == 0) {
            return (z > -0.65 && z < -0.1) || (z < 0.9 && z > 0.35);
        } else if (angle == 90) {
            return (x > -0.9 && x < -0.35) || (x < 0.65 && x > 0.1);
        } else if (angle == 180) {
            return (z > -0.9 && z < -0.35) || (z < 0.65 && z > 0.1);
        } else if (angle == 270) {
            return (x > -0.65 && x < -0.1) || (x < 0.9 && x > 0.35);
        }
        return false;
    }

    private static boolean isDropping(){

        return  (BlockUtils.getRelativeBlock(0, -1, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 1).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 1).equals(Blocks.air))
                || (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.air)
                && BlockUtils.getRelativeBlock(0, 1, 0).equals(Blocks.air));
    }
}