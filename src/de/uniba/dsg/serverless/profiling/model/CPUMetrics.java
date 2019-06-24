package de.uniba.dsg.serverless.profiling.model;

import de.uniba.dsg.serverless.profiling.util.MetricsUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CPUMetrics extends Metrics<CPUMetrics> {

    private final long time;

    public CPUMetrics(List<String> lines, long time) throws ProfilingException {
        RELEVANT_METRICS = Arrays.asList("user", "system");
        metrics = util.fromLines(lines);
        validate();
        this.time = time;
    }

}
