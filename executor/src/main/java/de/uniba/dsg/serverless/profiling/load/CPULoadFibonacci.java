package de.uniba.dsg.serverless.profiling.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class CPULoadFibonacci implements Runnable {

    @Expose
    public final long fibonacci;

    @Override
    public void run() {
        System.out.println("starting fib");
        long startTime = System.currentTimeMillis();
        fibonacci(fibonacci);
        System.out.println("Fibonacci Load took " + (System.currentTimeMillis() - startTime) + "ms");
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
     */
    public CPULoadFibonacci(int fibonacci) {
        this.fibonacci = fibonacci;
    }

}
