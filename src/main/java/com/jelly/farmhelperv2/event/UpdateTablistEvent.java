package com.jelly.farmhelperv2.event;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

public class UpdateTablistEvent extends Event {
    public final List<String> tablist;
    public final long timestamp;

    public UpdateTablistEvent(List<String> tablist, long timestamp) {
        this.tablist = tablist;
        this.timestamp = timestamp;
    }
}
