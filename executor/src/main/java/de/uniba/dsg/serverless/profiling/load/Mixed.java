package de.uniba.dsg.serverless.profiling.load;

import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.executor.ProfilingException;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Mixed {

    private Optional<CPULoad> cpuLoad;
    private Optional<CPULoadFibonacci> cpuLoadFibonacci;
    private Optional<MemoryLoad> memoryLoad;
    private Optional<IOLoad> ioLoad;

    private long loadTime;

    public Mixed() throws ProfilingException {
        loadTime = getLoadTime();
        cpuLoad = getCpuLoad();
        cpuLoadFibonacci = getCpuLoadFibonacci();
        memoryLoad = getMemoryLoad();
        ioLoad = getIoLoad();
    }

    private long getLoadTime() {
        String loadTime = System.getenv("LOAD_TIME");
        if (loadTime!=null) {
            try {
                return Long.parseLong(loadTime);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    public void simulateLoad() {
        ExecutorService service = Executors.newFixedThreadPool(4);

        cpuLoad.ifPresent(service::submit);
        cpuLoadFibonacci.ifPresent(service::submit);
        memoryLoad.ifPresent(service::submit);
        ioLoad.ifPresent(service::submit);
        if (cpuLoad.isPresent() || memoryLoad.isPresent() || ioLoad.isPresent()) {
            Uninterruptibles.sleepUninterruptibly(loadTime, TimeUnit.MILLISECONDS);
        }
        shutdownAndAwaitTermination(service);
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
        }
    }

    private Optional<CPULoad> getCpuLoad() {
        try {
            double cpuFrom = Double.valueOf(System.getenv("CPU_FROM"));
            double cpuTo = Double.valueOf(System.getenv("CPU_TO"));
            return Optional.of(new CPULoad(cpuFrom, cpuTo, loadTime));
        } catch (NullPointerException | NumberFormatException e) {
            return Optional.empty();
        }
    }


    private Optional<CPULoadFibonacci> getCpuLoadFibonacci() {
        try {
            int fibonacci = Integer.valueOf(System.getenv("CPU_FIBONACCI"));
            return Optional.of(new CPULoadFibonacci(fibonacci, loadTime));
        } catch (NullPointerException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<MemoryLoad> getMemoryLoad() {
        try {
            long memoryTo = Long.valueOf(System.getenv("MEMORY_TO"));
            return Optional.of(new MemoryLoad(memoryTo, loadTime));
        } catch (NullPointerException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<IOLoad> getIoLoad() throws ProfilingException {
        try {
            int ioFrom = Integer.valueOf(System.getenv("IO_FROM"));
            int ioTo = Integer.valueOf(System.getenv("IO_TO"));
            int ioSize = Integer.valueOf(System.getenv("IO_SIZE"));
            return Optional.of(new IOLoad(ioFrom, ioTo, ioSize, loadTime));
        } catch (NullPointerException | NumberFormatException e) {
            return Optional.empty();
        }
    }

}
