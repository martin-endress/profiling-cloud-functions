package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.Optional;

public class ProfileMetaInfo {

    public final String created;
    public final HostConfig hostConfig;
    public final String imageId;
    public final InspectContainerResponse.ContainerState state;
    public final long durationMS;
    public final double cpuUtilisation;

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
        cpuUtilisation = calculateCpuUtilization(profile);
    }

    private double calculateCpuUtilization(Profile profile) throws ProfilingException {
        long cpuQuota = hostConfig.getCpuQuota();
        cpuQuota = (cpuQuota == 0) ? ContainerProfiling.DEFAULT_CPU_PERIOD : cpuQuota; // 0 means no limit
        long statsTotalCpuMS = profile.lastMetrics.getMetric("stats_total_cpu_usage") / 1_000_000; // ns -> ms
        double availableTime = (durationMS * (cpuQuota * 1.0 / ContainerProfiling.DEFAULT_CPU_PERIOD));
        return statsTotalCpuMS / availableTime;
    }
}
