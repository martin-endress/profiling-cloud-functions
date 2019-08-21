package de.uniba.dsg.serverless.calibration;

import com.google.common.base.Charsets;
import com.google.gson.GsonBuilder;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
