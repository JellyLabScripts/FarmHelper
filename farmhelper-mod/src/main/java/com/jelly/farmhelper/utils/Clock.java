package com.jelly.farmhelper.utils;

public class Clock {
    private long remainingTime;
    private boolean paused;
    private boolean scheduled;
    private long endTime;

    public void schedule(long milliseconds) {
        this.endTime = System.currentTimeMillis() + milliseconds;
        this.remainingTime = milliseconds;
        this.scheduled = true;
        this.paused = false;
    }

    public long getRemainingTime() {
        if (paused) {
            return remainingTime;
        }
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
    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        if (scheduled && !paused) {
            remainingTime = endTime - System.currentTimeMillis();
            paused = true;
        }
    }

    public void resume() {
        if (scheduled && paused) {
            endTime = System.currentTimeMillis() + remainingTime;
            paused = false;
        }
    }

    public void reset() {
        scheduled = false;
        paused = false;
        endTime = 0;
        remainingTime = 0;
    }
}
