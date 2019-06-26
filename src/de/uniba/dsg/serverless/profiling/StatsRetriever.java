package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.docker.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.model.*;

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
    private static final Path CPU_STAT = Paths.get("cpuacct.stat");

    private final long startTime;
    private long containerStartTime = 0L;

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
        ContainerProfiling profiling = new ContainerProfiling();
        String containerId = profiling.startContainer("JAVA_PARAMS=46");
        System.out.println("Container (id=" + containerId + ") started.");
        containerStartTime = System.currentTimeMillis();


        Profile p = getProfileUsingDockerApi(profiling);
        //Profile p = getProfileUsingControlGroups(profiling,containerId);

        System.out.println(p.toString());
        p.save();
        System.out.println("Profile created");
    }

    private Profile getProfileUsingControlGroups(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        Path cpuLogs = getCPULogsPath(containerId);
        Path memoryLogs = getMemoryLogsPath(containerId);
        while (metricsAvailable(memoryLogs)) {
            metrics.add(getMetrics(cpuLogs, memoryLogs));
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }

    private Profile getProfileUsingDockerApi(ContainerProfiling profiling) throws ProfilingException {
        List<Statistics> stats = profiling.logStatistics();
        List<Metrics> metrics = new ArrayList<>();
        for (Statistics s : stats) {
            metrics.add(new Metrics(s, 0));
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }

    private Metrics getMetrics(Path cpuPath, Path memoryPath) throws ProfilingException {
        long currentTime = System.currentTimeMillis() - containerStartTime;
        try {
            List<String> lines = Files.readAllLines(cpuPath);
            lines.addAll(Files.readAllLines(memoryPath));
            return new Metrics(lines, currentTime);
        } catch (IOException e) {
            throw new ProfilingException("Could not read file.", e);
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

    private Path getCPULogsPath(String containerId) {
        return DOCKER_CPU_GROUP.resolve(containerId).resolve(CPU_STAT);
    }

    private Path getMemoryLogsPath(String containerId) {
        return DOCKER_MEMORY_GROUP.resolve(containerId).resolve(MEMORY_STAT);
    }


}
