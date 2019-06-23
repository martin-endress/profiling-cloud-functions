package de.uniba.dsg.serverless.profiling.model;

import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.List;
import java.util.Map;

public class CPUMetrics extends Metrics {

    private final String time;
    private final Map<String, Long> metrics;

    public CPUMetrics(List<String> lines, String time) throws ProfilingException {
        this.time = time;
        metrics = new MetricsUtil().fromLines(lines);
    }


}
