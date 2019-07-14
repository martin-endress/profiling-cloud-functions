package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

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
        created = profile.additional.getCreated();
        hostConfig = profile.additional.getHostConfig();
        imageId = profile.additional.getImageId();
        state = profile.additional.getState();
        durationMS = MetricsUtil.timeDifference(state.getStartedAt(), state.getFinishedAt());
        long statsTotalCpuMS = profile.lastMetrics.getMetric("stats_total_cpu_usage") / 1_000_000; // ns -> ms
        cpuUtilisation = 1.0 * statsTotalCpuMS / durationMS;
    }


}
