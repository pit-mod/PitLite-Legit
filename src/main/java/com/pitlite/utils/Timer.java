package com.pitlite.utils;

public class Timer {
    private long lastMS = System.currentTimeMillis();

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - this.lastMS > time) {
            if (reset) {
                reset();
            }
            return true;
        }
        return false;
    }

    public long getPassed() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }
}
