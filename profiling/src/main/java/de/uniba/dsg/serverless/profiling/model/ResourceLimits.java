package de.uniba.dsg.serverless.profiling.model;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceLimits {


    public final double cpuLimit;
    public final long memoryLimit;

    public static final Path FOLDER = Paths.get("profiling", "src", "main", "resources");

    public ResourceLimits(double cpuLimit, long memoryLimit) {
        this.cpuLimit = cpuLimit;
        this.memoryLimit = memoryLimit;
    }

    public static ResourceLimits unlimited() {
        return new ResourceLimits(0.0, 0L);
    }

    public static ResourceLimits fromFile(String fileName) throws ProfilingException {
        Gson parser = new Gson();
        try {
            Reader reader = new BufferedReader(new FileReader(FOLDER.resolve(fileName).toString()));
            return parser.fromJson(reader, ResourceLimits.class);
        } catch (IOException e) {
            throw new ProfilingException("Resource limits could not be read. ", e);
        }
    }

}
