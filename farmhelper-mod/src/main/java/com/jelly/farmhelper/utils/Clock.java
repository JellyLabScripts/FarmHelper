package com.jelly.farmhelper.utils;

public class Clock {
    private long endTime;
    private boolean scheduled;

    public void schedule(long milliseconds) {
        this.endTime = System.currentTimeMillis() + milliseconds;
        scheduled = true;
    }

    public long getRemainingTime() {
        return endTime - System.currentTimeMillis();
    }
    public long getEndTime() {
        return endTime;
    }

    public boolean passed() {
        return System.currentTimeMillis() >= endTime;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void reset() {
        scheduled = false;
    }
}
