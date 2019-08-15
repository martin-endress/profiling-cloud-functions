package de.uniba.dsg.serverless.calibration;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class Calibration {

    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/result/"; // specified by linpack benchmark
    private static final int LINPACK_ARRAY_SIZE = 200;

    private final Path calibrationFolder;

    public Calibration(String name) {
        calibrationFolder = Paths.get("calibration", name);
    }

    public void executeLocalBenchmark() throws ProfilingException {
        ContainerProfiling linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);
        linpack.buildContainer();
        linpack.startContainer(Collections.singletonMap("LINPACK_ARRAY_SIZE", String.valueOf(LINPACK_ARRAY_SIZE)));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        Path output = calibrationFolder.resolve("result/output.csv");
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. linpack/output.csv does not exist.");
        }
        BenchmarkParser parser = new BenchmarkParser(calibrationFolder, output);
        parser.parseBenchmark();
    }
}
