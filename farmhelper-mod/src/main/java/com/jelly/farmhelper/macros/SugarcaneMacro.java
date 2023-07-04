package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.Config.CropEnum;


import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.features.Failsafe;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class SugarcaneMacro extends Macro {

    enum WalkState {
        A, D, S
    }

    private final int LANE_WIDTH = 3;

    private WalkState currentWalkState;

    private float yaw;
    private float pitch;

    private boolean stuck;

    private final Rotation rotation = new Rotation();

    private final Clock lastTp = new Clock();

    private final Clock waitForChangeDirection = new Clock();


    private boolean isTping = false;


    @Override
    public void onEnable() {
        yaw = AngleUtils.getClosestDiagonal();
        pitch = 0;
        rotation.easeTo(yaw, pitch, 500);
        CropEnum crop = MacroHandler.getFarmingCrop();
        LogUtils.debugLog("Crop: " + crop);
        MacroHandler.crop = crop;

        currentWalkState = calculateDirection();

        stuck = false;
        mc.thePlayer.inventory.currentItem = PlayerUtils.getHoeSlot(CropEnum.SUGAR_CANE);
        Antistuck.stuck = false;
        Antistuck.cooldown.schedule(1000);
        waitForChangeDirection.reset();
        isTping = false;
        lastTp.reset();
    }

    private BlockPos beforeTeleportationPos = null;

    private void checkForTeleport() {
        if (beforeTeleportationPos == null) return;
        if (mc.thePlayer.getPosition().distanceSq(beforeTeleportationPos) > 1) {
            LogUtils.debugLog("Teleported!");
            beforeTeleportationPos = null;
            isTping = false;
            lastTp.schedule(1_000);
            if (!isSpawnLocationSet()) {
                setSpawnLocation();
            }
        }
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }

    private WalkState walkStateBeforeFailsafe = null;

    @Override
    public void saveLastStateBeforeDisable() {
        walkStateBeforeFailsafe = currentWalkState;
        super.saveLastStateBeforeDisable();
    }

    @Override
    public void restoreState() {
        currentWalkState = walkStateBeforeFailsafe;
        super.restoreState();
    }

    @Override
    public void triggerTpCooldown() {
        lastTp.schedule(1500);
    }

    @Override
    public void onTick() {
        if(mc.thePlayer == null || mc.theWorld == null || stuck) return;

        checkForTeleport();

        if (isTping) return;

        if (rotation.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }

        if (lastTp.isScheduled() && lastTp.getRemainingTime() < 500 && !rotation.rotating && mc.thePlayer.rotationPitch != pitch) {
            yaw = AngleUtils.getClosestDiagonal();
            rotation.easeTo(yaw, pitch, 500);
            KeyBindUtils.stopMovement();
        }

        if (lastTp.isScheduled() && !lastTp.passed()) {
            updateKeys(false, false, false, false, false, mc.thePlayer.capabilities.isFlying, false);
            return;
        }

        if (!Failsafe.emergency && !isTping
                && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) >= FarmHelper.config.rotationCheckSensitivity
                || Math.abs(mc.thePlayer.rotationPitch - pitch) >= FarmHelper.config.rotationCheckSensitivity)) {
            rotation.reset();
            Failsafe.emergencyFailsafe(Failsafe.FailsafeType.ROTATION);
            return;
        }

        if(Antistuck.stuck){
            stuck = true;
            new Thread(fixRowStuck).start();
        }

        CropUtils.getTool();

        if (beforeTeleportationPos != null) {
            LogUtils.debugLog("Waiting for tp...");
            KeyBindUtils.stopMovement();
            triggerWarpGarden();
            return;
        } else {

            if (Failsafe.emergency) {
                LogUtils.debugLog("Blocking changing movement due to emergency");
                return;
            }

            KeyBindUtils.updateKeys(false,
                    currentWalkState == WalkState.S,
                    currentWalkState == WalkState.D,
                    currentWalkState == WalkState.A, true, mc.thePlayer.capabilities.isFlying, false);
        }

        updateState();
    }

    @Override
    public void onLastRender() {
        if(rotation.rotating)
            rotation.update();
    }

    private WalkState calculateDirection() {
        if(isWater(getRelativeBlock(2, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw - 45f))
        || isWater(getRelativeBlock(-1, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(-1, 0, 1, yaw - 45f)))
            if(!(hasWall(0, 1, yaw - 45f) && hasWall(-1, 0, yaw - 45f)))
                return WalkState.A;
        else if(isWater(getRelativeBlock(2, -1, 1, yaw + 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw + 45f))
                || isWater(getRelativeBlock(-1, -1, 1, yaw + 45f)) ||isWater(getRelativeBlock(-1, 0, 1, yaw + 45f)))
            if(!(hasWall(0, 1, yaw + 45f) && hasWall(11, 0, yaw + 45f)))
                return WalkState.D;
        return WalkState.S;
    }

    void updateState() {
        if (!isRewarpLocationSet()) {
            LogUtils.scriptLog("Your rewarp position is not set!");
        } else if (isRewarpLocationSet() && isStandingOnRewarpLocation()) {
            triggerWarpGarden();
            return;
        }

        switch(currentWalkState){

            case A:
                if(hasWall(0, 1, yaw - 45f) && hasWall(0, -1, yaw - 45f))
                    break; // this situation is indeterminable for A, S or D!

                if (hasWall(0, 1, yaw - 45f)) {
                    if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                        currentWalkState = WalkState.S;
                        waitForChangeDirection.reset();
                        PlayerUtils.attemptSetSpawn();
                        return;
                    }
                    if (!waitForChangeDirection.isScheduled()) {
                        long waitTime = (FarmHelper.config.fastChangeDirectionCane) ? (long) (Math.random() * 200 + 250) : (long) (Math.random() * 750 + 500);
                        if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                            waitTime = 1;
                        System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                        waitForChangeDirection.schedule(waitTime);
                    }
                }
            case D:
                if(hasWall(0, 1, yaw + 45f) && hasWall(0, -1, yaw + 45f))
                    break;

                if (hasWall(0, 1, yaw + 45f)) {
                    if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                        currentWalkState = WalkState.S;
                        waitForChangeDirection.reset();
                        PlayerUtils.attemptSetSpawn();
                        return;
                    }
                    if (!waitForChangeDirection.isScheduled()) {
                        long waitTime = (FarmHelper.config.fastChangeDirectionCane) ? (long) (Math.random() * 200 + 250) : (long) (Math.random() * 750 + 500);
                        if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                            waitTime = 1;
                        System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                        waitForChangeDirection.schedule(waitTime);
                    }
                }
                break;
            case S:
                if((hasWall(0, 1, yaw - 45f) && hasWall(0, -1, yaw - 45f)) ||
                        (hasWall(0, 1, yaw + 45f) && hasWall(0, -1, yaw + 45f)))
                    break;

                if (hasWall(0, -1, yaw - 45f) && (getNearestSideWall(yaw - 45, 1) + getNearestSideWall(yaw - 45, -1)) == 2 * LANE_WIDTH) {
                    if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                        currentWalkState = WalkState.A;
                        waitForChangeDirection.reset();
                        PlayerUtils.attemptSetSpawn();
                        return;
                    }
                    if (!waitForChangeDirection.isScheduled()) {
                        long waitTime = (FarmHelper.config.fastChangeDirectionCane) ? (long) (Math.random() * 200 + 250) : (long) (Math.random() * 750 + 500);
                        if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                            waitTime = 1;
                        System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                        waitForChangeDirection.schedule(waitTime);
                    }
                }

                if (hasWall(0, -1, yaw + 45f) && (getNearestSideWall(yaw + 45, 1) + getNearestSideWall(yaw + 45, -1)) == 2 * LANE_WIDTH) {
                    if (waitForChangeDirection.isScheduled() && waitForChangeDirection.passed()) {
                        currentWalkState = WalkState.D;
                        waitForChangeDirection.reset();
                        PlayerUtils.attemptSetSpawn();
                        return;
                    }
                    if (!waitForChangeDirection.isScheduled()) {
                        long waitTime = (FarmHelper.config.fastChangeDirectionCane) ? (long) (Math.random() * 200 + 250) : (long) (Math.random() * 750 + 500);
                        if ((BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)))
                            waitTime = 1;
                        System.out.println("Scheduling wait for change direction for " + waitTime + "ms");
                        waitForChangeDirection.schedule(waitTime);
                    }
                }

                break;
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
        return;
    }

    public Runnable fixRowStuck = () -> {
        try {
            LogUtils.scriptLog("Activated antistuck");
            Thread.sleep(20);
            updateKeys(false, true, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, true, false, false);
            Thread.sleep(200);
            updateKeys(true, false, false, false, false);
            Thread.sleep(200);
            updateKeys(false, false, false, true, false);
            Thread.sleep(200);
            updateKeys(false, false, false, false, false);
            stuck = false;
            Antistuck.stuck = false;
            Antistuck.cooldown.schedule(2000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    };

    boolean hasWall(int rightOffset, int frontOffset, float yaw) {
        boolean f1 = !isWalkable(getRelativeBlock(rightOffset, 1, frontOffset, yaw));
        return  f1 ||
                (BlockUtils.getRelativeBlock(0,0, 0).equals(Blocks.end_portal_frame) ?
                !isWalkable(getRelativeBlock(rightOffset, 2, frontOffset, yaw)) : !isWalkable(getRelativeBlock(rightOffset, 0, frontOffset, yaw)));
    }
    int getNearestSideWall(float yaw, int dir) { // right = 1; left = -1
        for(int i = 0; i < 8; i++) {
            if(hasWall(i * dir, 0, yaw)) return i;
        }
        return -999;
    }

}
