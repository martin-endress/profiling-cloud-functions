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
    private byte[] usedMemory;
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
        //long startTime = System.currentTimeMillis();
        //long endTime = startTime + time;


        // TODO improve this to make it more consistent
        //while (System.currentTimeMillis() < endTime) {
        //double progress = (System.currentTimeMillis() - startTime) / (1. * (endTime - startTime));
        //int newLoad = (int) ((progress - this.progress) * totalMemory);
        byte[] load = new byte[(int) totalMemory];
        //this.progress = progress;
        //usedMemory.add(load);
        Uninterruptibles.sleepUninterruptibly(time, TimeUnit.MILLISECONDS);
        //}
    }
}
