package com.jelly.farmhelper.utils;

public class Timer {
    public long startedAt = 0;

    public boolean hasPassed(long ms) {
        return System.currentTimeMillis() - startedAt > ms;
    }

    public void reset() {
        startedAt = 0;
    }

    public void schedule() {
        startedAt = System.currentTimeMillis();
    }

    public boolean isScheduled() {
        return startedAt != 0;
    }

    public Timer() {
        reset();
    }
}
