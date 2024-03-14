package com.jelly.farmhelperv2.event;

import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class UpdateScoreboardLineEvent extends Event {
    private final String line;

    public UpdateScoreboardLineEvent(String line) {
        this.line = line;
    }

}
