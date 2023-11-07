package com.github.may2beez.farmhelperv2.event;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.time.LocalDateTime;

public class MillisecondEvent extends Event {
    public LocalDateTime dateTime;
    public long timestamp;

    public MillisecondEvent() {
        timestamp = System.currentTimeMillis();
        dateTime = LocalDateTime.now();
    }
}
