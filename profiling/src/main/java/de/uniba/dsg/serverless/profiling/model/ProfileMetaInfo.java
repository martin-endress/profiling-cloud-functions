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
    public double cpuUtilisation; // relative to the quota
    public double maxCpuUtilisation; // maximum cpu utilization
    public long maxMemoryUtilization;
    public double memoryLimit;
    public double averageMemoryUtilization;
    public double networkUsage;

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
            maxCpuUtilisation = calculateMaxCpuUtilisation(profile);
            memoryLimit = profile.lastMetrics.getMetric(Metrics.MEMORY_LIMIT);
            maxMemoryUtilization = getMaxMemoryUtilization(profile);
            averageMemoryUtilization = getAverageMemoryUtilization(profile);
            networkUsage = getNetworkUsage(profile);
        }
    }

    private double calculateCpuUtilization(Profile profile) throws ProfilingException {
        long cpuQuota = hostConfig.getCpuQuota();
        cpuQuota = (cpuQuota == 0) ? ContainerProfiling.DEFAULT_CPU_PERIOD : cpuQuota; // 0 means no limit
        long statsTotalCpuMS = profile.lastMetrics.getMetric(Metrics.STATS_TOTAL_CPU_USAGE) / 1_000_000; // ns -> ms
        double availableTime = (durationMS * (cpuQuota * 1.0 / ContainerProfiling.DEFAULT_CPU_PERIOD));
        return statsTotalCpuMS / availableTime;
    }

    /**
     * Divide current cpu usage by current time
     *
     * @param profile profile
     * @return max cpu usage
     * @throws ProfilingException
     */
    private double calculateMaxCpuUtilisation(Profile profile) throws ProfilingException {
        return profile.deltaMetrics
                .stream()
                // Stats CPU usage (nano seconds) / stats time (milli seconds) * 1_000_000
                .map(m -> m.getOrDefault(Metrics.STATS_TOTAL_CPU_USAGE, 0L) / (1_000_000. * m.getOrDefault(Metrics.STATS_TIME, 1L)))
                .reduce(Math::max)
                .orElseThrow(() -> new ProfilingException("Max CPU usage could not be calculated."));
    }

    private double getAverageMemoryUtilization(Profile profile) throws ProfilingException {
        double averageUsage = profile.metrics
                .stream()
                .map(m -> m.getOrDefault(Metrics.MEMORY_USAGE, 0L))
                .filter(a -> a != 0L)
                .mapToDouble(a -> a)
                .average()
                .orElseThrow(() -> new ProfilingException("Average memory usage could not be calculated."));
        return averageUsage / memoryLimit;
    }

    private long getMaxMemoryUtilization(Profile profile) throws ProfilingException {
        return profile.metrics
                .stream()
                .map(m -> m.getOrDefault(Metrics.MEMORY_USAGE, -1))
                .reduce(Math::max)
                .orElseThrow(() -> new ProfilingException("Max memory usage could not be calculated."));
    }

    private long getNetworkUsage(Profile profile) throws ProfilingException {
        return profile.metrics
                .stream()
                .map(m -> m.getOrDefault(Metrics.TX_BYTES, -1))
                .reduce(Math::addExact)
                .orElseThrow(() -> new ProfilingException("IO Usage cannot be calculated"));

    }

}
