package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.AngleUtils;
import com.jelly.farmhelper.utils.BlockUtils;
import com.jelly.farmhelper.utils.KeyBindUtils;
import net.minecraft.init.Blocks;

import static com.jelly.farmhelper.utils.BlockUtils.*;
import static com.jelly.farmhelper.utils.KeyBindUtils.*;

public class VerticalCropMacro extends Macro{
    enum direction {
        RIGHT,
        LEFT,
        NONE
    }
    direction dir;

    float pitch;
    float yaw;

    int flyToggleTimer = 0;

    Rotation rotation = new Rotation();

    @Override
    public void onEnable() {
        yaw = AngleUtils.getClosest();
        switch(FarmConfig.cropType){
            case SUGARCANE:
                pitch = 1.2f; // 0 - 2
                break;
            case POTATO: case CARROT: case WHEAT:
                pitch = 2.8f; // 2.4-3.0
                break;
            case NETHERWART:
                pitch = 0f; // -1 - 1
                break;
            case MELONS:
                pitch = 28f; //28-30
                break;
        }
        dir = direction.NONE;
        flyToggleTimer = 0;
        rotation.easeTo(yaw, pitch, 500);

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



        System.out.println(mc.thePlayer.capabilities.isFlying + " " + flyToggleTimer + " " + getRelativeBlock(0, 0, 0));

        if (BlockUtils.getRelativeBlock(0, -1, 0).equals(Blocks.end_portal_frame)
                || BlockUtils.getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame) ||
                BlockUtils.getRelativeBlock(0, -2, 0).equals(Blocks.end_portal_frame)) {//standing on tp pad
            if(flyToggleTimer > 0)
                flyToggleTimer --;

            dir = direction.NONE;

            if(mc.thePlayer.capabilities.isFlying || (!getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame))) {
                KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
            } else if (mc.thePlayer.posY % 1 > 0 && mc.thePlayer.posY % 1 < 0.8125f) {
                KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
            } else if (isWalkable(getRelativeBlock(1, 1, 0))) {
                updateKeys(false, false, true, false, true);
            } else if (isWalkable(getRelativeBlock(-1, 1, 0))) {
                updateKeys(false, false, false, true, true);
                return;
            }
           /* if ((!getRelativeBlock(0, 0, 0).equals(Blocks.end_portal_frame))) {
                KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
            } else if (mc.thePlayer.posY % 1 > 0 && mc.thePlayer.posY % 1 < 0.8125f) {
                if(flyToggleTimer == 0 && !mc.thePlayer.capabilities.isFlying) {
                    KeyBindUtils.onTick(mc.gameSettings.keyBindJump);
                    flyToggleTimer = 5;
                }
            } else if (isWalkable(getRelativeBlock(1, 1, 0))) {
                updateKeys(false, false, true, false, true);
            } else if (isWalkable(getRelativeBlock(-1, 1, 0))) {
                updateKeys(false, false, false, true, true);
                return;
            }*/
        }

        if (isWalkable(getRightBlock()) && isWalkable(getLeftBlock())) {

            if(mc.thePlayer.lastTickPosY - mc.thePlayer.posY != 0)
                return;

            if (dir == direction.NONE) {
                dir = calculateDirection();
            }
            if (dir == direction.RIGHT) {
                updateKeys(false, false, true, false, true);
            } else if (dir == direction.LEFT) {
                updateKeys(false, false, false, true, true);
            } else {
                stopMovement();
            }
        } else if (!isWalkable(getLeftBlock()) && isWalkable(getRightBlock())) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                dir = direction.RIGHT;
                updateKeys(false, false, true, false, true);
            }
        } else if (isWalkable(getLeftBlock()) && !isWalkable(getRightBlock())) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                dir = direction.LEFT;
                updateKeys(false, false, false, true, true);
            }
        }


    }

    @Override
    public void onLastRender() {
        if(rotation.rotating)
            rotation.update();
    }

    direction calculateDirection() {

        boolean f1 = true, f2 = true;
        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, -1, 0)) && f1) {
                return direction.RIGHT;
            }
            if(!isWalkable(getRelativeBlock(i, 0, 0)))
                f1 = false;
            if (isWalkable(getRelativeBlock(-i, -1, 0)) && f2) {
                return direction.LEFT;
            }
            if(!isWalkable(getRelativeBlock(-i, 0, 0)))
                f2 = false;
        }
        return direction.NONE;
    }

}
