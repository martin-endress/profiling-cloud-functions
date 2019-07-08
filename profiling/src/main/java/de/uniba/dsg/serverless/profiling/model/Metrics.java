package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.*;
import java.util.stream.IntStream;

public class Metrics {
    private Map<String, Long> metrics;
    public List<String> relevantMetrics = new ArrayList<>();
    private MetricsUtil util = new MetricsUtil();

    public Metrics(List<String> lines, long time) throws ProfilingException {
        relevantMetrics = new ArrayList<>(Arrays.asList("time", "user", "system"));
        metrics = fromLines(lines);
        metrics.put("time", time);
        validate();
    }

    public Metrics(Statistics stats, long containerStartTime) throws ProfilingException {
        metrics = fromStatistics(stats, containerStartTime);
        validate();
    }

    /**
     * Adds all metrics entries to the set.
     *
     * @param metrics RelevantMetrics must be distinct and not contain any existing keys
     * @throws ProfilingException when a duplicate is key is present.
     */
    public void addMetrics(Metrics metrics) throws ProfilingException {
        for (String metricsKey : metrics.relevantMetrics) {
            Long value = metrics.metrics.get(metricsKey);
            if (this.metrics.containsKey(metricsKey)) {
                throw new ProfilingException("Merging two Metrices failed. Duplicate key: " + metricsKey);
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
        return String.join(",", out);
    }

    private void validate() throws ProfilingException {
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
    private Map<String, Long> fromStatistics(Statistics stats, long containerStartTime) throws ProfilingException {
        validateStats(stats);
        HashMap<String, Long> map = new HashMap<>();

        long time = util.parseTime(stats.getRead());
        long relativeTime = time - containerStartTime;
        map.put("stats_time", relativeTime);
        relevantMetrics.add("stats_time");

        long totalCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getTotalUsage()).orElse(-1L);
        map.put("stats_total_cpu_usage", totalCpu);
        relevantMetrics.add("stats_total_cpu_usage");

        // per CPU stats
        List<Long> usagePerCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getPercpuUsage()).orElse(new ArrayList<>());
        IntStream.range(0, usagePerCpu.size()).forEach(i -> {
            map.put("cpu_" + i, usagePerCpu.get(i));
            relevantMetrics.add("cpu_" + i);
        });

        Map<String, StatisticNetworksConfig> networkMap = Optional.ofNullable(stats.getNetworks()).orElse(new HashMap<>());
        if (networkMap.isEmpty()) {
            throw new ProfilingException("Networkmap is empty. ");
        }
        for (Map.Entry<String, StatisticNetworksConfig> c : networkMap.entrySet()) {
            // prepend network name if there is more than one network
            String networkName = networkMap.size() != 1 ? c.getKey() : "";
            map.put(networkName + "rx_bytes", c.getValue().getRxBytes());
            map.put(networkName + "rx_dropped", c.getValue().getRxDropped());
            map.put(networkName + "rx_errors", c.getValue().getRxErrors());
            map.put(networkName + "rx_packets", c.getValue().getRxPackets());
            map.put(networkName + "tx_bytes", c.getValue().getTxBytes());
            map.put(networkName + "tx_dropped", c.getValue().getTxDropped());
            map.put(networkName + "tx_errors", c.getValue().getTxErrors());
            map.put(networkName + "tx_packets", c.getValue().getTxPackets());

            relevantMetrics.addAll(Arrays.asList(
                    networkName + "rx_bytes",
                    networkName + "rx_dropped",
                    networkName + "rx_errors",
                    networkName + "rx_packets",
                    networkName + "tx_bytes",
                    networkName + "tx_dropped",
                    networkName + "tx_errors",
                    networkName + "tx_packets"));
        }

        return map;
    }

    private void validateStats(Statistics stats) throws ProfilingException {
        if (stats.getMemoryStats() == null
                || stats.getMemoryStats().getStats() == null
                || stats.getCpuStats() == null
                || stats.getCpuStats().getCpuUsage() == null) {
            throw new ProfilingException("Stats must not be null");
        }
    }

}
