package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryMetrics {

    public static final List<String> RELEVANT_METRICS = Arrays.asList("cache", "swap", "active_anon", "inactive_file");

    private final MetricsUtil util = new MetricsUtil();
    private final Map<String, Long> metrics;

    private final long time;

    public MemoryMetrics(List<String> lines, long time) throws ProfilingException {
        metrics = util.fromLines(lines);
        metrics.keySet().containsAll(RELEVANT_METRICS);
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
        metrics.put("activeAnon", activeAnon);
        long inactiveFile = Optional.ofNullable(stats.getMemoryStats().getStats().getInactiveFile()).orElse(0l);
        metrics.put("inactiveFile", inactiveFile);

        String currentTime = stats.getRead();
        this.time = util.timeDifference(started, currentTime);
    }

    @Override
    public String toString() {
        List<String> out = new ArrayList<>();
        for (String s : metrics.keySet()) {
            out.add(metrics.get(s).toString());
        }
        return out.stream().collect(Collectors.joining(","));
    }
}
