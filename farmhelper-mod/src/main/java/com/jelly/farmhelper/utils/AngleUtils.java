package com.jelly.farmhelper.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

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

    public static float getClosest30() {
        if (get360RotationYaw() < 45) {
            return 30f;
        } else if (get360RotationYaw() < 90) {
            return 60f;
        } else if (get360RotationYaw() < 135) {
            return 120f;
        } else if (get360RotationYaw() < 180) {
            return 150f;
        } else if (get360RotationYaw() < 225) {
            return 210f;
        } else if (get360RotationYaw() < 270) {
            return 240f;
        } else if (get360RotationYaw() < 315) {
            return 300f;
        } else {
            return 330f;
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

    public static Pair<Float, Float> getRotation(BlockPos block) {
        return getRotation(new Vec3(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5));
    }

    public static Pair<Float, Float> getRotation(Entity entity, boolean randomness) {
        return getRotation(new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), randomness);

    }

    public static Pair<Float, Float> getRotation(Entity entity) {
        return getRotation(entity, false);
    }

    public static Pair<Float, Float> getRotation(Vec3 vec3, boolean randomness) {
        double diffX = vec3.xCoord - mc.thePlayer.posX;
        double diffY = vec3.yCoord - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
        double diffZ = vec3.zCoord - mc.thePlayer.posZ;
        return getRotationTo(diffX, diffY, diffZ, randomness);
    }

    public static Pair<Float, Float> getRotation(Vec3 vec3) {
        return getRotation(vec3, false);
    }

    private static Pair<Float, Float> getRotationTo(double diffX, double diffY, double diffZ, boolean randomness) {
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float pitch = (float) -Math.atan2(dist, diffY);
        float yaw = (float) Math.atan2(diffZ, diffX);
        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1) + (randomness ? (float) (Math.random() * 3 - 1.5f) : 0);
        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90) + (randomness ? (float) (Math.random() * 3 - 1.5f) : 0);

        return Pair.of(yaw, pitch);
    }
}
