package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Metrics {
    private Map<String, Long> metrics;
    public List<String> relevantMetrics = new ArrayList<>();

    public Metrics(List<String> lines, long time) throws ProfilingException {
        relevantMetrics= new ArrayList<>(Arrays.asList("time", "cache", "swap", "active_anon", "inactive_file", "user", "system"));
        metrics = fromLines(lines);
        metrics.put("time", time);
        validate();
    }

    public Metrics(Statistics stats, long time) throws ProfilingException {
        metrics = fromStatistics(stats);
        metrics.put("time", time);
        validate();
    }

    /**
     * TODO
     *
     * @param metrics
     * @return
     */
    public void addMetrics(Metrics metrics) {
        for (String metricsKey : metrics.relevantMetrics) {
            Long value = metrics.metrics.get(metricsKey);
            while (this.metrics.containsKey(metricsKey)) {
                metricsKey += "_";
            }
            this.relevantMetrics.add(metricsKey);
            this.metrics.put(metricsKey, value);
        }
    }

    @Override
    public String toString() {
        List<String> out = new ArrayList<>();
        for (String s : relevantMetrics) {
            out.add(metrics.get(s).toString());
        }
        return out.stream().collect(Collectors.joining(","));
    }

    private void validate() throws ProfilingException {
        for (String s : relevantMetrics) {
            if (!metrics.keySet().contains(s)) {
                throw new ProfilingException("Metrics does not contain metric: " + s);
            }
        }
        if (!metrics.keySet().containsAll(relevantMetrics) || metrics.isEmpty()) {
            throw new ProfilingException("Metrics must not be empty and contain all relevantMetrics.");
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

    /**
     * asdf
     *
     * @param stats statistics
     * @return Map with Metrics
     * @throws ProfilingException if MemoryStats or CpuStats are null
     * @see <a href="https://github.com/moby/moby/issues/29306">https://github.com/moby/moby/issues/29306</a>
     */
    private Map<String, Long> fromStatistics(Statistics stats) throws ProfilingException {
        if (stats.getMemoryStats() == null
                || stats.getMemoryStats().getStats() == null
                || stats.getCpuStats() == null
                || stats.getCpuStats().getCpuUsage() == null) {
            throw new ProfilingException("Stats must not be null");
        }
        HashMap<String, Long> map = new HashMap<>();

        StatsConfig memory = stats.getMemoryStats().getStats();
        long cache = Optional.ofNullable(memory.getCache()).orElse(-1L);
        map.put("cache", cache);
        long swap = Optional.ofNullable(memory.getSwap()).orElse(-1L);
        map.put("swap", swap);
        long activeAnon = Optional.ofNullable(memory.getActiveAnon()).orElse(-1L);
        map.put("active_anon", activeAnon);
        long inactiveFile = Optional.ofNullable(memory.getInactiveFile()).orElse(-1L);
        map.put("inactive_file", inactiveFile);

        long totalCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getTotalUsage()).orElse(-1L);
        map.put("total_cpu_usage", totalCpu);
        // per CPU stats
        List<Long> usagePerCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getPercpuUsage()).orElse(new ArrayList<>());
        IntStream.range(0, usagePerCpu.size()).forEach(i -> {
            map.put("cpu_" + i, usagePerCpu.get(i));
            relevantMetrics.add("cpu_" + i);
        });

        Map<String, StatisticNetworksConfig> networkMap = Optional.ofNullable(stats.getNetworks()).orElse(new HashMap<>());
        for (Map.Entry<String, StatisticNetworksConfig> c : networkMap.entrySet()) {
            map.put("tx_bytes", c.getValue().getTxBytes());
            map.put("rx_bytes", c.getValue().getRxBytes());
        }

        //map.put("time", stats.getRead()); TODO

        relevantMetrics.addAll(Arrays.asList("total_cpu_usage", "cache", "swap", "active_anon", "inactive_file", "tx_bytes", "rx_bytes"));


        return map;
    }

}
