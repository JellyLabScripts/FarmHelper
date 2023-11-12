package com.github.may2beez.farmhelperv2.handler;

import com.github.may2beez.farmhelperv2.event.MotionUpdateEvent;
import com.github.may2beez.farmhelperv2.mixin.client.MinecraftAccessor;
import com.github.may2beez.farmhelperv2.util.AngleUtils;
import com.github.may2beez.farmhelperv2.util.LogUtils;
import com.github.may2beez.farmhelperv2.util.helper.Rotation;
import com.github.may2beez.farmhelperv2.util.helper.RotationConfiguration;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Optional;
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
    private boolean completed;

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
        completed = false;
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

        LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        int absYaw = Math.abs((int) neededChange.getYaw());
        int absPitch = Math.abs((int) neededChange.getPitch());
        int pythagoras = (int) pythagoras(absYaw, absPitch);
        if (pythagoras < 25) {
            LogUtils.sendDebug("[Rotation] Very close rotation, speeding up by 0.65");
            configuration.setTime((long) (configuration.getTime() * 0.65));
        } else if (pythagoras < 45) {
            configuration.setTime((long) (configuration.getTime() * 0.77));
            LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.77");
        } else if (pythagoras < 80) {
            configuration.setTime((long) (configuration.getTime() * 0.9));
            LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
        } else if (pythagoras > 100) {
            configuration.setTime((long) (configuration.getTime() * 1.25));
            LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.25");
        } else {
            configuration.setTime((long) (configuration.getTime() * 1.0));
            LogUtils.sendDebug("[Rotation] Normal rotation");
        }
        endTime = System.currentTimeMillis() + configuration.getTime();
        this.configuration = configuration;
    }

    public void easeBackFromServerRotation() {
        if (configuration == null) return;
        LogUtils.sendDebug("[Rotation] Easing back from server rotation");
        configuration.setGoingBackToClientSide(true);
        completed = false;
        rotating = true;
        startTime = System.currentTimeMillis();
        configuration.setTarget(Optional.empty());
        startRotation.setRotation(new Rotation(serverSideYaw, serverSidePitch));
        Rotation neededChange = getNeededChange(startRotation, new Rotation(clientSideYaw, clientSidePitch));
        targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
        targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());

        LogUtils.sendDebug("[Rotation] Needed change: " + neededChange.getYaw() + " " + neededChange.getPitch());

        int absYaw = Math.abs((int) neededChange.getYaw());
        int absPitch = Math.abs((int) neededChange.getPitch());
        int pythagoras = (int) pythagoras(absYaw, absPitch);
        if (pythagoras < 25) {
            LogUtils.sendDebug("[Rotation] Very close rotation, speeding up by 0.65");
            configuration.setTime((long) (configuration.getTime() * 0.65));
        } else if (pythagoras < 45) {
            configuration.setTime((long) (configuration.getTime() * 0.77));
            LogUtils.sendDebug("[Rotation] Close rotation, speeding up by 0.77");
        } else if (pythagoras < 80) {
            configuration.setTime((long) (configuration.getTime() * 0.9));
            LogUtils.sendDebug("[Rotation] Not so close, but not that far rotation, speeding up by 0.9");
        } else if (pythagoras > 100) {
            configuration.setTime((long) (configuration.getTime() * 1.25));
            LogUtils.sendDebug("[Rotation] Far rotation, slowing down by 1.25");
        } else {
            configuration.setTime((long) (configuration.getTime() * 1.0));
            LogUtils.sendDebug("[Rotation] Normal rotation");
        }
        endTime = System.currentTimeMillis() + configuration.getTime();
        configuration.setCallback(Optional.of(this::reset));
    }

    private float pythagoras(float a, float b) {
        return (float) Math.sqrt(a * a + b * b);
    }

    public Rotation getNeededChange(Rotation startRot, Vec3 target) {
        Rotation targetRot = getRotation(target);
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
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), false);
    }

    public Rotation getRotation(Entity to, boolean randomness) {
        return getRotation(mc.thePlayer.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), to.getPositionEyes(((MinecraftAccessor) mc).getTimer().renderPartialTicks), randomness);
    }

    public Rotation getRotation(Vec3 from, Vec3 to) {
        return getRotation(from, to, false);
    }

    public Rotation getRotation(BlockPos pos) {
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
        completed = false;
        rotating = false;
        configuration = null;
        startTime = 0;
        endTime = 0;
    }

    private float interpolate(float start, float end, Function<Float, Float> function) {
        float t = (float) (System.currentTimeMillis() - startTime) / (endTime - startTime);
        return (end - start) * function.apply(t) + start;
    }

    private float easeOutCubic(double number) {
        return (float) Math.max(0, Math.min(1, 1 - Math.pow(1 - number, 3)));
    }

    private float easeOutQuint(float x) {
        return (float) (1 - Math.pow(1 - x, 5));
    }

    @SubscribeEvent
    public void onLastRender(RenderWorldLastEvent event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.CLIENT)
            return;

        if (mc.currentScreen != null) {
            startTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis() + configuration.getTime();
            return;
        }
        if (System.currentTimeMillis() <= endTime) {
            mc.thePlayer.rotationYaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), this::easeOutCubic);
            mc.thePlayer.rotationPitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), this::easeOutQuint);
        } else if (!completed) {
            mc.thePlayer.rotationYaw = targetRotation.getYaw();
            mc.thePlayer.rotationPitch = targetRotation.getPitch();
            completed = true;
            rotating = false;
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
            } else {
                reset();
            }
        }
    }

    @SubscribeEvent
    public void onUpdatePre(MotionUpdateEvent.Pre event) {
        if (!rotating || configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        if (mc.currentScreen != null) {
            startTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis() + configuration.getTime();
            return;
        }

        if (System.currentTimeMillis() >= endTime) {
            if (configuration.getCallback().isPresent()) {
                configuration.getCallback().get().run();
                System.out.println(configuration);
                if (configuration == null) {
                    return;
                }
            } else {
                reset();
                return;
            }
        }

        clientSidePitch = mc.thePlayer.rotationPitch;
        clientSideYaw = mc.thePlayer.rotationYaw;

        if (configuration != null && configuration.isGoingBackToClientSide()) {
            targetRotation.setYaw(clientSideYaw);
            targetRotation.setPitch(clientSidePitch);
        } else if (configuration != null && configuration.getTarget().isPresent() && configuration.getTarget().get().getTarget().isPresent()) {
            Vec3 vec = configuration.getTarget().get().getTarget().get();
            Rotation rot = getRotation(vec);
            Rotation neededChange = getNeededChange(startRotation, rot);
            targetRotation.setYaw(startRotation.getYaw() + neededChange.getYaw());
            targetRotation.setPitch(startRotation.getPitch() + neededChange.getPitch());
        }
        event.yaw = interpolate(startRotation.getYaw(), targetRotation.getYaw(), this::easeOutCubic);
        event.pitch = interpolate(startRotation.getPitch(), targetRotation.getPitch(), this::easeOutQuint);
        serverSidePitch = event.pitch;
        serverSideYaw = event.yaw;
        mc.thePlayer.rotationYaw = event.yaw;
        mc.thePlayer.rotationPitch = event.pitch;
    }

    @SubscribeEvent
    public void onUpdatePost(MotionUpdateEvent.Post event) {
        if (!rotating) return;
        if (configuration == null || configuration.getRotationType() != RotationConfiguration.RotationType.SERVER)
            return;

        mc.thePlayer.rotationYaw = clientSideYaw;
        mc.thePlayer.rotationPitch = clientSidePitch;
    }
}
