package de.uniba.dsg.serverless.functions.mixed;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Mixed implements RequestHandler<String, String> {

    public Mixed() {

    }

    @Override
    public String handleRequest(String input, Context context) {
        long loadTime = 30_000;
        long megaByte = 1_048_576L;

        CPULoad cpuLoad = new CPULoad(0, 1, loadTime);
        IOLoad ioLoad = new IOLoad(0, 60, 10_000, loadTime);
        MemoryLoad memLoad = new MemoryLoad(512 * megaByte, loadTime);

        ExecutorService service = Executors.newFixedThreadPool(1);
        //service.submit(ioLoad);
        //service.submit(cpuLoad);
        service.submit(memLoad);


        Uninterruptibles.sleepUninterruptibly(loadTime, TimeUnit.MILLISECONDS);
        shutdownAndAwaitTermination(service);

        return "";
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
        }
    }


}
