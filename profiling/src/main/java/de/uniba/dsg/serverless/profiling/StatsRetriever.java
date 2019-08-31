package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public class StatsRetriever {
    private long containerStartTime = 0L;

    public static final Path OUTPUT_FOLDER = Paths.get("profiles");
    public final Path profileFolder;
    private final String profileName;

    private static final String EXECUTOR_DOCKERFILE = "executor/Dockerfile";
    private static final String EXECUTOR_IMAGE = "mendress/executor";

    private static final String SERVICE_MOCK_DOCKERFILE = "serviceMock/Dockerfile";
    private static final String SERVICE_MOCK_IMAGE = "mendress/servicemock";

    private ContainerProfiling serviceMock;
    private ContainerProfiling executor;

    public StatsRetriever(String profileName) throws ProfilingException {
        this(profileName, true);
    }

    public StatsRetriever(String profileName, boolean withBuild) throws ProfilingException {
        this.profileName = profileName;
        profileFolder = OUTPUT_FOLDER.resolve(profileName);

        serviceMock = new ContainerProfiling(SERVICE_MOCK_DOCKERFILE, SERVICE_MOCK_IMAGE);
        executor = new ContainerProfiling(EXECUTOR_DOCKERFILE, EXECUTOR_IMAGE);
        if (withBuild) {
            System.out.println("Building containers...");
            serviceMock.buildContainer();
            executor.buildContainer();
        }
    }

    public void profile(Map<String, String> loadPattern, ResourceLimits limits, int numberOfProfiles) throws ProfilingException {
        Path profilePath = profileFolder.resolve("profile_" + limits.getMemoryLimitInMb());
        if (Files.exists(profilePath)) {
            System.out.println("Profile has already been created.");
            return;
        }

        loadPattern.put("MOCK_PORT", "9000");
        try {
            serviceMock.startContainer(loadPattern);
            loadPattern.put("MOCK_IP", serviceMock.getIpAddress());
            for (int p = 0; p < numberOfProfiles; p++) {
                Profile profile = getProfile(loadPattern, limits);
                profile.save(profilePath.resolve(String.valueOf(p)));
            }
        } finally {
            serviceMock.kill();
        }
    }

    private Profile getProfile(Map<String, String> environment, ResourceLimits limits) throws ProfilingException {
        String containerId = executor.startContainer(environment, limits);
        containerStartTime = System.currentTimeMillis();
        System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");
        return getProfileUsingDockerApi(executor);
    }

    private Profile getProfileUsingBoth(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        Optional<Statistics> statistics = profiling.getNextStatistics();
        while (statistics.isPresent()) {
            Metrics apiMetrics = new Metrics(statistics.get(), containerStartTime);
            Metrics cgMetrics = controlGroupProfiling.getMetric();
            // delay between metrics is ~max 3 ms
            cgMetrics.addMetrics(apiMetrics);
            metrics.add(cgMetrics);
            statistics = profiling.getNextStatistics();
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }

    private Profile getProfileUsingDockerApi(ContainerProfiling profiling) throws ProfilingException {
        List<Statistics> stats = profiling.logStatistics();
        List<Metrics> metrics = new ArrayList<>();
        for (Statistics s : stats) {
            metrics.add(new Metrics(s, containerStartTime));
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }
}
