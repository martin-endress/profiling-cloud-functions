package de.uniba.dsg.serverless.profiling.profiling;

import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ControlGroupProfiling {

    private final String containerId;


    private static final Path CONTROL_GROUP = Paths.get("/sys", "fs", "cgroup");
    private static final Path DOCKER_MEMORY_GROUP = CONTROL_GROUP.resolve(Paths.get("memory", "docker"));
    private static final Path DOCKER_CPU_GROUP = CONTROL_GROUP.resolve(Paths.get("cpuacct", "docker"));

    private static final Path MEMORY_STAT = Paths.get("memory.stat");
    private static final Path CPU_STAT = Paths.get("cpuacct.stat");

    private final Path cpuLogs;
    private final Path memoryLogs;

    private final long containerStartTime;

    public ControlGroupProfiling(String containerId, long containerStartTime) {
        this.containerId = containerId;
        this.containerStartTime = containerStartTime;

        cpuLogs = getCPULogsPath(containerId);
        memoryLogs = getMemoryLogsPath(containerId);
    }

    public Metrics getMetric() throws ProfilingException {
        long currentTime = System.currentTimeMillis() - containerStartTime;
        try {
            List<String> lines = Files.readAllLines(cpuLogs);
            lines.addAll(Files.readAllLines(memoryLogs));
            return new Metrics(lines, currentTime);
        } catch (IOException e) {
            throw new ProfilingException("Could not read file.", e);
        }
    }

    public boolean metricsAvailable() throws ProfilingException {
        if (!Files.exists(cpuLogs)) {
            return false;
        } else if (!Files.isRegularFile(cpuLogs) || !Files.isReadable(cpuLogs)) {
            throw new ProfilingException("Metrics path must be a readable file.");
        } else {
            return true;
        }
    }

    private Path getCPULogsPath(String containerId) {
        return DOCKER_CPU_GROUP.resolve(containerId).resolve(CPU_STAT);
    }

    private Path getMemoryLogsPath(String containerId) {
        return DOCKER_MEMORY_GROUP.resolve(containerId).resolve(MEMORY_STAT);
    }

}
