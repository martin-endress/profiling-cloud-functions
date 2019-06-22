package de.uniba.dsg.serverless.profiling.model;

import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Metrics {

    protected Map<String, Long> metrics;
    protected final MetricsUtil util = new MetricsUtil();

    @Override
    public String toString() {
        List<String> out = new ArrayList<>();
        for (String s : metrics.keySet()) {
            out.add(metrics.get(s).toString());
        }
        return out.stream().collect(Collectors.joining(","));
    }
}
