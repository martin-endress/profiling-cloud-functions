package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class StatsRetriever {
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
            Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .ifPresent(System.err::println);
        }
    }

    public void retrieveStats() throws ProfilingException {
        ContainerProfiling profiling = new ContainerProfiling();
        String containerId = profiling.startContainer("JAVA_PARAMS=46");
        containerStartTime = profiling.getStartedAt();
        System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");

        Profile p;
        //p = getProfileUsingDockerApi(profiling);
        //p = getProfileUsingControlGroups(profiling, containerId);
        p = getProfileUsingBoth(profiling, containerId);


        System.out.println(p.toString());
        p.save();
        System.out.println("Profile created");
    }

    private Profile getProfileUsingBoth(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        Optional<Statistics> statistics = profiling.getNextStatistics();
        while (statistics.isPresent()) {
            Metrics apiMetrics = new Metrics(statistics.get(), 0L);
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
            metrics.add(new Metrics(s, 0));
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }
}
