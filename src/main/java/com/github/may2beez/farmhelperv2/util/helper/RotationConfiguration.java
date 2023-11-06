package com.github.may2beez.farmhelperv2.util.helper;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.Optional;

@Getter
@Setter
public class RotationConfiguration {
    private final Minecraft mc = Minecraft.getMinecraft();
    private Rotation from;
    private Optional<Rotation> to = Optional.empty();
    private Optional<Target> target = Optional.empty();
    private long time;
    private Optional<Runnable> callback;
    @Setter
    @Getter
    private boolean goingBackToClientSide = false;

    public enum RotationType {
        SERVER,
        CLIENT
    }
    private RotationType rotationType = RotationType.CLIENT;

    public static class Target {
        private Vec3 vec;
        private Entity entity;

        public Target(Vec3 vec) {
            this.vec = vec;
        }

        public Target(Entity entity) {
            this.entity = entity;
        }

        public Target(BlockPos blockPos) {
            this.vec = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        }

        public Optional<Vec3> getTarget() {
            if (vec != null) {
                return Optional.of(vec);
            } else if (entity != null) {
                return Optional.of(entity.getPositionVector());
            } else {
                return Optional.empty();
            }
        }
    }

    public RotationConfiguration(Rotation from, Rotation to, long time, RotationType rotationType, Runnable callback) {
        this.from = from;
        this.to = Optional.ofNullable(to);
        this.time = time;
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Rotation from, Target target, long time, RotationType rotationType, Runnable callback) {
        this.from = from;
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Rotation to, long time, Runnable callback) {
        this.from = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        this.to = Optional.ofNullable(to);
        this.time = time;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Rotation to, long time, RotationType rotationType, Runnable callback) {
        this.from = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        this.to = Optional.ofNullable(to);
        this.time = time;
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Target target, long time, Runnable callback) {
        this.from = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.callback = Optional.ofNullable(callback);
    }

    public RotationConfiguration(Target target, long time, RotationType rotationType, Runnable callback) {
        this.from = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        this.time = time;
        this.target = Optional.ofNullable(target);
        this.rotationType = rotationType;
        this.callback = Optional.ofNullable(callback);
    }
}
