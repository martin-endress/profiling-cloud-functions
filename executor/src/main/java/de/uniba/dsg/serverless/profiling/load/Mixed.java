package de.uniba.dsg.serverless.profiling.load;

import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.executor.ProfilingException;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Mixed {

    private Optional<CPULoad> cpuLoad;
    private Optional<MemoryLoad> memoryLoad;
    private Optional<IOLoad> ioLoad;

    private long loadTime;

    public Mixed() throws ProfilingException {
        String time = System.getenv("LOAD_TIME");
        loadTime = Long.valueOf(time);
        cpuLoad = getCpuLoad();
        memoryLoad = getMemoryLoad();
        ioLoad = getIoLoad();
    }


    public void simulateLoad() {
        ExecutorService service = Executors.newFixedThreadPool(3);

        cpuLoad.ifPresent(service::submit);
        memoryLoad.ifPresent(service::submit);
        ioLoad.ifPresent(service::submit);

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

    private Optional<CPULoad> getCpuLoad() {
        try {
            double cpuFrom = Double.valueOf(System.getenv("CPU_FROM"));
            double cpuTo = Double.valueOf(System.getenv("CPU_TO"));
            return Optional.of(new CPULoad(cpuFrom, cpuTo, loadTime));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<MemoryLoad> getMemoryLoad() {
        try {
            long memoryTo = Long.valueOf(System.getenv("MEMORY_TO"));
            return Optional.of(new MemoryLoad(memoryTo, loadTime));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<IOLoad> getIoLoad() throws ProfilingException {
        try {
            int ioFrom = Integer.valueOf(System.getenv("IO_FROM"));
            int ioTo = Integer.valueOf(System.getenv("IO_TO"));
            int ioSize = Integer.valueOf(System.getenv("IO_SIZE"));
            return Optional.of(new IOLoad(ioFrom, ioTo, ioSize, loadTime));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
