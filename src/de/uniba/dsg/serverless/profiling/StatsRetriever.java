package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.docker.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.model.MemoryMetrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatsRetriever {

    private static final Path CONTROL_GROUP = Paths.get("/sys", "fs", "cgroup");
    private static final Path DOCKER_MEMORY_GROUP = CONTROL_GROUP.resolve(Paths.get("memory", "docker"));
    private static final Path DOCKER_CPU_GROUP = CONTROL_GROUP.resolve(Paths.get("cpuacct", "docker"));

    private static final Path MEMORY_STAT = Paths.get("memory.stat");

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
        String containerId = profiling.startContainer();
        System.out.println("Container (id=" + containerId + ") started.");

        List<Statistics> stats = profiling.logStatistics();
        InspectContainerResponse additional = profiling.inspectContainer();
        List<MemoryMetrics> memoryMetrics = new ArrayList<>();
        String started = additional.getState().getStartedAt();
        for (Statistics s : stats) {
            memoryMetrics.add(new MemoryMetrics(s, started));
        }
        Profile p = new Profile(memoryMetrics, additional);
        System.out.println(p.toString());
        p.save();
        System.out.println("Profile created");
    }

    private MemoryMetrics getMemoryMetrics(String containerId, long startTime) throws ProfilingException {
        try {
            Path path = getContainerLogsPath(containerId);
            List<String> lines = Files.readAllLines(path.resolve(MEMORY_STAT));
            long currentTime = System.currentTimeMillis() - startTime;
            return new MemoryMetrics(lines, currentTime);
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }

    private Path getContainerLogsPath(String containerId) throws ProfilingException {
        Path path = DOCKER_MEMORY_GROUP.resolve(containerId);
        if (!Files.exists(path)) {
            throw new ProfilingException("Path " + path + " does not exist.");
        }
        if (!Files.isDirectory(path)) {
            throw new ProfilingException("Path " + path + " is not a directory.");
        }
        if (!Files.isReadable(path)) {
            throw new ProfilingException("Folder " + path + " is not readable.");
        }
        return path;
    }


}
