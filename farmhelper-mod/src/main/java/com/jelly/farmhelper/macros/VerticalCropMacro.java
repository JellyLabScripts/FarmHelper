package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.enums.MacroEnum;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class VerticalCropMacro extends Macro{
    private static final Minecraft mc = Minecraft.getMinecraft();
    enum State {
        RIGHT,
        LEFT,
        BACK_TO_TOP_LAYER,
        NONE
    }

    static State currentState;
    static State prevState;

    float pitch;
    float yaw;

    Rotation rotation = new Rotation();

    private final Clock lastTp = new Clock();
    private boolean isTping = false;
    private final Clock waitForChangeDirection = new Clock();

    private State stateBeforeFailsafe = null;

    @Override
    public void saveLastStateBeforeDisable() {
        stateBeforeFailsafe = currentState;
        super.saveLastStateBeforeDisable();
    }

    @Override
    public void restoreState() {
        LogUtils.debugLog("Restoring state: " + stateBeforeFailsafe);
        currentState = stateBeforeFailsafe;
        super.restoreState();
    }


    @Override
    public void onEnable() {
        lastTp.reset();
        waitForChangeDirection.reset();
        yaw = AngleUtils.getClosest();
        CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;

        switch(crop){
            case SUGARCANE:
                pitch = (float) (Math.random() * 2); // 0 - 2
                break;
            case POTATO: case CARROT: case WHEAT:
                pitch = 2.8f + (float) (Math.random() * 0.6); // 2.8-3.4
                break;
            case NETHERWART:
                pitch = (float) (Math.random() * 2 - 1); // -1 - 1
                break;
            case MELON: case PUMPKIN:
                pitch = 28 + (float) (Math.random() * 2); //28-30
                break;
            case COCOA_BEANS:
                pitch = -90;
                break;
        }
        prevState = null;
        currentState = State.NONE;
        rotation.easeTo(yaw, pitch, 500);
        CropUtils.getTool();
        isTping = false;
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    private BlockPos beforeTeleportationPos = null;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 1) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.reset();
            lastTp.schedule(1_000);
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        checkForTeleport();

        if (rotation.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (isTping) return;

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && mc.thePlayer.rotationPitch != pitch) {
            yaw = AngleUtils.getClosest();
            rotation.easeTo(yaw, pitch, 500);
            changeStateTo(calculateDirection());
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            KeyBindUtils.updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            return;
        }

        if (lastTp.passed()) {
            lastTp.reset();
        }

        if (!Failsafe.emergency && !rotation.rotating && !lastTp.isScheduled() && !isTping && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FailsafeConfig.rotationSens || Math.abs(mc.thePlayer.rotationPitch - pitch) > FailsafeConfig.rotationSens)) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }

        CropUtils.getTool();

        if (BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.ladder) ||
                BlockUtils.getRelativeBlock(-1, 0, 0).equals(Blocks.ladder) ||
                        BlockUtils.getRelativeBlock(1, 0, 0).equals(Blocks.ladder) && !FarmConfig.warpBackToStart) {
            changeStateTo(State.BACK_TO_TOP_LAYER);
        }

        if (currentState == State.BACK_TO_TOP_LAYER) {
            if (!BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.ladder) &&
                BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.ladder) && FarmHelper.gameState.dy < 0.01) {
                if (BlockUtils.getRelativeBlock(-1, 0, 0).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(-1, 0, 0))) {
                    changeStateTo(State.LEFT);
                    System.out.println("Left");
                } else if (BlockUtils.getRelativeBlock(1, 0, 0).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(1, 0, 0))) {
                    changeStateTo(State.RIGHT);
                    System.out.println("Right");
                }
            }

            KeyBindUtils.updateKeys(false, false, prevState == State.RIGHT, prevState == State.LEFT, true, false, false);
        }

        if (!BlockUtils.getRelativeBlock(0, 0, 1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 0, 1)) &&
            !BlockUtils.getRelativeBlock(0, 0, -1).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(0, 0, -1)) &&
            FarmHelper.gameState.dx < 0.1 && FarmHelper.gameState.dz < 0.1 && FarmHelper.gameState.dy < 0.1 &&
            BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.wall_sign) &&
            (!BlockUtils.getRelativeBlock(1, 0, 0).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(1, 0, 0)) ||
            !BlockUtils.getRelativeBlock(-1, 0, 0).isPassable(mc.theWorld, BlockUtils.getRelativeBlockPos(-1, 0, 0))) &&
            FarmConfig.warpBackToStart) {
            KeyBindUtils.stopMovement();
            if (!waitForChangeDirection.isScheduled()) {
                long waitTime = (long) (Math.random() * 750 + 500);
                waitForChangeDirection.schedule(waitTime);
                beforeTeleportationPos = mc.thePlayer.getPosition();
            } else if (waitForChangeDirection.passed()) {
                mc.thePlayer.sendChatMessage(FarmHelper.gameState.wasInGarden ? "/warp garden" : "/is");
                isTping = true;
            }
            return;
        }

        if (isWalkable(getRightBlock()) && isWalkable(getLeftBlock()) && currentState != State.BACK_TO_TOP_LAYER) {
            if(mc.thePlayer.lastTickPosY - mc.thePlayer.posY != 0)
                return;

            PlayerUtils.attemptSetSpawn();

            if (currentState == State.NONE) {
                changeStateTo(calculateDirection());
            }
            if (currentState == State.RIGHT)
                updateKeys(((FarmConfig.cropType != MacroEnum.PUMPKIN_MELON) && shouldWalkForwards()), false, true, false, true, false, false);
            else if (currentState == State.LEFT) {
                updateKeys((FarmConfig.cropType != MacroEnum.PUMPKIN_MELON)  && shouldWalkForwards(), false, false, true, true, false, false);
            } else {
                stopMovement();
            }
        } else if (isWalkable(getRightBlock()) && isWalkable(getRightTopBlock()) &&
                (!isWalkable(getLeftBlock()) || !isWalkable(getLeftTopBlock())) && currentState != State.BACK_TO_TOP_LAYER) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    changeStateTo(State.RIGHT);
                    waitForChangeDirection.reset();
                    updateKeys(false, false, true, false, true);
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 750 + 500);
                    if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                            || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                            BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                        waitTime = 1;
                    System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        } else if (isWalkable(getLeftBlock()) && isWalkable(getLeftTopBlock()) &&
                (!isWalkable(getRightBlock()) || !isWalkable(getRightTopBlock())) && currentState != State.BACK_TO_TOP_LAYER) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    changeStateTo(State.LEFT);
                    waitForChangeDirection.reset();
                    updateKeys(false, false, false, true, true);
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 750 + 500);
                    if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                            || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                            BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                        waitTime = 1;
                    System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        }
    }

    private void changeStateTo(State newState) {
        State currState = currentState;
        currentState = newState;
        if (currState != currentState) {
            System.out.println("Changed direction to " + currentState + " from " + currState);
            prevState = currState;
            waitForChangeDirection.reset();
        }
    }

    @Override
    public void triggerTpCooldown() {
        lastTp.schedule(1500);
    }

    private static boolean shouldWalkForwards() {
        float angle = AngleUtils.getClosest();
        double x = mc.thePlayer.posX % 1;
        double z = mc.thePlayer.posZ % 1;
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

    @Override
    public void onLastRender() {
        if(rotation.rotating)
            rotation.update();
    }

    State calculateDirection() {

        boolean f1 = true, f2 = true;

        if (rightCropIsReady()) {
            return State.RIGHT;
        } else if (leftCropIsReady()) {
            return State.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, -1, 0)) && f1) {
                return State.RIGHT;
            }
            if(!isWalkable(getRelativeBlock(i, 0, 0)))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, -1, 0)) && f2) {
                return State.LEFT;
            }
            if(!isWalkable(getRelativeBlock(-i, 0, 0)))
                f2 = false;
        }
        return State.NONE;
    }
}
