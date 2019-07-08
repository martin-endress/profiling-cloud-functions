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
        long loadTime = 120_000;

        CPULoad cpuLoad = new CPULoad(1, 0, loadTime);
        IOLoad ioLoad = new IOLoad(10, 20, 100_000, loadTime);

        ExecutorService service = Executors.newFixedThreadPool(2);
        //service.submit(ioLoad);
        service.submit(cpuLoad);


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
