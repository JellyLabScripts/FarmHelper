package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;

public class AngleUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float get360RotationYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    public static float get360RotationYaw() {
        return get360RotationYaw(mc.thePlayer.rotationYaw);
    }

    public static float clockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(targetYaw360 - initialYaw360);
    }

    public static float antiClockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(initialYaw360 - targetYaw360);
    }

    public static float smallestAngleDifference(float initialYaw360, float targetYaw360) {
        return Math.min(clockwiseDifference(initialYaw360, targetYaw360), antiClockwiseDifference(initialYaw360, targetYaw360));
    }

    public static float getActualYawFrom360(float yaw360) {
        float currentYaw = yaw360;
        if(mc.thePlayer.rotationYaw > yaw360){
            while (mc.thePlayer.rotationYaw - currentYaw < 180 || mc.thePlayer.rotationYaw - currentYaw > 0){
                if(Math.abs(currentYaw + 360 - mc.thePlayer.rotationYaw) < Math.abs(currentYaw - mc.thePlayer.rotationYaw))
                    currentYaw = currentYaw + 360;
                else  break;
            }
        }
        if(mc.thePlayer.rotationYaw < yaw360){
            while (currentYaw - mc.thePlayer.rotationYaw > 180 || mc.thePlayer.rotationYaw - currentYaw < 0){
                if(Math.abs(currentYaw - 360 - mc.thePlayer.rotationYaw) < Math.abs(currentYaw - mc.thePlayer.rotationYaw))
                    currentYaw = currentYaw - 360;
                else  break;
            }
        }
        return currentYaw;


    }

    public static float getClosestDiagonal() {
        if (get360RotationYaw() < 90 && get360RotationYaw() > 0) {
            return 45;
        } else if (get360RotationYaw() < 180) {
            return 135f;
        } else if (get360RotationYaw() < 270) {
            return 225f;
        } else {
            return 315f;
        }
    }
    public static float getClosest() {
        if (get360RotationYaw() < 45 || get360RotationYaw() > 315) {
            return 0f;
        } else if (get360RotationYaw() < 135) {
            return 90f;
        } else if (get360RotationYaw() < 225) {
            return 180f;
        } else {
            return 270f;
        }
    }
    public static float getClosest(float yaw) {
        if (get360RotationYaw(yaw) < 45 || get360RotationYaw(yaw) > 315) {
            return 0f;
        } else if (get360RotationYaw(yaw) < 135) {
            return 90f;
        } else if (get360RotationYaw(yaw) < 225) {
            return 180f;
        } else {
            return 270f;
        }
    }
}
