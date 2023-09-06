package com.jelly.farmhelper.player;

import com.jelly.farmhelper.hud.DebugHUD;
import com.jelly.farmhelper.utils.AngleUtils;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.tuple.MutablePair;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

// Inspired by Apfelsaft, no steal clueless
public class Rotation {
    private final static Minecraft mc = Minecraft.getMinecraft();
    public boolean rotating;
    public boolean completed;

    private long startTime;
    private long endTime;

    MutablePair<Float, Float> start = new MutablePair<>(0f, 0f);
    MutablePair<Float, Float> target = new MutablePair<>(0f, 0f);
    MutablePair<Float, Float> difference = new MutablePair<>(0f, 0f);

    public void easeTo(float yaw, float pitch, long time) {
        completed = false;
        rotating = true;
        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
        start.setLeft(mc.thePlayer.rotationYaw);
        start.setRight(mc.thePlayer.rotationPitch);
        MutablePair<Float, Float> neededChange = getNeededChange(start, new MutablePair<>(yaw, pitch));
        target.setLeft(start.left + neededChange.left);
        target.setRight(start.right + neededChange.right);
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
        if (System.currentTimeMillis() <= endTime) {
//            if (shouldRotateClockwise()) {
//                mc.thePlayer.rotationYaw = start.left + interpolate(difference.left);
//            } else {
//                mc.thePlayer.rotationYaw = start.left - interpolate(difference.left);
//            }
            mc.thePlayer.rotationYaw = interpolate(start.getLeft(), target.getLeft());
            mc.thePlayer.rotationPitch = interpolate(start.getRight(), target.getRight());
        }
        else if (!completed) {
//            if (shouldRotateClockwise()) {
//                LogUtils.sendDebug("Rotation final st - " + start.left + ", " + mc.thePlayer.rotationYaw);
//                mc.thePlayer.rotationYaw = target.left;
//                LogUtils.sendDebug("Rotation final - " + start.left + difference.left);
//            } else {
//                mc.thePlayer.rotationYaw = target.left;
//                LogUtils.sendDebug("Rotation final - " + (start.left - difference.left));
//            }
            mc.thePlayer.rotationYaw = target.left;
            mc.thePlayer.rotationPitch = target.right;
//            mc.thePlayer.rotationPitch = start.right + difference.right;
            completed = true;
            rotating = false;
        }
        DebugHUD.rotating = rotating;
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

//    private float interpolate(float difference) {
//        final float spentMillis = System.currentTimeMillis() - startTime;
//        final float relativeProgress = spentMillis / (endTime - startTime);
//        return (difference) * easeOutSine(relativeProgress);
//    }
//
//    private float easeOutCubic(double number) {
//        return (float)(1.0 - Math.pow(1.0 - number, 3.0));
//    }

    private float interpolate(float start, float end) {
        return (end - start) * easeOutCubic((float) (System.currentTimeMillis() - startTime) / (endTime - startTime)) + start;
    }

    public float easeOutCubic(double number) {
        return (float) Math.max(0, Math.min(1, 1 - Math.pow(1 - number, 3)));
    }

    private float easeOutSine(double number) {
        return (float) Math.sin((number * Math.PI) / 2);
    }
}
