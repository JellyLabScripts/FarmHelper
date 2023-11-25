package com.github.may2beez.farmhelperv2.handler;

import com.github.may2beez.farmhelperv2.event.MotionUpdateEvent;
import com.github.may2beez.farmhelperv2.mixin.client.MinecraftAccessor;
import com.github.may2beez.farmhelperv2.util.AngleUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Clock;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import com.github.may2beez.farmhelperv2.util.helper.Target;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static cc.polyfrost.oneconfig.libs.universal.UMath.wrapAngleTo180;

public class RotationHandler {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static RotationHandler instance;

    public static RotationHandler getInstance() {
        if (instance == null) {
            instance = new RotationHandler();
        }
        return instance;
    }

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

    private final Rotation startRotation = new Rotation(0f, 0f);
    private final Rotation targetRotation = new Rotation(0f, 0f);

    public void easeTo(RotationConfiguration configuration) {
        distanceTraveled = 0;
        dontRotate.reset();
        rotating = true;
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

        LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch(), true);

        float absYaw = Math.max(Math.abs(neededChange.getYaw()), 1);
        float absPitch = Math.max(Math.abs(neededChange.getPitch()), 1);
        float pythagoras = pythagoras(absYaw, absPitch);
        float time = getTime(pythagoras, configuration.getTime());
        endTime = (long) (System.currentTimeMillis() + Math.max(time, 50 + Math.random() * 100));
        this.configuration = configuration;
    }

    private float getTime(float pythagoras, float time) {
        if (pythagoras < 25) {
            LogUtils.sendDebug("[Rotation] Very close rotation, speeding up by 0.65", true);
            return (long) (time * 0.65);
        } else if (pythagoras < 45) {
            LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.77", true);
            return (long) (time * 0.77);
        } else if (pythagoras < 80) {
            LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9", true);
            return (long) (time * 0.9);
        } else if (pythagoras > 100) {
            LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.25", true);
            return (long) (time * 1.25);
        } else {
            LogUtils.sendDebug("[Rotation] Normal rotation", true);
            return (long) (time * 1.0);
        }
    }

    public void easeBackFromServerRotation() {
        if (configuration == null) return;
        LogUtils.sendDebug("[Rotation] Easing back from server rotation");
        configuration.goingBackToClientSide(true);
        rotating = true;
        startTime = System.currentTimeMillis();
        configuration.setTarget(Optional.empty());
        startRotation.setRotation(new Rotation(serverSideYaw, serverSidePitch));
        Rotation neededChange = getNeededChange(startRotation, new Rotation(clientSideYaw, clientSidePitch));
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

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
        if (configuration != null) {
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
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks).subtract(0,  0.25, 0), randomness);
    }

    public Rotation getRotation(Vec3 from, Vec3 to) {
        if (configuration != null) {
            return getRotation(from, to, configuration.randomness());
        }
        return getRotation(from, to, false);
    }

    public Rotation getRotation(BlockPos pos) {
        if (configuration != null) {
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
        LogUtils.sendDebug("[Rotation] Resetting");
        rotating = false;
        configuration = null;
        startTime = 0;
        endTime = 0;
    }

    private float interpolate(float start, float end, Function<Float, Float> function) {
        float t = (float) (System.currentTimeMillis() - startTime) / (endTime - startTime);
        return (end - start) * function.apply(t) + start;
    }

    private float easeOutQuart(float x) {
        return (float) (1 - Math.pow(1 - x, 4));
    }

    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
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
                rot = getRotation(target.getEntity(), configuration.randomness());
            } else if (target.getBlockPos() != null) {
                rot = getRotation(target.getBlockPos(), configuration.randomness());
            } else if (target.getTarget().isPresent()) {
                rot = getRotation(target.getTarget().get(), configuration.randomness());
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
            endTime = System.currentTimeMillis() + Math.max(time, (long) (50 + Math.random() * 100));
        }
        mc.thePlayer.rotationYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), this::easeOutExpo);
        mc.thePlayer.rotationPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), this::easeOutQuart);
    }

    private final Clock dontRotate = new Clock();
    private float distanceTraveled = 0;

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
                rot = getRotation(target.getEntity(), configuration.randomness());
            } else if (target.getBlockPos() != null) {
                rot = getRotation(target.getBlockPos(), configuration.randomness());
            } else if (target.getTarget().isPresent()) {
                rot = getRotation(target.getTarget().get(), configuration.randomness());
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
            LogUtils.sendDebug("Going back to client side");
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
            System.out.println("Abs diff: " + absDiffX + " " + absDiffY);
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
