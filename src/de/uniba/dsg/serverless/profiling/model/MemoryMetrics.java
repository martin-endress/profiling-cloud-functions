package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.Statistics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MemoryMetrics extends Metrics {

    private final long time;

    public MemoryMetrics(List<String> lines, long time) throws ProfilingException {
        RELEVANT_METRICS = Arrays.asList("cache", "swap", "active_anon", "inactive_file");
        metrics = util.fromLines(lines);
        validate();
        this.time = time;
    }

    public MemoryMetrics(Statistics stats, String started) throws ProfilingException {
        if (stats.getMemoryStats() == null || stats.getMemoryStats().getStats() == null) {
            throw new ProfilingException("Memory stats must not be null");
        }
        metrics = new HashMap<>();

        long cache = Optional.ofNullable(stats.getMemoryStats().getStats().getCache()).orElse(0l);
        metrics.put("cache", cache);
        long swap = Optional.ofNullable(stats.getMemoryStats().getStats().getSwap()).orElse(0l);
        metrics.put("swap", swap);
        long activeAnon = Optional.ofNullable(stats.getMemoryStats().getStats().getActiveAnon()).orElse(0l);
        metrics.put("active_anon", activeAnon);
        long inactiveFile = Optional.ofNullable(stats.getMemoryStats().getStats().getInactiveFile()).orElse(0l);
        metrics.put("inactive_file", inactiveFile);

        validate();

        String currentTime = stats.getRead();
        this.time = util.timeDifference(started, currentTime);
    }

}
