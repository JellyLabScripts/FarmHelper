package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.enums.CropEnum;
import com.jelly.farmhelper.config.interfaces.FailsafeConfig;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.BlockUtils.getRelativeBlock;
import static com.jelly.farmhelper.utils.KeyBindUtils.stopMovement;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class MushroomMacro extends Macro {
    private static final Minecraft mc = Minecraft.getMinecraft();
    enum direction {
        LEFT,
        FORWARD,
        NONE
    }

    direction dir;
    direction prevDir;

    float pitch;
    float yaw;

    Rotation rotation = new Rotation();

    private final Clock lastTp = new Clock();

    private Vec3 preTpPos;
    private final Clock waitForChangeDirection = new Clock();


    @Override
    public void onEnable() {
        lastTp.reset();
        waitForChangeDirection.reset();
        pitch = (float) (Math.random() * 2 - 1); // -1 - 1
        yaw = AngleUtils.getClosestDiagonal();
        dir = direction.NONE;
        rotation.easeTo(yaw, pitch, 500);
        mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot();
        preTpPos = null;
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    @Override
    public void onTick() {

        if(mc.thePlayer == null || mc.theWorld == null)
            return;

        if(rotation.rotating) {
            KeyBindUtils.stopMovement();
            return;
        }

        if (lastTp.isScheduled() && preTpPos != null && preTpPos.distanceTo(mc.thePlayer.getPositionVector()) > 1) {
            updateKeys(dir == direction.FORWARD, false, false, dir == direction.LEFT, true);
        }

        if (lastTp.passed()) {
            lastTp.reset();
        }

        if (lastTp.isScheduled()) {
            return;
        }

        if (!lastTp.isScheduled() && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > FailsafeConfig.rotationSens || Math.abs(mc.thePlayer.rotationPitch - pitch) > FailsafeConfig.rotationSens)) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }

        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)) {//standing on tp pad
            if (!lastTp.isScheduled()) {
                preTpPos = mc.thePlayer.getPositionVector();
                lastTp.schedule(2500);
            }
            LogUtils.debugLog("Scheduled tp");

            updateKeys(false, false, false, false, false);
            if (dir == direction.FORWARD) {
                dir = direction.LEFT;
            } else {
                dir = direction.FORWARD;
            }
            return;
        }

        if (dir == direction.FORWARD) {
            updateKeys(true, false, false, false, false);
        }

        if (isWalkable(getRightBlock()) && isWalkable(getLeftBlock())) {
            if(mc.thePlayer.lastTickPosY - mc.thePlayer.posY != 0)
                return;

            if (dir == direction.NONE) {
                dir = calculateDirection();
            }
            if (dir == direction.FORWARD)
                updateKeys(true, false, false, false, true);
            else if (dir == direction.LEFT) {
                updateKeys(false, false, false, true, true);
            } else {
                stopMovement();
            }
        } else if (isWalkable(getRightBlock()) && isWalkable(getRightTopBlock()) &&
                (!isWalkable(getLeftBlock()) || !isWalkable(getLeftTopBlock()))) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    dir = direction.FORWARD;
                    waitForChangeDirection.reset();
                    updateKeys(true, false, false, false, true);
                    if (Math.random() < 0.5d)
                        PlayerUtils.attemptSetSpawn();
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        } else if (isWalkable(getLeftBlock()) && isWalkable(getLeftTopBlock()) &&
                (!isWalkable(getRightBlock()) || !isWalkable(getRightTopBlock()))) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                    dir = direction.LEFT;
                    waitForChangeDirection.reset();
                    updateKeys(false, false, false, true, true);
                    if (Math.random() < 0.5d)
                        PlayerUtils.attemptSetSpawn();
                    return;
                }
                if (!waitForChangeDirection.isScheduled()) {
                    long waitTime = (long) (Math.random() * 500 + 250);
                    waitForChangeDirection.schedule(waitTime);
                }
            }
        }

        if (prevDir != dir) {
            prevDir = dir;
            waitForChangeDirection.reset();
        }
    }

    @Override
    public void onLastRender() {
        if(rotation.rotating)
            rotation.update();
    }

    direction calculateDirection() {

        boolean f1 = true, f2 = true;

        if (!leftCropIsReady()) {
            return direction.FORWARD;
        } else if (!rightCropIsReady()) {
            return direction.LEFT;
        }

        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, -1, 0, mc.thePlayer.rotationYaw - 45)) && f1) {
                return direction.FORWARD;
            }
            if(!isWalkable(getRelativeBlock(i, 0, 0, mc.thePlayer.rotationYaw - 45)))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, -1, 0, mc.thePlayer.rotationYaw - 45)) && f2) {
                return direction.LEFT;
            }
            if(!isWalkable(getRelativeBlock(-i, 0, 0, mc.thePlayer.rotationYaw - 45)))
                f2 = false;
        }
        System.out.println("No direction found");
        return direction.NONE;
    }
}
