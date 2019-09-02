package de.uniba.dsg.serverless.profiling.load;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MemoryLoad implements Runnable {

    @Expose
    public final long time;
    @Expose
    private final long totalMemory;
    private List<byte[]> usedMemory = new ArrayList<>();

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
        long tick = 200L;
        long ticks = time / tick;
        int tickMemory = (int) (totalMemory / ticks);
        long nextTick = System.currentTimeMillis() + tick;
        for (int i = 0; i < ticks; i++) {
            usedMemory.add(new byte[tickMemory]);
            Uninterruptibles.sleepUninterruptibly(nextTick - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            nextTick += tick;
        }
        System.out.println("done simulating memory load");
    }
}
