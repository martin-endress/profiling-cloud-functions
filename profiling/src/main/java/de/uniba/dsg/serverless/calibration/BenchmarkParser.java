package de.uniba.dsg.serverless.calibration;

import com.google.common.base.Charsets;
import com.google.gson.GsonBuilder;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BenchmarkParser {

    public static final String BENCHMARK_NAME = "benchmark.json";

    private final Path benchmarkPath;
    private final Path outputPath;

    public BenchmarkParser(Path outputPath, Path benchmarkPath) {
        this.benchmarkPath = benchmarkPath;
        this.outputPath = outputPath;
    }

    public void parseBenchmark() throws ProfilingException {
        if (!Files.exists(benchmarkPath)) {
            throw new ProfilingException("Benchmark file does not exist.");
        }
        try {
            CSVParser parser = CSVParser.parse(benchmarkPath, Charsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            double averageKFlops = parser.getRecords()
                    .stream()
                    .map(r -> r.get("KFLOPS"))
                    .mapToDouble(Double::parseDouble)
                    .average()
                    .orElseThrow(() -> new ProfilingException("Average kFlops could not be calculated."));
            writeResultsToFile(new BenchmarkResult(averageKFlops));
        } catch (IOException e) {
            throw new ProfilingException("Could not read output file.", e);
        }
    }

    private void writeResultsToFile(BenchmarkResult result) throws ProfilingException {
        String resultJson = new GsonBuilder().setPrettyPrinting().create().toJson(result);
        try {
            Files.write(outputPath.resolve(BENCHMARK_NAME), resultJson.getBytes());
        } catch (IOException e) {
            throw new ProfilingException("Could not write to file");
        }
    }

}
