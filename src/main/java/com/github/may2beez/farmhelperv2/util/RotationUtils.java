package com.github.may2beez.farmhelperv2.util;

import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.function.Function;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

public class RotationUtils {
    private final static Minecraft mc = Minecraft.getMinecraft();
    public boolean rotating;
    public boolean completed;

    private long startTime;
    private long endTime;
    private long time;

    MutablePair<Float, Float> start = new MutablePair<>(0f, 0f);
    MutablePair<Float, Float> target = new MutablePair<>(0f, 0f);
    MutablePair<Float, Float> difference = new MutablePair<>(0f, 0f);

    public void easeTo(float yaw, float pitch, long time) {
        completed = false;
        rotating = true;
        startTime = System.currentTimeMillis();
        start.setLeft(mc.thePlayer.rotationYaw);
        start.setRight(mc.thePlayer.rotationPitch);
        MutablePair<Float, Float> neededChange = getNeededChange(start, new MutablePair<>(yaw, pitch));
        target.setLeft(start.left + neededChange.left);
        target.setRight(start.right + neededChange.right);

        if (neededChange.left < 30 && neededChange.right < 30) {
            this.time = (long) (time * 0.65);
            LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.7");
        } else if (neededChange.left < 80 && neededChange.right < 60) {
            this.time = (long) (time * 0.9);
            LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
        } else if (neededChange.left > 180 || neededChange.right > 100) {
            this.time = (long) (time * 1.5);
            LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.5");
        } else {
            this.time = time;
            LogUtils.sendDebug("[Rotation] Normal rotation");
        }
        endTime = System.currentTimeMillis() + time;
        getDifference();
    }

    public static MutablePair<Float, Float> getNeededChange(MutablePair<Float, Float> startRot, MutablePair<Float, Float> endRot) {
        float yawDiff = (float) (wrapAngleTo180(endRot.getLeft()) - wrapAngleTo180(startRot.getLeft()));

        yawDiff = AngleUtils.normalizeAngle(yawDiff);

        return new MutablePair<>(yawDiff, endRot.getRight() - startRot.right);
    }

    public void lockAngle(float yaw, float pitch) {
        if (mc.thePlayer.rotationYaw != yaw || mc.thePlayer.rotationPitch != pitch && !rotating)
            easeTo(yaw, pitch, 1000);
    }

    public void update() {
        if (mc.currentScreen != null) {
            startTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis() + time;
            return;
        }
        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationYaw = interpolate(start.getLeft(), target.getLeft(), this::easeOutCubic);
            mc.thePlayer.rotationPitch = interpolate(start.getRight(), target.getRight(), this::easeOutQuint);
        }
        else if (!completed) {
            mc.thePlayer.rotationYaw = target.left;
            mc.thePlayer.rotationPitch = target.right;
            completed = true;
            rotating = false;
        }
    }

    private boolean shouldRotateClockwise() {
        return AngleUtils.clockwiseDifference(AngleUtils.get360RotationYaw(start.left), target.left) < 180;
    }

    public void reset() {
        completed = false;
        rotating = false;
    }

    private void getDifference() {
        difference.setLeft(AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), target.left));
        difference.setRight(target.right - start.right);
    }

    private float interpolate(float start, float end, Function<Float, Float> function) {
        float t = (float) (System.currentTimeMillis() - startTime) / (endTime - startTime);
        return (end - start) * function.apply(t) + start;
    }

    public float easeOutCubic(double number) {
        return (float) Math.max(0, Math.min(1, 1 - Math.pow(1 - number, 3)));
    }

    public float easeOutQuint(float x) {
        return (float) (1 - Math.pow(1 - x, 5));
    }
}
