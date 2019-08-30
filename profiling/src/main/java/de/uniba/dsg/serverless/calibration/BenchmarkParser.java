package de.uniba.dsg.serverless.calibration;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BenchmarkParser {

    public static final String BENCHMARK_NAME = "benchmark.json";

    private final Path benchmarkPath;

    public BenchmarkParser(Path benchmarkPath) {
        this.benchmarkPath = benchmarkPath;
    }

    public BenchmarkResult parseBenchmark() throws ProfilingException {
        if (!Files.exists(benchmarkPath)) {
            throw new ProfilingException("Benchmark file does not exist.");
        }
        try {
            List<String> lines = Files.readAllLines(benchmarkPath);
            String result = lines.get(lines.size() - 7);
            String[] parts = result.split("\\s+");
            return new BenchmarkResult(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
        } catch (IOException e) {
            throw new ProfilingException("Could not read file. ", e);
        }
    }

}
