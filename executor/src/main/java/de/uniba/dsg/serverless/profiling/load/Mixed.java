package de.uniba.dsg.serverless.profiling.load;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;
import de.uniba.dsg.serverless.profiling.executor.ProfilingException;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Mixed {

    private final Load load;
    private long loadTime;
    private int loads;

    public Mixed(Load load) {
        this.load = load;
        calculateInfo();
    }

    public Mixed(Path loadFile) throws ProfilingException {
        this.load = Load.fromFile(loadFile);
        calculateInfo();
    }

    private void calculateInfo() {
        long cpuTime = load.cpuLoad != null ? load.cpuLoad.time : 0L;
        long memoryTime = load.memoryLoad != null ? load.memoryLoad.time : 0L;
        long ioTime = load.networkLoad != null ? load.networkLoad.time : 0L;
        loadTime = Math.max(cpuTime, Math.max(memoryTime, ioTime));

        loads = (int) Stream.of(load.cpuLoad, load.memoryLoad, load.networkLoad).filter(Objects::nonNull).count();
    }

    public void simulateLoad() {
        ExecutorService service = Executors.newFixedThreadPool(loads);
        if (load.cpuLoad != null) {
            service.submit(load.cpuLoad);
        }
        if (load.memoryLoad != null) {
            service.submit(load.memoryLoad);
        }
        if (load.networkLoad != null) {
            service.submit(load.networkLoad);
        }

        Uninterruptibles.sleepUninterruptibly(loadTime, TimeUnit.MILLISECONDS);
        shutdownAndAwaitTermination(service);
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
