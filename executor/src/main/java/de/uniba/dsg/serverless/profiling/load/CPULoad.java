package de.uniba.dsg.serverless.profiling.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.annotations.Expose;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CPULoad implements Runnable {

    public final int sleep;
    //@Expose
    //public final double from;
    //@Expose
    //public final double to;
    @Expose
    public final long time;


    //@Override
    //public void run() {
    //    System.out.println("simulating CPU load" + time);
    //    long startTime = System.currentTimeMillis();
    //    long endTime = startTime + time;
    //
    //    while (System.currentTimeMillis() < endTime) {
    //        if (System.currentTimeMillis() % 100 == 0) {
    //            double progress = (System.currentTimeMillis() - startTime) / (1. * (endTime - startTime));
    //            double load = from + (to - from) * progress;
    //            Uninterruptibles.sleepUninterruptibly((long) Math.floor((1 - load) * 100), TimeUnit.MILLISECONDS);
    //        }
    //    }
    //}

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        fibonacci(44);
        System.out.println(System.currentTimeMillis() - startTime);
    }

    private long fibonacci(long n) {
        if (n <= 1) {
            return 1;
        } else {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }

    /**
     * CPULoad which runs for a defined amount of time.
     * <p>
     * Example: <code>CPULoad(0.5,1.0,10_000)</code> will create a load which goes from 50% CPU usage to 100% linearly in 10 seconds.
     *
     * @param from start load as a percentage
     * @param to   end load as a percentage
     * @param time total running time
     */
    public CPULoad(int sleep, long time) {
        this.sleep = Math.max(0, sleep);
        //this.from = Math.max(0., Math.min(from, 1.));
        //this.to = Math.max(0., Math.min(to, 1.));
        this.time = time;
    }

    public static CPULoad fromMap(Map<String, String> map) {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, CPULoad.class);
    }

}
