package com.jelly.FarmHelper.utils;

import com.jelly.FarmHelper.config.enums.AngleEnum;
import net.minecraft.client.Minecraft;

public class AngleUtils {
    private static Minecraft mc = Minecraft.getMinecraft();

    public static float angleToValue(AngleEnum c) {
        return !c.toString().replace("A", "").contains("N") ?
            Integer.parseInt(c.toString().replace("A", "")) :
            Integer.parseInt(c.toString().replace("A", "").replace("N", "")) * -1;
    }

    public static float get360RotationYaw() {
        return get360RotationYaw(mc.thePlayer.rotationYaw);
    }

    public static float get360RotationYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    public static float getOppositeAngle(int angle) {
        return (angle < 180) ? angle + 180 : angle - 180;
    }

    public static boolean shouldRotateClockwise(float initialYaw360, float targetYaw360) {
        return clockwiseDifference(initialYaw360, targetYaw360) < 180;
    }

    public static float clockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(targetYaw360 - initialYaw360);
    }

    public static float antiClockwiseDifference(float initialYaw360, float targetYaw360) {
        return get360RotationYaw(initialYaw360 - targetYaw360);
    }

    public static void smoothRotateTo(float targetYaw360, float speed) {
        if (shouldRotateClockwise(get360RotationYaw(), targetYaw360)) {
            smoothRotateClockwise(clockwiseDifference(get360RotationYaw(), targetYaw360), speed);
        } else {
            smoothRotateAnticlockwise(antiClockwiseDifference(get360RotationYaw(), targetYaw360), speed);
        }
    }

    public static void smoothRotateClockwise(float rotateAngle) {
        smoothRotateClockwise(rotateAngle, 1);
    }

    public static void smoothRotateClockwise(float rotateAngle, float speed) {
        float targetYaw = (get360RotationYaw() + rotateAngle) % 360;
        while (get360RotationYaw() != targetYaw) {
            if (Math.abs(get360RotationYaw() - targetYaw) < speed) {
                mc.thePlayer.rotationYaw += Math.abs(get360RotationYaw() - targetYaw);
                return;
            }
            mc.thePlayer.rotationYaw += (0.3f + Utils.nextInt(3) / 10.0f) * speed;
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sineRotateCW(float rotateAngle, float speed) {
        float targetYaw = (get360RotationYaw() + rotateAngle) % 360;
        while (get360RotationYaw() != targetYaw) {
            float difference = Math.abs(get360RotationYaw() - targetYaw);
            if (difference < 0.4f * speed) {
                mc.thePlayer.rotationYaw += difference;
                return;
            }
            mc.thePlayer.rotationYaw += speed * 0.3 * ((difference / rotateAngle) + (Math.PI / 2));
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void smoothRotateAnticlockwise(float rotateAngle) {
        smoothRotateAnticlockwise(rotateAngle, 1);
    }

    public static void smoothRotateAnticlockwise(float rotateAngle, float speed) {
        float targetYaw = get360RotationYaw(get360RotationYaw() - rotateAngle);
        while (get360RotationYaw() != targetYaw) {
            if (Math.abs(get360RotationYaw() - targetYaw) < speed) {
                mc.thePlayer.rotationYaw -= Math.abs(get360RotationYaw() - targetYaw);
                return;
            }
            mc.thePlayer.rotationYaw -= (0.3f + Utils.nextInt(3) / 10.0f) * speed;
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sineRotateACW(float rotateAngle, float speed) {
        float targetYaw = get360RotationYaw(get360RotationYaw() - rotateAngle);
        while (get360RotationYaw() != targetYaw) {
            float difference = Math.abs(get360RotationYaw() - targetYaw);
            if (difference < 0.4f * speed) {
                mc.thePlayer.rotationYaw -= difference;
                return;
            }
            mc.thePlayer.rotationYaw -= speed * 0.3 * ((difference / rotateAngle) + (Math.PI / 2));
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static float getActualRotationYaw() { //f3
        return getActualRotationYaw(mc.thePlayer.rotationYaw);
    }

    public static float getActualRotationYaw(float yaw) { //f3
        return yaw > 0 ?
            (yaw % 360 > 180 ? -(180 - (yaw % 360 - 180)) : yaw % 360) :
            (-yaw % 360 > 180 ? (180 - (-yaw % 360 - 180)) : -(-yaw % 360));
    }

    public static void hardRotate(float yaw) {
        while (get360RotationYaw() != yaw) {
            if (Math.abs(get360RotationYaw() - yaw) < 0.2f) {
                mc.thePlayer.rotationYaw = yaw;
                return;
            }
            if (shouldRotateClockwise(get360RotationYaw(), yaw)) {
                mc.thePlayer.rotationYaw += 0.1f;
            } else {
                mc.thePlayer.rotationYaw -= 0.1f;
            }
        }
    }
}
