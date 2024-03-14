package com.jelly.farmhelperv2.event;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

public class UpdateScoreboardListEvent extends Event {
    public final List<String> scoreboardLines;
    public final List<String> cleanScoreboardLines;
    public final long timestamp;

    public UpdateScoreboardListEvent(List<String> scoreboardLines, List<String> cleanScoreboardLines, long timestamp) {
        this.scoreboardLines = scoreboardLines;
        this.cleanScoreboardLines = cleanScoreboardLines;
        this.timestamp = timestamp;
    }
}
