package com.jelly.farmhelperv2.handler;

import com.jelly.farmhelperv2.event.MotionUpdateEvent;
import com.jelly.farmhelperv2.mixin.client.MinecraftAccessor;
import com.jelly.farmhelperv2.util.AngleUtils;
import com.jelly.farmhelperv2.util.LogUtils;
import com.jelly.farmhelperv2.util.helper.Clock;
import com.jelly.farmhelperv2.util.helper.Rotation;
import com.jelly.farmhelperv2.util.helper.RotationConfiguration;
import com.jelly.farmhelperv2.util.helper.Target;
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
    private float clientSideYaw = 0;
    private float clientSidePitch = 0;
    @Getter
    private float serverSideYaw = 0;
    @Getter
    private float serverSidePitch = 0;
    @Getter
    private RotationConfiguration configuration;
    private float distanceTraveled = 0;

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
        distanceTraveled = 0;
        dontRotate.reset();
        startTime = System.currentTimeMillis();
        startRotation.setRotation(configuration.getFrom());
        Rotation neededChange;
        if (configuration.getTarget().isPresent() && configuration.getTarget().get().getTarget().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.getTarget().get().getTarget().get());
        } else if (configuration.getTo().isPresent()) {
            neededChange = getNeededChange(startRotation, configuration.getTo().get());
        } else {
            throw new IllegalArgumentException("No target or rotation specified!");
        }
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        LogUtils.sendDebugRotation("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythagoras = pythagoras(absYaw, absPitch);
        float time = getTime(pythagoras, configuration.getTime());
        endTime = (long) (System.currentTimeMillis() + Math.max(time, 50 + Math.random() * 100));
        rotating = true;
    }

    private float getTime(float pythagoras, float time) {
        if (pythagoras < 25) {
            LogUtils.sendDebugRotation("[Rotation] Very close rotation, speeding up by 0.65");
            return (long) (time * 0.65);
        }
        if (pythagoras < 45) {
            LogUtils.sendDebugRotation("[Rotation] Close rotation, speeding up by 0.77");
            return (long) (time * 0.77);
        }
        if (pythagoras < 80) {
            LogUtils.sendDebugRotation("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
            return (long) (time * 0.9);
        }
        if (pythagoras > 100) {
            LogUtils.sendDebugRotation("[Rotation] Far rotation, slowing down by 1.1");
            return (long) (time * 1.1);
        }
        LogUtils.sendDebugRotation("[Rotation] Normal rotation");
        return (long) (time * 1.0);
    }

    public void easeBackFromServerRotation() {
        if (configuration == null) return;
        LogUtils.sendDebugRotation("[Rotation] Easing back from server rotation");
        configuration.goingBackToClientSide(true);
        rotating = true;
        startTime = System.currentTimeMillis();
        configuration.setTarget(Optional.empty());
        startRotation.setRotation(new Rotation(serverSideYaw, serverSidePitch));
        Rotation neededChange = getNeededChange(startRotation, new Rotation(clientSideYaw, clientSidePitch));
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        LogUtils.sendDebugRotation("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float time = getTime(pythagoras(absYaw, absPitch), configuration.getTime());
        endTime = System.currentTimeMillis() + Math.max((long) time, 50);
        configuration.setCallback(Optional.of(this::reset));
    }

    private float pythagoras(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
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

    public Rotation getRotation(Vec3 to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, false);
    }

    public Rotation getRotation(Vec3 to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to, randomness);
    }

    public Rotation getRotation(Entity to) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks).subtract(0, 25, 0), false);
    }

    public Rotation getRotation(Entity to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks).subtract(0, 0.25, 0), randomness);
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

    public void reset() {
        LogUtils.sendDebugRotation("[Rotation] Resetting");
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

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.CLIENT)
            return;

        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            endTime = System.currentTimeMillis() + configuration.getTime();
            return;
        }

        if (System.currentTimeMillis() >= endTime) {
            // finish
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
            } else { // No callback, just reset
                if (Math.abs(mc.thePlayer.rotationYaw - targetRotation.getYaw()) < 0.1 && Math.abs(mc.thePlayer.rotationPitch - targetRotation.getPitch()) < 0.1) {
                    mc.thePlayer.rotationYaw = targetRotation.getYaw();
                    mc.thePlayer.rotationPitch = targetRotation.getPitch();
                }
                reset();
                return;
            }
            if (configuration == null || !configuration.goingBackToClientSide()) { // Reset was called in callback
                return;
            }
            return;
        }

        if (configuration.followTarget() && configuration.getTarget().isPresent()) {
            Target target = configuration.getTarget().get();
            Rotation rot;
            if (target.getEntity() != null) {
                rot = getRotation(target.getEntity());
            } else if (target.getBlockPos() != null) {
                rot = getRotation(target.getBlockPos());
            } else if (target.getTarget().isPresent()) {
                rot = getRotation(target.getTarget().get());
            } else {
                throw new IllegalArgumentException("No target specified!");
            }
            if (distanceTraveled > 180) {
                distanceTraveled = 0;
                startRotation.setRotation(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
            }
            Rotation neededChange = getNeededChange(startRotation, rot);
            targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
            targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
            distanceTraveled += Math.abs(neededChange.getYaw());
            long time = (long) getTime(pythagoras(Math.abs(neededChange.getYaw()), Math.abs(neededChange.getPitch())), configuration.getTime());
            endTime = System.currentTimeMillis() + Math.max(time, (long) (100 + Math.random() * 100));
        }
        mc.thePlayer.rotationYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutExpo);
        mc.thePlayer.rotationPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), configuration.easeOutBack() ? this::easeOutBack : this::easeOutQuart);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePre(MotionUpdateEvent.Pre event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        if (System.currentTimeMillis() >= endTime) {
            // finish
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
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
        if (configuration.followTarget() && configuration.getTarget().isPresent() && !configuration.goingBackToClientSide()) {
            Target target = configuration.getTarget().get();
            Rotation rot;
            if (target.getEntity() != null) {
                rot = getRotation(target.getEntity());
            } else if (target.getBlockPos() != null) {
                rot = getRotation(target.getBlockPos());
            } else if (target.getTarget().isPresent()) {
                rot = getRotation(target.getTarget().get());
            } else {
                throw new IllegalArgumentException("No target specified!");
            }
            if (distanceTraveled > 180) {
                distanceTraveled = 0;
                startRotation.setRotation(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch));
            }
            Rotation neededChange = getNeededChange(startRotation, rot);
            targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
            targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
            distanceTraveled += Math.abs(neededChange.getYaw());
//            if (previousTargetLocationHasTeleported(target.getTarget().get())) {
//                previousTargetLocation = Optional.empty();
//                dontRotate.schedule((long) (100 + Math.random() * 300));
//                return;
//            }
            long time = (long) getTime(pythagoras(Math.abs(neededChange.getYaw()), Math.abs(neededChange.getPitch())), configuration.getTime());
            endTime = System.currentTimeMillis() + Math.max(time, (long) (50 + Math.random() * 100));
        }
        if (configuration.goingBackToClientSide()) {
            LogUtils.sendDebugRotation("Going back to client side");
            targetRotation.setYaw(clientSideYaw);
            targetRotation.setPitch(clientSidePitch);
        }
        if (mc.currentScreen != null || dontRotate.isScheduled() && !dontRotate.passed()) {
            event.yaw = serverSideYaw;
            event.pitch = serverSidePitch;
            endTime = System.currentTimeMillis() + configuration.getTime();
        } else {
            float interX = interpolate(startRotation.getYaw(), targetRotation.getYaw(), this::easeOutExpo);
            float interY = interpolate(startRotation.getPitch(), targetRotation.getPitch(), this::easeOutQuart);
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

    @SubscribeEvent(receiveCanceled = true)
    public void onUpdatePost(MotionUpdateEvent.Post event) {
        if (!rotating) return;
        if (configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        mc.thePlayer.rotationYaw = clientSideYaw;
        mc.thePlayer.rotationPitch = clientSidePitch;
    }
}
