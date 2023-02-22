package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.features.Antistuck;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.*;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.updateKeys;

public class SugarcaneMacro extends Macro {

    enum State {
        WALK,
        TPPAD
    }

    enum WalkState {
        A, D, S
    }

    private final int LANE_WIDTH = 3;

    private State currentState;
    private WalkState currentWalkState;

    private float yaw;
    private float pitch;

    private boolean stuck;

    private final Rotation rotator = new Rotation();

    @Override
    public void onEnable() {
        yaw = AngleUtils.getClosestDiagonal();
        pitch = 0;
        rotator.easeTo(yaw, pitch, 500);

        currentState = State.WALK;
        currentWalkState = calculateDirection();

        stuck = false;
        Antistuck.stuck = false;
        Antistuck.cooldown.schedule(1000);
    }

    @Override
    public void onDisable() {
        KeyBindUtils.stopMovement();
    }



    @Override
    public void onTick() {
        if(mc.thePlayer == null || mc.theWorld == null || stuck) return;

        if (rotator.rotating) {
            updateKeys(false, false, false, false, false);
            return;
        }



        if (currentState != State.TPPAD && (AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), yaw) > 5 || Math.abs(mc.thePlayer.rotationPitch - pitch) > 5)) {
            LogUtils.debugFullLog("Staff rotate");
            rotator.reset();
            /*if (rotateWait.passed() && rotateWait.isScheduled()) {
                LogUtils.webhookLog("Rotate Checked");
                LogUtils.debugLog("Rotate Checked");
                rotation.easeTo(yaw, pitch, 2000);
                rotateWait.reset();
                updateKeys(false, false, false, false, false);
                return;
            } else if (rotateWait.passed()) {
                rotateWait.schedule(500);
            }*/
        }

        if(Antistuck.stuck){
            stuck = true;
            new Thread(fixRowStuck).start();
        }

        switch(currentState){
            case WALK:
                KeyBindUtils.updateKeys(false,
                        currentWalkState == WalkState.S,
                        currentWalkState == WalkState.D,
                        currentWalkState == WalkState.A, true, mc.thePlayer.capabilities.isFlying, false);
                break;
            case TPPAD:
                if(mc.thePlayer.capabilities.isFlying) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
                } else if (mc.thePlayer.posY % 1 > 0 && mc.thePlayer.posY % 1 < 0.8125f) {
                    KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
                } else KeyBindUtils.updateKeys(false,
                            calculateDirection() == WalkState.S,
                            calculateDirection()== WalkState.D,
                            calculateDirection() == WalkState.A, true, false, false);
        }
        updateState();

    }

    @Override
    public void onLastRender() {
        if(rotator.rotating)
            rotator.update();
    }

    private WalkState calculateDirection() {
        if(isWater(getRelativeBlock(2, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw - 45f))
        || isWater(getRelativeBlock(-1, -1, 1, yaw - 45f)) || isWater(getRelativeBlock(-1, 0, 1, yaw - 45f)))
            return WalkState.A;
        else if(isWater(getRelativeBlock(2, -1, 1, yaw + 45f)) || isWater(getRelativeBlock(2, 0, 1, yaw + 45f))
                || isWater(getRelativeBlock(-1, -1, 1, yaw + 45f)) ||isWater(getRelativeBlock(-1, 0, 1, yaw + 45f)))
            return WalkState.D;
        else return WalkState.S;
    }

    void updateState() {
        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame) || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame)) {
            currentState = State.TPPAD;
            return;
        } else {
            currentState = State.WALK;
        }

        switch(currentWalkState){

            case A:
                if(hasWall(0, 1, yaw - 45f) && hasWall(0, -1, yaw - 45f))
                    break; // this situation is indeterminable for A, S or D!

                if(hasWall(0, 1, yaw - 45f))
                    currentWalkState = WalkState.S;
            case D:
                if(hasWall(0, 1, yaw + 45f) && hasWall(0, -1, yaw + 45f))
                    break;

                if(hasWall(0, 1, yaw + 45))
                    currentWalkState = WalkState.S;
                break;
            case S:
                if((hasWall(0, 1, yaw - 45f) && hasWall(0, -1, yaw - 45f)) ||
                        (hasWall(0, 1, yaw + 45f) && hasWall(0, -1, yaw + 45f)))
                    break;

                if(hasWall(0, -1,  yaw - 45) &&
                        (getNearestSideWall(yaw - 45, 1) + getNearestSideWall(yaw - 45, -1)) == 2 * LANE_WIDTH)
                    currentWalkState = WalkState.A;

                if(hasWall(0, -1,  yaw + 45) &&
                        (getNearestSideWall(yaw + 45, 1) + getNearestSideWall(yaw + 45, -1)) == 2 * LANE_WIDTH)
                    currentWalkState = WalkState.D;

                break;
        }

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
        return !isWalkable(getRelativeBlock(rightOffset, 0, frontOffset, yaw)) || !isWalkable(getRelativeBlock(rightOffset, 1, frontOffset, yaw));
    }
    int getNearestSideWall(float yaw, int dir) { // right = 1; left = -1
        for(int i = 0; i < 8; i++) {
            if(hasWall(i * dir, 0, yaw)) return i;
        }
        return -999;
    }

}
