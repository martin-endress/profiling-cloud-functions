package de.uniba.dsg.serverless.profiling.model;

import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Metrics {

    protected Map<String, Long> metrics;
    protected final MetricsUtil util;

    public List<String> RELEVANT_METRICS;

    public Metrics() {
        util = new MetricsUtil();
        RELEVANT_METRICS = new ArrayList<>();
    }

    @Override
    public String toString() {
        List<String> out = new ArrayList<>();
        for (String s : RELEVANT_METRICS) {
            out.add(metrics.get(s).toString());
        }
        return out.stream().collect(Collectors.joining(","));
    }

    public void validate() throws ProfilingException {
        if (!metrics.keySet().containsAll(RELEVANT_METRICS) || metrics.isEmpty()) {
            throw new ProfilingException("Metrics must not be empty and contain all RELEVANT_METRICS.");
        }
    }

    public String getHeader() {
        return RELEVANT_METRICS.stream().collect(Collectors.joining(","));
    }
}
