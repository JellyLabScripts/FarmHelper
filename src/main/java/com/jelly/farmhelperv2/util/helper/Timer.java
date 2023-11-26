package com.jelly.farmhelperv2.util.helper;

public class Timer {
    public long startedAt = 0;
    public long pausedAt = 0;
    public boolean paused = false;

    public boolean hasPassed(long ms) {
        return System.currentTimeMillis() - startedAt > ms;
    }

    public void reset() {
        startedAt = 0;
        pausedAt = 0;
        paused = false;
    }

    public void schedule() {
        startedAt = System.currentTimeMillis();
        pausedAt = 0;
        paused = false;
    }

    public void pause() {
        if (startedAt != 0 && !paused) {
            pausedAt = System.currentTimeMillis();
            paused = true;
        }
    }

    public void resume() {
        if (startedAt != 0 && paused) {
            startedAt += System.currentTimeMillis() - pausedAt;
            paused = false;
        }
    }

    public boolean isScheduled() {
        return startedAt != 0;
    }

    public long getElapsedTime() {
        if (startedAt == 0) {
            return 0;
        }
        if (paused) {
            return pausedAt - startedAt;
        }
        return System.currentTimeMillis() - startedAt;
    }

    public Timer() {
        reset();
    }
}
