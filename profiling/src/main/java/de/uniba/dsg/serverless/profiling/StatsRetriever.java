package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;

import java.nio.file.Path;
import java.util.*;

public class StatsRetriever {
    private long containerStartTime = 0L;

    private final Path outputFoder;
    private final String profileName;

    private static final String EXECUTOR_DOCKERFILE = "executor/Dockerfile";
    private static final String EXECUTOR_IMAGE = "mendress/executor";

    private static final String SERVICE_MOCK_DOCKERFILE = "serviceMock/Dockerfile";
    private static final String SERVICE_MOCK_IMAGE = "mendress/servicemock";

    public StatsRetriever(Path outputFoder, String profileName) {
        this.outputFoder = outputFoder;
        this.profileName = profileName;
    }

    public void retrieveStats() throws ProfilingException {
        final Path profileFolder = outputFoder.resolve(profileName);

        ContainerProfiling serviceMock = new ContainerProfiling(SERVICE_MOCK_DOCKERFILE, SERVICE_MOCK_IMAGE);
        ContainerProfiling executor = new ContainerProfiling(EXECUTOR_DOCKERFILE, EXECUTOR_IMAGE);

        serviceMock.buildContainer();
        executor.buildContainer();

        Map<String, String> environment = getParameters();
        environment.put("MOCK_PORT", "9000");
        serviceMock.startContainer(environment);
        environment.put("MOCK_IP", serviceMock.getIpAddress());

        ResourceLimits limits = ResourceLimits.fromFile("limits.json");

        List<Profile> profiles = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            String containerId = executor.startContainer(environment, limits);
            containerStartTime = System.currentTimeMillis();
            System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");
            Profile p = getProfileUsingDockerApi(executor);
            p.save(profileFolder.resolve("profile" + i));
            profiles.add(p);
        }
        serviceMock.kill();
    }

    private Map<String, String> getParameters() {
        Map<String, String> map = new HashMap<>();
        map.put("LOAD_TIME", "10000");
        map.put("CPU_FROM", "1");
        map.put("CPU_TO", "1");
        //map.put("MEMORY_TO", String.valueOf(1024 * 1024 * 1024)); // 1 gb
        //map.put("IO_FROM", "1");
        //map.put("IO_TO", "2");
        //map.put("IO_SIZE", "5000");
        return map;
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
