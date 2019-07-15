package de.uniba.dsg.serverless.functions.mixed;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemoryLoad implements Runnable {

    private final long time;
    private final long totalMemory;
    private List<byte[]> usedMemory = new ArrayList<>();
    private double progress = 0;

    /**
     * @param memory memory in bytes
     * @param time   milliseconds
     */
    public MemoryLoad(long memory, long time) {
        this.time = time;
        this.totalMemory = memory;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time;

        // TODO improve this to make it more consistent
        while (System.currentTimeMillis() < endTime) {
            double progress = (System.currentTimeMillis() - startTime) / (1. * (endTime - startTime));
            int newLoad = (int) ((progress - this.progress) * totalMemory);
            byte[] load = new byte[newLoad];
            this.progress = progress;
            usedMemory.add(load);
            Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        }
    }
}
