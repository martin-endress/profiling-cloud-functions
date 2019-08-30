package de.uniba.dsg.serverless.profiling.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class CPULoadFibonacci implements Runnable {

    @Expose
    public final long fibonacci;
    @Expose
    public final long time;

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        fibonacci(fibonacci);
        System.out.println("took " + (System.currentTimeMillis() - startTime) + "ms");
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
     * @param fibonacci input to load simulation
     * @param time      total running time
     */
    public CPULoadFibonacci(int fibonacci, long time) {
        this.fibonacci = fibonacci;
        this.time = time;
    }

    public static CPULoadFibonacci fromMap(Map<String, String> map) {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, CPULoadFibonacci.class);
    }

}
