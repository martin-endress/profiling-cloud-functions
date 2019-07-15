package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

public class ProfileMetaInfo {

    public final String created;
    public final HostConfig hostConfig;
    public final String imageId;
    public final InspectContainerResponse.ContainerState state;
    public final long durationMS;
    public final double cpuUtilisation;
    public final double memoryLimit;
    public final double averageMemoryUtilization;

    /**
     * Creates Profile Meta data which are parsed to json
     *
     * @param profile profile (containing additional Information
     * @throws ProfilingException when the profile.state.time or profile.metrics is corrupt
     */
    public ProfileMetaInfo(Profile profile) throws ProfilingException {
        InspectContainerResponse additional = profile.additional;
        created = additional.getCreated();
        hostConfig = additional.getHostConfig();
        imageId = additional.getImageId();
        state = additional.getState();
        durationMS = MetricsUtil.timeDifference(state.getStartedAt(), state.getFinishedAt());
        if (profile.lastMetrics.containsMetric(Metrics.STATS_TOTAL_CPU_USAGE)) {
            cpuUtilisation = calculateCpuUtilization(profile);
            memoryLimit = profile.lastMetrics.getMetric(Metrics.MEMORY_LIMIT);
            averageMemoryUtilization = getAverageMemoryUtilization(profile);
        } else {
            // no stats present
            cpuUtilisation = 0;
            memoryLimit = 0;
            averageMemoryUtilization = 0;
        }
    }

    private double calculateCpuUtilization(Profile profile) throws ProfilingException {
        long cpuQuota = hostConfig.getCpuQuota();
        cpuQuota = (cpuQuota == 0) ? ContainerProfiling.DEFAULT_CPU_PERIOD : cpuQuota; // 0 means no limit
        long statsTotalCpuMS = profile.lastMetrics.getMetric(Metrics.STATS_TOTAL_CPU_USAGE) / 1_000_000; // ns -> ms
        double availableTime = (durationMS * (cpuQuota * 1.0 / ContainerProfiling.DEFAULT_CPU_PERIOD));
        return statsTotalCpuMS / availableTime;
    }

    private double getAverageMemoryUtilization(Profile profile) throws ProfilingException {
        double averageUsage = profile.metrics.stream()
                .map(m -> m.getOrDefault(Metrics.MEMORY_USAGE, 0L))
                .filter(a -> a != 0L)
                .mapToDouble(a -> a)
                .average()
                .orElseThrow(() -> new ProfilingException("Average memory usage could not be calculated."));
        return averageUsage / memoryLimit;
    }
}
