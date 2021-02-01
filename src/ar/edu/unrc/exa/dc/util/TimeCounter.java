package ar.edu.unrc.exa.dc.util;

import java.util.concurrent.TimeUnit;

public class TimeCounter {

    private boolean running = false;
    private long time = 0;
    private long totalTime = 0;

    public void clockStart() {
        if (running)
            throw new IllegalStateException("Time counter already running");
        this.time = System.nanoTime();
        this.running = true;
    }

    public void clockEnd() {
        if (!running)
            throw new IllegalStateException("Time counter is not running");
        this.totalTime += System.nanoTime() - this.time;
        this.running = false;
    }

    public long toSeconds() {
        return TimeUnit.NANOSECONDS.toSeconds(totalTime);
    }

    public long toMilliSeconds() {
        return TimeUnit.NANOSECONDS.toMillis(totalTime);
    }

}
