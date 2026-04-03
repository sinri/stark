package io.github.sinri.stark.core;

public class RepeatStopper {
    private volatile boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    public boolean shouldStop() {
        return stopped;
    }
}
