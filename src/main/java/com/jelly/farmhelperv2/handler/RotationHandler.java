package com.jelly.farmhelperv2.handler;

import com.jelly.farmhelperv2.event.MotionUpdateEvent;
import com.jelly.farmhelperv2.mixin.client.MinecraftAccessor;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

public class RotationHandler {
    private static RotationHandler instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Rotation startRotation = new Rotation(0f, 0f);
    private final Rotation targetRotation = new Rotation(0f, 0f);
    private final Clock dontRotate = new Clock();
    @Getter
    private boolean rotating;
    private long startTime;
    private long endTime;
    @Getter
    private float clientSideYaw = 0;
    @Getter
    private float clientSidePitch = 0;
    @Getter
    private float serverSideYaw = 0;
    @Getter
    private float serverSidePitch = 0;
    @Getter
    private RotationConfiguration configuration;

    private final Random random = new Random();

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

    public void easeTo(RotationConfiguration configuration) {
        this.configuration = configuration;
        easingModifier = (random.nextFloat() * 0.5f - 0.25f);
        dontRotate.reset();
        startTime = System.currentTimeMillis();
        startRotation.setRotation(configuration.from());
        Rotation neededChange;
        if (configuration.randomness())
            randomAddition = (Math.random() * 0.3 - 0.15);
        else
            randomAddition = 0;
        if (configuration.target().isPresent() && configuration.target().get().getTarget().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.target().get().getTarget().get());
        } else if (configuration.to().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.to().get());
        } else {
            throw new IllegalArgumentException("No target or rotation specified!");
        }

        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythagoras = pythagoras(absYaw, absPitch);
        float time = getTime(pythagoras, configuration.time());
        endTime = (long) (System.currentTimeMillis() + Math.max(time, 50 + Math.random() * 100));
        rotating = true;
    }

    private float getTime(float pythagoras, float time) {
        if (pythagoras < 25) {
            return (long) (time * 0.65);
        }
        if (pythagoras < 45) {
            return (long) (time * 0.77);
        }
        if (pythagoras < 80) {
            return (long) (time * 0.9);
        }
        if (pythagoras > 100) {
            return (long) (time * 1.1);
        }
        return (long) (time * 1.0);
    }

    public void easeBackFromServerRotation() {
        if (configuration == null) return;
        LogUtils.sendDebug("[Rotation] Easing back from server rotation");
        configuration.goingBackToClientSide(true);
        startTime = System.currentTimeMillis();
        configuration.target(Optional.empty());
        startRotation.setRotation(new Rotation(serverSideYaw, serverSidePitch));
        Rotation neededChange = getNeededChange(startRotation, new Rotation(clientSideYaw, clientSidePitch));
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        float time = configuration.time();
        endTime = System.currentTimeMillis() + Math.max((long) time, 50);
        configuration.callback(Optional.of(this::reset));
        rotating = true;
    }

    private float pythagoras(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
    }

    public Rotation getNeededChange(Rotation target) {
        if (configuration != null && configuration.rotationType() == RotationConfiguration.RotationType.SERVER) {
            return getNeededChange(new Rotation(serverSideYaw, serverSidePitch), target);
        } else {
            return getNeededChange(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), target);
        }
    }

    public Rotation getNeededChange(Rotation startRot, Vec3 target) {
        Rotation targetRot;
        if (configuration != null && random.nextGaussian() > 0.8) {
            targetRot = getRotation(target, configuration.randomness());
        } else {
            targetRot = getRotation(target);
        }
        return getNeededChange(startRot, targetRot);
    }

    public Rotation getNeededChange(Rotation startRot, Rotation endRot) {
        float yawDiff = (float) (wrapAngleTo180(endRot.getYaw()) - wrapAngleTo180(startRot.getYaw()));

        yawDiff = AngleUtils.normalizeAngle(yawDiff);

        return new Rotation(yawDiff, endRot.getPitch() - startRot.getPitch());
    }

    private double randomAddition = (Math.random() * 0.3 - 0.15);

    public Rotation getRotation(Vec3 to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, false);
    }

    public Rotation getRotation(Vec3 to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, randomness);
    }

    public Rotation getRotation(Entity to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionVector().addVector(0, Math.min(((to.height * 0.85) + randomAddition), 1.7), 0), false);
    }

    public Rotation getRotation(Vec3 from, Vec3 to) {
        if (configuration != null && random.nextGaussian() > 0.8) {
            return getRotation(from, to, configuration.randomness());
        }
        return getRotation(from, to, false);
    }

    public Rotation getRotation(BlockPos pos) {
        if (configuration != null && random.nextGaussian() > 0.8) {
            return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), configuration.randomness());
        }
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), false);
    }

    public Rotation getRotation(BlockPos pos, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), randomness);
    }

    public Rotation getRotation(Vec3 from, Vec3 to, boolean randomness) {
        double xDiff = to.xCoord - from.xCoord;
        double yDiff = to.yCoord - from.yCoord;
        double zDiff = to.zCoord - from.zCoord;

        double dist = Math.sqrt(xDiff * xDiff + zDiff * zDiff);

        float yaw = (float) Math.toDegrees(Math.atan2(zDiff, xDiff)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(yDiff, dist));

        if (randomness) {
            yaw += (float) ((Math.random() - 1) * 4);
            pitch += (float) ((Math.random() - 1) * 4);
        }

        return new Rotation(yaw, pitch);
    }

    public float distanceTo(Rotation to) {
        Rotation neededChange = getNeededChange(to);
        return Math.abs(neededChange.getYaw()) + Math.abs(neededChange.getPitch());
    }

    public boolean shouldRotate(Rotation to) {
        return shouldRotate(to, 0.1f);
    }

    public boolean shouldRotate(Rotation to, float difference) {
        Rotation neededChange = getNeededChange(to);
        return Math.abs(neededChange.getYaw()) > difference || Math.abs(neededChange.getPitch()) > difference;
    }

    public void reset() {
        rotating = false;
        configuration = null;
        startTime = 0;
        endTime = 0;
    }

    private float interpolate(float start, float end, Function<Float, Float> function) {
        float t = (float) (System.currentTimeMillis() - startTime) / (endTime - startTime);
        return (end - start) * function.apply(t) + start;
    }

    private float easingModifier = 0;

    private float easeOutQuart(float x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }

    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f + easingModifier;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    private final Clock delayBetweenTargetFollow = new Clock();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!rotating || configuration == null || configuration.rotationType() != RotationConfiguration.RotationType.CLIENT)
            return;

        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            endTime = System.currentTimeMillis() + configuration.time();
            return;
        }

        if (System.currentTimeMillis() >= endTime && !configuration.followTarget()) {
            // finish
            if (configuration.callback().isPresent()) {
                configuration.callback().get().run();
            } else { // No callback, just reset
                mc.thePlayer.rotationYaw = targetRotation.getYaw();
                mc.thePlayer.rotationPitch = targetRotation.getPitch();
                reset();
                return;
            }
            if (configuration == null || !configuration.goingBackToClientSide()) { // Reset was called in callback
                return;
            }
            return;
        }

        if (configuration.followTarget() && configuration.target().isPresent()) {
            adjustTargetRotation(false);
            float currentYaw = mc.thePlayer.rotationYaw;
            float currentPitch = mc.thePlayer.rotationPitch;
            if (shouldRotate(targetRotation, 0.1f)) {
                float needYaw = (targetRotation.getYaw() - currentYaw);
                float needPitch = (targetRotation.getPitch() - currentPitch);
                float distance = Math.abs(needYaw) + Math.abs(needPitch);
                needYaw *= (float) (0.04f + Math.random() * 0.04f);
                needPitch *= (float) (0.04f + Math.random() * 0.04f);
                float scaledFps = 60f / Minecraft.getDebugFPS();
                needYaw *= scaledFps;
                needPitch *= scaledFps;
                needYaw /= Math.max(distance / 80, 1);
                mc.thePlayer.rotationYaw += needYaw;
                if (mc.thePlayer.rotationPitch + needPitch > 75 && needPitch < 0) {
                    mc.thePlayer.rotationPitch += needPitch;
                } else if (mc.thePlayer.rotationPitch + needPitch < -75 && needPitch > 0) {
                    mc.thePlayer.rotationPitch += needPitch;
                } else if (mc.thePlayer.rotationPitch + needPitch > -75 && mc.thePlayer.rotationPitch + needPitch < 75) {
                    mc.thePlayer.rotationPitch += needPitch;
                }
            }
        } else {
            mc.thePlayer.rotationYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
            mc.thePlayer.rotationPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutQuart);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePre(MotionUpdateEvent.Pre event) {
        if (!rotating || configuration == null || configuration.rotationType() != RotationConfiguration.RotationType.SERVER) {
            serverSidePitch = event.pitch;
            serverSideYaw = event.yaw;
            return;
        }

        if (System.currentTimeMillis() >= endTime) {
            // finish
            if (configuration.callback().isPresent()) {
                configuration.callback().get().run();
            } else { // No callback, just reset
                reset();
                return;
            }
            if (configuration == null || !configuration.goingBackToClientSide()) { // Reset was called in callback
                return;
            }
        }
        clientSidePitch = mc.thePlayer.rotationPitch;
        clientSideYaw = mc.thePlayer.rotationYaw;
        // rotating
        if (configuration.followTarget() && configuration.target().isPresent() && !configuration.goingBackToClientSide() && delayBetweenTargetFollow.passed()) {
            adjustTargetRotation(true);
        }
        if (configuration.goingBackToClientSide()) {
            LogUtils.sendDebug("Going back to client side");
            targetRotation.setYaw(clientSideYaw);
            targetRotation.setPitch(clientSidePitch);
        }
        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            event.yaw = serverSideYaw;
            event.pitch = serverSidePitch;
            endTime = System.currentTimeMillis() + configuration.time();
        } else {
            float interX = interpolate(startRotation.getYaw(), targetRotation.getYaw(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
            float interY = interpolate(startRotation.getPitch(), targetRotation.getPitch(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
            float absDiffX = Math.abs(interX - targetRotation.getYaw());
            float absDiffY = Math.abs(interY - targetRotation.getPitch());
            event.yaw = absDiffX < 0.1 ? targetRotation.getYaw() : interX;
            event.pitch = absDiffY < 0.1 ? targetRotation.getPitch() : interY;
        }
        serverSidePitch = event.pitch;
        serverSideYaw = event.yaw;
        mc.thePlayer.rotationYaw = serverSideYaw;
        mc.thePlayer.rotationPitch = serverSidePitch;
    }

    private void adjustTargetRotation(boolean serverSide) {
        Rotation rot;
        if (configuration.target().isPresent() && configuration.target().get().getTarget().isPresent()) {
            rot = getRotation(configuration.target().get().getTarget().get());
        } else if (configuration.to().isPresent()) {
            rot = configuration.to().get();
        } else {
            throw new IllegalArgumentException("No target or rotation specified!");
        }
        startRotation.setPitch(serverSide ? serverSidePitch : mc.thePlayer.rotationPitch);
        startRotation.setYaw(serverSide ? serverSideYaw : mc.thePlayer.rotationYaw);
        startTime = System.currentTimeMillis();
        Rotation neededChange = getNeededChange(startRotation, rot);
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
        delayBetweenTargetFollow.schedule(160 + Math.random() * 80);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePost(MotionUpdateEvent.Post event) {
        if (!rotating) return;
        if (configuration == null || configuration.rotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        mc.thePlayer.rotationYaw = clientSideYaw;
        mc.thePlayer.rotationPitch = clientSidePitch;
    }
}
