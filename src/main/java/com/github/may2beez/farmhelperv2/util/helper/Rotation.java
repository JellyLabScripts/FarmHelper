package com.github.may2beez.farmhelperv2.util.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setRotation(Rotation rotation) {
        this.yaw = rotation.getYaw();
        this.pitch = rotation.getPitch();
    }

    @Override
    public String toString() {
        return "Rotation{" +
                "yaw=" + yaw +
                ", pitch=" + pitch +
                '}';
    }
}
