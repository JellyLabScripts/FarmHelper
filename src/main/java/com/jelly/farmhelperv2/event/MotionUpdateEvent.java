package com.jelly.farmhelperv2.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class MotionUpdateEvent extends Event {
    public float yaw;
    public float pitch;

    protected MotionUpdateEvent(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Cancelable
    public static class Pre extends MotionUpdateEvent {
        public Pre(final float yaw, final float pitch) {
            super(yaw, pitch);
        }
    }

    @Cancelable
    public static class Post extends MotionUpdateEvent {
        public Post(final float yaw, final float pitch) {
            super(yaw, pitch);
        }
    }
}
