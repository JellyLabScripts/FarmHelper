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

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class MushroomMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();
    enum direction {
        LEFT,
        RIGHT,
        NONE
    }

    direction dir;
    direction prevDir;

    float pitch;
    float yaw;

    Rotation rotation = new Rotation();

    private final Clock lastTp = new Clock();
    private boolean isTping = false;

    private final Clock waitForChangeDirection = new Clock();
    private final Clock waitBetweenTp = new Clock();
    private CropEnum crop;

    @Override
    public void onEnable() {
        lastTp.reset();
        waitForChangeDirection.reset();
        waitBetweenTp.reset();
        pitch = (float) (Math.random() * 2 - 1); // -1 - 1
        crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;

        if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) {
            yaw = AngleUtils.getClosest30();
        } else {
            yaw = AngleUtils.getClosestDiagonal();
        }

        prevDir = null;
        dir = direction.NONE;
        rotation.easeTo(yaw, pitch, 500);
        mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(CropEnum.MUSHROOM);
        isTping = false;
        if (getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) || getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)) {
            lastTp.schedule(1500);
            LogUtils.debugLog("Started on tp pad");
            waitBetweenTp.schedule(10000);
        }
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    @Override
    public void onChatMessageReceived(String msg) {
        super.onChatMessageReceived(msg);
        if (msg.contains("Warped from the ") && msg.contains(" to the ")) {
            lastTp.schedule(1000);
            isTping = false;
            LogUtils.debugLog("Tped");
            waitBetweenTp.schedule(10000);
        }
    }

    @Override
    public void onTick() {

        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        if (rotation.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (waitBetweenTp.isScheduled() && waitBetweenTp.passed()) {
            waitBetweenTp.reset();
        }

        if (lastTp.isScheduled() && lastTp.passed()) {
            lastTp.reset();
            if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) {
                LogUtils.debugLog("Change direction to FORWARD (tp pad)");
                dir = direction.RIGHT;
            } else {
                LogUtils.debugLog("Change direction");
                dir = calculateDirection();
            }
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) {
                LogUtils.debugLog("Going FORWARD");
                updateKeys(true, false, false, false, true);
            } else {
                LogUtils.debugLog("Going " + dir);
                updateKeys(dir == direction.RIGHT, false, false, dir == direction.LEFT, true);
            }
            return;
        }

        if (!rotation.rotating && !isTping && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FailsafeConfig.rotationSens || Math.abs(mc.thePlayer.rotationPitch - pitch) > FailsafeConfig.rotationSens)) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }

        if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)) && !isTping && (!waitBetweenTp.isScheduled() || waitBetweenTp.passed())) {//standing on tp pad
            isTping = true;
            LogUtils.debugLog("Scheduled tp");

            updateKeys(false, false, false, false, false);
            if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) return;
            if (dir == direction.RIGHT) {
                dir = direction.LEFT;
            } else {
                dir = direction.RIGHT;
            }
            return;
        }

        if (isWalkable(getRightBlock(getAngleDiff())) && isWalkable(getLeftBlock(getAngleDiff()))) {
            if(mc.thePlayer.lastTickPosY - mc.thePlayer.posY != 0)
                return;

            PlayerUtils.attemptSetSpawn();

            if (dir == direction.NONE) {
                if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD) {
                    LogUtils.debugLog("Change direction to FORWARD (tp pad)");
                    dir = direction.RIGHT;
                } else {
                    dir = calculateDirection();
                }
            }


            if (dir == direction.RIGHT) {
                LogUtils.debugLog("Going RIGHT");
                updateKeys(true, false, false, false, true);
            } else if (dir == direction.LEFT) {
                LogUtils.debugLog("Going LEFT");
                updateKeys(false, false, false, true, true);
            } else {
                LogUtils.debugLog("Error: dir == direction.NONE");
                stopMovement();
            }
        } else if (isWalkable(getRightBlock(getAngleDiff())) && isWalkable(getRightTopBlock(getAngleDiff())) &&
                (!isWalkable(getLeftBlock(getAngleDiff())) || !isWalkable(getLeftTopBlock(getAngleDiff())))) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    dir = direction.RIGHT;
                    waitForChangeDirection.reset();
                    LogUtils.debugLog("Change direction to RIGHT");
                    updateKeys(true, false, false, false, true);
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        } else if (isWalkable(getLeftBlock(getAngleDiff())) && isWalkable(getLeftTopBlock(getAngleDiff())) &&
                (!isWalkable(getRightBlock(getAngleDiff())) || !isWalkable(getRightTopBlock(getAngleDiff())))) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    dir = direction.LEFT;
                    waitForChangeDirection.reset();
                    LogUtils.debugLog("Change direction to LEFT");
                    updateKeys(false, false, false, true, true);
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        }

        if (prevDir != dir) {
            LogUtils.debugLog("Direction changed to " + dir + " from " + prevDir);
            prevDir = dir;
            waitForChangeDirection.reset();
        }
    }
    
    public static int getAngleDiff() {
        return (FarmConfig.cropType == MacroEnum.MUSHROOM ? 45 : (int) AngleUtils.getClosest30());
    }

    @Override
    public void onLastRender() {
        if(rotation.rotating)
            rotation.update();
    }

    direction calculateDirection() {

        boolean f1 = true, f2 = true;

        if (FarmConfig.cropType == MacroEnum.MUSHROOM_TP_PAD)
            return direction.RIGHT;

        if (rightCropIsReady()) {
            return direction.RIGHT;
        } else if (leftCropIsReady()) {
            return direction.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, -1, 0, getAngleDiff())) && f1) {
                return direction.RIGHT;
            }
            if(!isWalkable(getRelativeBlock(i, 0, 0, getAngleDiff())))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, -1, 0, getAngleDiff())) && f2) {
                return direction.LEFT;
            }
            if(!isWalkable(getRelativeBlock(-i, 0, 0, getAngleDiff())))
                f2 = false;
        }
        System.out.println("No direction found");
        return direction.NONE;
    }
}
