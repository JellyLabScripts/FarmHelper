package com.jelly.farmhelper.macros;

import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.config.interfaces.FarmConfig;
import com.jelly.farmhelper.player.Rotation;
import com.jelly.farmhelper.utils.AngleUtils;
import com.jelly.farmhelper.utils.KeyBindUtils;

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
                pitch = 0f; //TODO: Find a pitch that works
                break;
        }
        dir = direction.NONE;
        rotation.easeTo(yaw, pitch, 500);

    }

    @Override
    public void onTick() {

        if(mc.thePlayer == null || mc.theWorld == null)
            return;

        if(mc.thePlayer.lastTickPosY - mc.thePlayer.posY != 0)
            return;

        if ((mc.thePlayer.posY % 1) == 0.8125) {//standing on tp pad
            dir = direction.NONE;
            if (mc.thePlayer.capabilities.isFlying) {
                KeyBindUtils.updateKeys(false, false, false, false, false, true, false);
            } else if (mc.thePlayer.posY % 1 > 0 && mc.thePlayer.posY % 1 < 0.8125f) {
                KeyBindUtils.updateKeys(false, false, false, false, false, false, true);
            } else if (isWalkable(getRelativeBlock(1, 1, 0))) {
                updateKeys(false, false, true, false, true);
            } else if (isWalkable(getRelativeBlock(-1, 1, 0))) {
                updateKeys(false, false, false, true, true);
                return;
            }
        }

        if (isWalkable(getRightBlock()) && isWalkable(getLeftBlock())) {
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
            }
        } else if (isWalkable(getLeftBlock()) && !isWalkable(getRightBlock())) {
            if (FarmHelper.gameState.dx < 0.01d && FarmHelper.gameState.dz < 0.01d) {
                dir = direction.LEFT;
            }
        }


    }

    direction calculateDirection() {
        for (int i = 0; i < 180; i++) {
            if (isWalkable(getRelativeBlock(i, -1, 0))) {
                return direction.RIGHT;
            }
            if (!isWalkable(getRelativeBlock(i, 0, 0)))
                break;

        }
        for (int i = 0; i > -180; i--) {
            if (isWalkable(getRelativeBlock(i, -1, 0))) {
                return direction.LEFT;
            }
            if (!isWalkable(getRelativeBlock(i, 0, 0)))
                break;
        }
        return direction.NONE;
    }

}
