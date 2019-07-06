package de.uniba.dsg.serverless.functions.mixed;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Mixed implements RequestHandler<String, String> {

    public Mixed() {

    }

    @Override
    public String handleRequest(String input, Context context) {
        long loadTime = 30_000;

        CPULoad cpuLoad = new CPULoad(0, 1, loadTime);
        IOLoad ioLoad = new IOLoad(0, 10, 0, loadTime);

        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(ioLoad);
        service.submit(cpuLoad);

        try {
            Thread.sleep(loadTime);
        } catch (InterruptedException ignored) {
        }

        shutdownAndAwaitTermination(service);

        return "";
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
        }
    }


}
