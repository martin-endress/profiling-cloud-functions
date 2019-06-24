package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Metrics {
    private Map<String, Long> metrics;
    public final List<String> RELEVANT_METRICS = Arrays.asList("time", "cache", "swap", "active_anon", "inactive_file", "user", "system");

    public Metrics(List<String> lines, long time) throws ProfilingException {
        metrics = fromLines(lines);
        metrics.put("time", time);
        validate();
    }

    public Metrics(Statistics stats, long time) throws ProfilingException {
        metrics = fromStatistics(stats);
        metrics.put("time", time);
        validate();
    }

    @Override
    public String toString() {
        List<String> out = new ArrayList<>();
        for (String s : RELEVANT_METRICS) {
            out.add(metrics.get(s).toString());
        }
        return out.stream().collect(Collectors.joining(","));
    }

    private void validate() throws ProfilingException {
        for (String s : RELEVANT_METRICS) {
            if (!metrics.keySet().contains(s)) {
                throw new ProfilingException("Metrics does not contain metric: " + s);
            }
        }
        if (!metrics.keySet().containsAll(RELEVANT_METRICS) || metrics.isEmpty()) {
            throw new ProfilingException("Metrics must not be empty and contain all RELEVANT_METRICS.");
        }
    }

    /**
     * Creates a map containing performance metrics
     *
     * @param lines performance metrics as key value pairs separated by spaces <br>Example: <br>cache 1337L<br>cpu_usage 420L
     * @return Map
     * @throws ProfilingException input does not have the correct format
     */
    private Map<String, Long> fromLines(List<String> lines) throws ProfilingException {
        HashMap<String, Long> map = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length != 2) {
                throw new ProfilingException("Each line must be a key value pair separated by \" \". line:" + line);
            }
            long val;
            try {
                val = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                throw new ProfilingException("Parse error in line: " + line, e);
            }
            map.put(parts[0], val);
        }
        return map;
    }

    private Map<String, Long> fromStatistics(Statistics stats) throws ProfilingException {
        if (stats.getMemoryStats() == null || stats.getMemoryStats().getStats() == null) {
            throw new ProfilingException("Memory stats must not be null");
        }
        HashMap<String, Long> map = new HashMap<>();

        long cache = Optional.ofNullable(stats.getMemoryStats().getStats().getCache()).orElse(0l);
        map.put("cache", cache);
        long swap = Optional.ofNullable(stats.getMemoryStats().getStats().getSwap()).orElse(0l);
        map.put("swap", swap);
        long activeAnon = Optional.ofNullable(stats.getMemoryStats().getStats().getActiveAnon()).orElse(0l);
        map.put("active_anon", activeAnon);
        long inactiveFile = Optional.ofNullable(stats.getMemoryStats().getStats().getInactiveFile()).orElse(0l);
        map.put("inactive_file", inactiveFile);

        long private_ = Optional.ofNullable(stats.getCpuStats().getSystemCpuUsage()).orElse(0L);
        map.put("user", private_);
        long online = Optional.ofNullable(stats.getCpuStats().getOnlineCpus()).orElse(0L);
        map.put("system", online);

        return map;
    }

}
