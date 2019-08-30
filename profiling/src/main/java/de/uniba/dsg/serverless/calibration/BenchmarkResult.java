package de.uniba.dsg.serverless.calibration;

import com.google.gson.GsonBuilder;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BenchmarkResult {

    public double average;
    public double max;

    public BenchmarkResult(double average, double max) {
        this.average = average;
        this.max = max;
    }

    public void writeResultsToFile(Path path) throws ProfilingException {
        String resultJson = new GsonBuilder().setPrettyPrinting().create().toJson(this);
        try {
            Files.write(path, resultJson.getBytes());
        } catch (IOException e) {
            throw new ProfilingException("Could not write to file", e);
        }
    }

}
