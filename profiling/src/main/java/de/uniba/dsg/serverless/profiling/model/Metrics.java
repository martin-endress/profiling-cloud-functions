package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.*;
import java.util.stream.IntStream;

public class Metrics {
    private Map<String, Long> metrics;
    public List<String> relevantMetrics = new ArrayList<>();
    private String timeStamp;

    public static final String STATS_TIME = "stats_time";
    public static final String STATS_TOTAL_CPU_USAGE = "stats_total_cpu_usage";
    public static final String MEMORY_LIMIT = "memory_limit";
    public static final String MEMORY_CACHE = "memory_cache";
    public static final String MEMORY_USAGE = "memory_usage";
    public static final String RX_BYTES = "rx_bytes";
    public static final String RX_DROPPED = "rx_dropped";
    public static final String RX_ERRORS = "rx_errors";
    public static final String RX_PACKETS = "rx_packets";
    public static final String TX_BYTES = "tx_bytes";
    public static final String TX_DROPPED = "tx_dropped";
    public static final String TX_ERRORS = "tx_errors";
    public static final String TX_PACKETS = "tx_packets";

    public Metrics(List<String> lines, long time) throws ProfilingException {
        relevantMetrics = new ArrayList<>(Arrays.asList("time", "user", "system"));
        metrics = fromLines(lines);
        metrics.put("time", time);
        validate();
    }

    public Metrics(Statistics stats, long containerStartTime) throws ProfilingException {
        timeStamp = stats.getRead();
        metrics = fromStatistics(stats, containerStartTime);
        validate();
    }

    public Long getMetric(String metric) throws ProfilingException {
        return Optional.ofNullable(metrics.get(metric))
                .orElseThrow(() -> new ProfilingException("Metrics not available: " + metric));
    }

    public Long getOrDefault(String metric, long defaultValue) {
        return Optional.ofNullable(metrics.get(metric)).orElse(defaultValue);
    }


    public boolean containsMetric(String key) {
        return metrics.containsKey(key);
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    /**
     * Adds all metrics entries to the set.
     *
     * @param metrics RelevantMetrics must be distinct and not contain any existing keys
     * @throws ProfilingException when a duplicate is key is present.
     */
    public void addMetrics(Metrics metrics) throws ProfilingException {
        timeStamp = (timeStamp != null) ? timeStamp : metrics.timeStamp;
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
        out.add(timeStamp);
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

        long time = MetricsUtil.parseTime(stats.getRead());
        long relativeTime = time - containerStartTime;
        map.put(STATS_TIME, relativeTime);
        relevantMetrics.add(STATS_TIME);

        long totalCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getTotalUsage()).orElse(-1L);
        map.put(STATS_TOTAL_CPU_USAGE, totalCpu);
        relevantMetrics.add(STATS_TOTAL_CPU_USAGE);

        // per CPU stats
        List<Long> usagePerCpu = Optional.ofNullable(stats.getCpuStats().getCpuUsage().getPercpuUsage()).orElse(new ArrayList<>());
        IntStream.range(0, usagePerCpu.size()).forEach(i -> {
            map.put("cpu_" + i, usagePerCpu.get(i));
            relevantMetrics.add("cpu_" + i);
        });

        map.put(MEMORY_USAGE, stats.getMemoryStats().getUsage());
        relevantMetrics.add(MEMORY_USAGE);
        map.put(MEMORY_CACHE, stats.getMemoryStats().getStats().getCache());
        relevantMetrics.add(MEMORY_CACHE);

        map.put(MEMORY_LIMIT, stats.getMemoryStats().getLimit());
        relevantMetrics.add(MEMORY_LIMIT);

        Map<String, StatisticNetworksConfig> networkMap = Optional.ofNullable(stats.getNetworks()).orElse(new HashMap<>());
        if (networkMap.size() != 1) {
            throw new ProfilingException("Networkmap must be of size 1. ");
        }

        for (String key : networkMap.keySet()) {
            StatisticNetworksConfig network = networkMap.get(key);
            map.put(RX_BYTES, network.getRxBytes());
            map.put(RX_DROPPED, network.getRxDropped());
            map.put(RX_ERRORS, network.getRxErrors());
            map.put(RX_PACKETS, network.getRxPackets());
            map.put(TX_BYTES, network.getTxBytes());
            map.put(TX_DROPPED, network.getTxDropped());
            map.put(TX_ERRORS, network.getTxErrors());
            map.put(TX_PACKETS, network.getTxPackets());

            relevantMetrics.addAll(Arrays.asList(
                    RX_BYTES,
                    RX_DROPPED,
                    RX_ERRORS,
                    RX_PACKETS,
                    TX_BYTES,
                    TX_DROPPED,
                    TX_ERRORS,
                    TX_PACKETS));
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
