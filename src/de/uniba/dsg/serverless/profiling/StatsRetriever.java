package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.docker.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.model.MemoryMetrics;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StatsRetriever {

    private static final Path CONTROL_GROUP = Paths.get("/sys", "fs", "cgroup");
    private static final Path DOCKER_MEMORY_GROUP = CONTROL_GROUP.resolve(Paths.get("memory", "docker"));
    private static final Path DOCKER_CPU_GROUP = CONTROL_GROUP.resolve(Paths.get("cpuacct", "docker"));

    private static final Path MEMORY_STAT = Paths.get("memory.stat");

    private final long startTime;

    public StatsRetriever() {
        startTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {
        try {
            new StatsRetriever().retrieveStats();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            System.err.println(Optional
                    .ofNullable(e.getCause())
                    .map(c -> c.getMessage())
                    .orElse(""));
            return;
        }
    }

    public void retrieveStats() throws ProfilingException {
        long startTime = System.currentTimeMillis();
        ContainerProfiling profiling = new ContainerProfiling();
        String containerId = profiling.startContainer();
        System.out.println("Container (id=" + containerId + ") started.");

        Path memoryLogs = getMemoryLogsPath(containerId);
        List<Metrics> metrics = new ArrayList<>();
        while (metricsAvailable(memoryLogs)) {
            metrics.add(getMemoryMetrics(memoryLogs));
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        InspectContainerResponse additional = profiling.inspectContainer();
        Profile p = new Profile(metrics, additional);

        //List<Statistics> stats = profiling.logStatistics();
        //List<MemoryMetrics> memoryMetrics = new ArrayList<>();
        //String started = additional.getState().getStartedAt();
        //for (Statistics s : stats) {
        //    memoryMetrics.add(new MemoryMetrics(s, started));
        //}
        System.out.println(p.toString());
        p.save();
        System.out.println("Profile created");
    }

    private MemoryMetrics getMemoryMetrics(Path path) throws ProfilingException {
        long currentTime = System.currentTimeMillis() - startTime;
        try {
            List<String> lines = Files.readAllLines(path);
            return new MemoryMetrics(lines, currentTime);
        } catch (IOException e) {
            throw new ProfilingException("Could not read file " + path + ".", e);
        }
    }

    private boolean metricsAvailable(Path path) throws ProfilingException {
        if (!Files.exists(path)) {
            return false;
        } else if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ProfilingException("Metrics path must be a readable file.");
        } else {
            return true;
        }
    }

    private Path getMemoryLogsPath(String containerId) {
        return DOCKER_MEMORY_GROUP.resolve(containerId).resolve(MEMORY_STAT);
    }

    private Path getCPULogsPath(String containerId) {
        return DOCKER_CPU_GROUP.resolve(containerId).resolve(MEMORY_STAT);
    }


}
