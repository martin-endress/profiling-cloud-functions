package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatsRetriever {
    private long containerStartTime = 0L;

    public static final String EXECUTOR_DOCKERFILE = "executor/Dockerfile";
    public static final String EXECUTOR_IMAGE = "mendress/executor";
    public static final String SERVICE_MOCK_DOCKERFILE = "serviceMock/Dockerfile";
    public static final String SERVICE_MOCK_IMAGE = "mendress/servicemock";
    public static final String PYTHON_PLOTTER_IMAGE = "mendress/pythonplotter";

    public static void main(String[] args) {
        try {
            new StatsRetriever().retrieveStats();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .ifPresent(System.err::println);
            e.printStackTrace();
        }
    }

    private void retrieveStats() throws ProfilingException {
        ContainerProfiling serviceMock = new ContainerProfiling(SERVICE_MOCK_DOCKERFILE, SERVICE_MOCK_IMAGE);
        ContainerProfiling executor = new ContainerProfiling(EXECUTOR_DOCKERFILE, EXECUTOR_IMAGE);

        //build(serviceMock);
        //build(executor);

        Map<String, String> environment = getParameters();
        environment.put("MOCK_PORT", "9000");
        serviceMock.startContainer(environment);
        environment.put("MOCK_IP", serviceMock.getIpAddress());

        ResourceLimits limits = ResourceLimits.fromFile("limits.json");


        List<Profile> profiles = new ArrayList<>();
        for(int i=0;i<150;i++){
            String containerId = executor.startContainer(environment, limits);
            containerStartTime = System.currentTimeMillis();
            System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");
            Profile p = getProfileUsingBoth(executor, containerId);
            profiles.add(p);
            p.save(Paths.get("run3"));
        }

        serviceMock.kill();
        System.out.println("Profile created");
    }

    private Map<String, String> getParameters() {
        Map<String, String> map = new HashMap<>();
        map.put("LOAD_TIME", "3000");
        map.put("CPU_FROM", "0.2");
        map.put("CPU_TO", "0.4");
        map.put("MEMORY_TO", "5000");
        map.put("IO_FROM", "1");
        map.put("IO_TO", "2");
        map.put("IO_SIZE", "5000");
        return map;
    }


    private void plotProfile() throws ProfilingException {
        ContainerProfiling plotter = new ContainerProfiling("", PYTHON_PLOTTER_IMAGE);
    }

    private void build(ContainerProfiling c) throws ProfilingException {
        System.out.println("building container: " + c.imageName);
        System.out.println("finished: " + c.buildContainer());

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

    private Profile getProfileUsingControlGroups(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        while (controlGroupProfiling.metricsAvailable()) {
            metrics.add(controlGroupProfiling.getMetric());
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
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
