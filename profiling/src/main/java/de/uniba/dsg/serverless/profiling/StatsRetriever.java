package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatsRetriever {
    private final long startTime;
    private long containerStartTime = 0L;

    public static final String EXECUTOR_DOCKERFILE = "executor/Dockerfile";
    public static final String EXECUTOR_IMAGE = "mendress/executor";
    public static final String SERVICE_MOCK_DOCKERFILE = "serviceMock/Dockerfile";
    public static final String SERVICE_MOCK_IMAGE = "mendress/servicemock";

    public StatsRetriever() {
        startTime = System.currentTimeMillis();
    }

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

    public void retrieveStats() throws ProfilingException {
        ContainerProfiling serviceMock = new ContainerProfiling(SERVICE_MOCK_DOCKERFILE, SERVICE_MOCK_IMAGE);
        ContainerProfiling executor = new ContainerProfiling(EXECUTOR_DOCKERFILE, EXECUTOR_IMAGE);
        boolean build = true;
        if (build) {
            // TODO parallel builds ?
            System.out.println("building container ..");
            String imageId = serviceMock.buildContainer();
            System.out.println(imageId);
            imageId = executor.buildContainer();
            System.out.println(imageId);
        }

        Map<String, String> environment = new HashMap<>();
        environment.put("MOCK_PORT","9000");
        serviceMock.startContainer(environment);
        environment.put("MOCK_IP", serviceMock.getIpAddress());

        String containerId = executor.startContainer(environment);
        containerStartTime = executor.getStartedAt();
        System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");

        Profile p;
        //p = getProfileUsingDockerApi(profiling);
        //p = getProfileUsingControlGroups(profiling, containerId);
        p = getProfileUsingBoth(executor, containerId);


        System.out.println(p.toString());
        p.save();
        System.out.println("Profile created");
    }

    private Profile getProfileUsingBoth(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        Optional<Statistics> statistics = profiling.getNextStatistics();
        while (statistics.isPresent()) {
            Metrics apiMetrics = new Metrics(statistics.get(), containerStartTime);
            Metrics cgMetrics = controlGroupProfiling.getMetric();
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
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
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
