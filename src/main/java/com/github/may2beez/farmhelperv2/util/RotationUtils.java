package com.github.may2beez.farmhelperv2.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.function.Function;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

public class RotationUtils {
    private final static Minecraft mc = Minecraft.getMinecraft();
    public boolean rotating;
    public boolean completed;

    private long startTime;
    private long endTime;
    private long time;

    Rotation start = new Rotation(0f, 0f);
    Rotation target = new Rotation(0f, 0f);
    Rotation difference = new Rotation(0f, 0f);

    public void easeTo(float yaw, float pitch, long time) {
        completed = false;
        rotating = true;
        startTime = System.currentTimeMillis();
        start.setYaw(mc.thePlayer.rotationYaw);
        start.setPitch(mc.thePlayer.rotationPitch);
        Rotation neededChange = getNeededChange(start, new Rotation(yaw, pitch));
        target.setYaw(start.getYaw() + neededChange.getYaw());
        target.setPitch(start.getPitch() + neededChange.getPitch());

        if (neededChange.getYaw() < 15 && neededChange.getPitch() < 15) {
            LogUtils.sendDebug("[Rotation] Very close rotation, speeding up by 0.5");
            this.time = (long) (time * 0.5);
        } else if (neededChange.getYaw() < 30 && neededChange.getPitch() < 30) {
            this.time = (long) (time * 0.65);
            LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.65");
        } else if (neededChange.getYaw() < 80 && neededChange.getPitch() < 60) {
            this.time = (long) (time * 0.9);
            LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
        } else if (neededChange.getYaw() > 180 || neededChange.getPitch() > 100) {
            this.time = (long) (time * 1.5);
            LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.5");
        } else {
            this.time = time;
            LogUtils.sendDebug("[Rotation] Normal rotation");
        }
        endTime = System.currentTimeMillis() + time;
        getDifference();
    }

    public static Rotation getNeededChange(Rotation startRot, Rotation endRot) {
        float yawDiff = (float) (wrapAngleTo180(endRot.getYaw()) - wrapAngleTo180(startRot.getYaw()));

        yawDiff = AngleUtils.normalizeAngle(yawDiff);

        return new Rotation(yawDiff, endRot.getPitch() - startRot.getPitch());
    }

    public void update() {
        if (mc.currentScreen != null) {
            startTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis() + time;
            return;
        }
        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationYaw = interpolate(start.getYaw(), target.getYaw(), this::easeOutCubic);
            mc.thePlayer.rotationPitch = interpolate(start.getPitch(), target.getPitch(), this::easeOutQuint);
        }
        else if (!completed) {
            mc.thePlayer.rotationYaw = target.getYaw();
            mc.thePlayer.rotationPitch = target.getPitch();
            completed = true;
            rotating = false;
        }
    }

    public void reset() {
        completed = false;
        rotating = false;
    }

    private void getDifference() {
        difference.setYaw(AngleUtils.smallestAngleDifference(AngleUtils.get360RotationYaw(), target.getYaw()));
        difference.setPitch(target.getPitch() - start.getPitch());
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

    @Getter
    @Setter
    public static class Rotation {
        private float yaw;
        private float pitch;

        public Rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
