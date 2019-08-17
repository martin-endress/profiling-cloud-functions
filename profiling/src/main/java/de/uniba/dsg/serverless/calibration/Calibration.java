package de.uniba.dsg.serverless.calibration;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Calibration {

    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/output/"; // specified by linpack benchmark

    public static final int[] MEMORY_SIZES = {128, 256, 512, 1024, 2048, 3008};

    private final Path calibrationFolder;
    private final String name;

    public Calibration(String name) throws ProfilingException {
        this.name = name;
        calibrationFolder = Paths.get("calibration", name);
        try {
            Files.createDirectories(calibrationFolder);
        } catch (IOException e) {
            throw new ProfilingException("Could not create directories.", e);
        }
    }

    public void executeLocalBenchmark() throws ProfilingException {
        Path output = calibrationFolder.resolve("output/out.txt");
        if (Files.exists(output)) {
            System.out.println("Calibration already performed.");
            return;
        }
        ContainerProfiling linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);
        linpack.buildContainer();
        linpack.startContainer();
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. output/out.txt does not exist.");
        }
        BenchmarkParser parser = new BenchmarkParser(output, calibrationFolder);
        parser.parseBenchmark();
    }

    public void executeAWSBenchmark() throws ProfilingException {
        AWSClient client = new AWSClient();
        for (int memory : MEMORY_SIZES) {
            client.invokeBenchmarkFunctions(memory, name);
        }
        for (int memory : MEMORY_SIZES) {
            String keyName = "linpack/" + memory + "/" + name;
            System.out.println("waiting for object...");
            client.waitForBucketObject(keyName, 600);
            client.getFileFromBucket(keyName, calibrationFolder.resolve(memory + ".csv"));
        }
    }
}


