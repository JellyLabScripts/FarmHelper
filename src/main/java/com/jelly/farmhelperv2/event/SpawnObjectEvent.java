package com.jelly.farmhelperv2.event;

import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SpawnObjectEvent extends Event {
    public int entityId;
    public double x;
    public double y;
    public double z;
    public Vec3 pos;
    public double speedX;
    public double speedY;
    public double speedZ;
    public float yaw;
    public float pitch;
    public int type;

    public SpawnObjectEvent(int entityId, double x, double y, double z, double speedX, double speedY, double speedZ, float yaw, float pitch, int type) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pos = new Vec3(x, y, z);
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.type = type;
    }
}
