package com.jelly.farmhelper.utils;

public class Clock {
    private long endTime;
    private boolean scheduled;

    public void schedule(long milliseconds) {
        this.endTime = System.currentTimeMillis() + milliseconds;
        scheduled = true;
    }

    public boolean passed() {
        return System.currentTimeMillis() >= endTime && scheduled;
    }

    public void reset() {
        scheduled = false;
    }
}
