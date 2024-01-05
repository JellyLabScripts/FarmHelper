package com.jelly.farmhelperv2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class AngleUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float get360RotationYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    public static float normalizeAngle(float angle) {
        while (angle > 180) {
            angle -= 360;
        }
        while (angle <= -180) {
            angle += 360;
        }
        return angle;
    }

    public static float normalizeYaw(float yaw) {
        float newYaw = yaw % 360F;
        if (newYaw < -180F) {
            newYaw += 360F;
        }
        if (newYaw > 180F) {
            newYaw -= 360F;
        }
        return newYaw;
    }

    public static float get360RotationYaw() {
        if (mc.thePlayer == null)
            return 0;
        return get360RotationYaw(mc.thePlayer.rotationYaw);
    }

    public static float clockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(targetYaw360 - initialYaw360);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static float antiClockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(initialYaw360 - targetYaw360);
    }

    public static float smallestAngleDifference(float initialYaw360, float targetYaw360) {
        return Math.min(clockwiseDifference(initialYaw360, targetYaw360), antiClockwiseDifference(initialYaw360, targetYaw360));
    }

    public static float getActualYawFrom360(float yaw360) {
        float currentYaw = yaw360;
        if (mc.thePlayer.rotationYaw > yaw360) {
            while (mc.thePlayer.rotationYaw - currentYaw < 180 || mc.thePlayer.rotationYaw - currentYaw > 0) {
                if (Math.abs(currentYaw + 360 - mc.thePlayer.rotationYaw) < Math.abs(currentYaw - mc.thePlayer.rotationYaw))
                    currentYaw = currentYaw + 360;
                else break;
            }
        }
        if (mc.thePlayer.rotationYaw < yaw360) {
            while (currentYaw - mc.thePlayer.rotationYaw > 180 || mc.thePlayer.rotationYaw - currentYaw < 0) {
                if (Math.abs(currentYaw - 360 - mc.thePlayer.rotationYaw) < Math.abs(currentYaw - mc.thePlayer.rotationYaw))
                    currentYaw = currentYaw - 360;
                else break;
            }
        }
        return currentYaw;


    }

    public static float getClosestDiagonal() {
        return getClosestDiagonal(get360RotationYaw());
    }

    public static float getClosestDiagonal(float yaw) {
        if (get360RotationYaw(yaw) < 90 && get360RotationYaw(yaw) > 0) {
            return 45;
        } else if (get360RotationYaw(yaw) < 180) {
            return 135f;
        } else if (get360RotationYaw(yaw) < 270) {
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

    public static float getClosest45(float inputAngle) {
        float normalizedAngle = (inputAngle % 360 + 360) % 360;
        float remainder = normalizedAngle % 45;
        if (remainder <= 22.5) {
            return (float) (Math.floor(normalizedAngle / 45) * 45);
        } else {
            return (float) (Math.ceil(normalizedAngle / 45) * 45);
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

//    public static RotationUtils.Rotation getRotation(BlockPos block) {
//        return getRotation(new Vec3(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5));
//    }
//
//    public static RotationUtils.Rotation getRotation(Entity entity, boolean randomness) {
//        AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
//        double diffX = boundingBox.minX + (boundingBox.maxX - boundingBox.minX) * 0.8 - mc.thePlayer.posX;
//        double diffY = boundingBox.minY + (boundingBox.maxY - boundingBox.minY) * 0.8 - mc.thePlayer.posY - mc.thePlayer.getEyeHeight() - 0.2;
//        double diffZ = boundingBox.minZ + (boundingBox.maxZ - boundingBox.minZ) * 0.8 - mc.thePlayer.posZ;
//        return getRotationTo(diffX, diffY, diffZ, randomness);
//    }
//
//    public static RotationUtils.Rotation getRotation(final Vec3 from, final Vec3 to) {
//        double diffX = to.xCoord - from.xCoord;
//        double diffY = to.yCoord - from.yCoord;
//        double diffZ = to.zCoord - from.zCoord;
//        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
//
//        float pitch = (float) -Math.atan2(dist, diffY);
//        float yaw = (float) Math.atan2(diffZ, diffX);
//        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1);
//        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90);
//
//        return new RotationUtils.Rotation(pitch, yaw);
//    }
//
//    public static RotationUtils.Rotation getRotation(Entity entity) {
//        return getRotation(entity.getPositionEyes(1), false);
//    }
//
//    public static RotationUtils.Rotation getRotation(Vec3 vec3, boolean randomness) {
//        double diffX = vec3.xCoord - mc.thePlayer.posX;
//        double diffY = vec3.yCoord - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
//        double diffZ = vec3.zCoord - mc.thePlayer.posZ;
//        return getRotationTo(diffX, diffY, diffZ, randomness);
//    }
//
//    public static RotationUtils.Rotation getRotation(Vec3 vec3) {
//        return getRotation(vec3, false);
//    }
//
//    private static RotationUtils.Rotation getRotationTo(double diffX, double diffY, double diffZ, boolean randomness) {
//        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
//
//        float pitch = (float) -Math.atan2(dist, diffY);
//        float yaw = (float) Math.atan2(diffZ, diffX);
//        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1) + (randomness ? (float) (Math.random() * 6 - 3f) : 0);
//        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90) + (randomness ? (float) (Math.random() * 6 - 3f) : 0);
//
//        return new RotationUtils.Rotation(yaw, pitch);
//    }
}
