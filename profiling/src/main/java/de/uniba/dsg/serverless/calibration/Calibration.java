package de.uniba.dsg.serverless.calibration;

import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.ArrayUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Calibration {

    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/output/"; // specified by linpack benchmark

    public static final int[] MEMORY_SIZES = {128, 256, 512, 1024, 2048, 3008};

    private final Path calibrationFolder;
    private final String name;
    private final ContainerProfiling linpack;

    public Calibration(String name) throws ProfilingException {
        this.name = name;
        calibrationFolder = Paths.get("calibration", name);
        linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);
        try {
            Files.createDirectories(calibrationFolder);
        } catch (IOException e) {
            throw new ProfilingException("Could not create directories.", e);
        }
    }

    public void executeLocalBenchmark(double limit) throws ProfilingException {
        Path output = calibrationFolder.resolve("output/out.txt");
        if (Files.exists(output)) {
            System.out.println("Calibration already performed.");
            return;
        }
        //linpack.buildContainer();
        //linpack.startContainer(ResourceLimits.fromFile("limits.json"));
        linpack.startContainer(new ResourceLimits(limit, 0));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. output/out.txt does not exist.");
        }
        BenchmarkResult result = new BenchmarkParser(output).parseBenchmark();

        StringBuilder sb = new StringBuilder();
        sb.append("average,max");
        sb.append("\n");
        sb.append(result.average + "," + result.max);
        System.out.println(sb.toString());
        try {
            Files.write(calibrationFolder.resolve("local.csv"), sb.toString().getBytes());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }

    public void executeAWSBenchmark() throws ProfilingException {
        AWSClient client = new AWSClient();
        for (int memory : MEMORY_SIZES) {
            client.invokeBenchmarkFunctions(memory, name);
        }
        List<BenchmarkResult> results = new ArrayList<>();
        for (int memory : MEMORY_SIZES) {
            Path out = calibrationFolder.resolve(memory + ".csv");
            String keyName = "linpack/" + memory + "/" + name;
            client.waitForBucketObject(keyName, 600);
            client.getFileFromBucket(keyName, out);
            results.add(new BenchmarkParser(out).parseBenchmark());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Arrays.stream(MEMORY_SIZES).mapToObj(String::valueOf).collect(Collectors.joining(",")));
        sb.append("\n");
        sb.append(results.stream().map(a -> String.valueOf(a.average)).collect(Collectors.joining(",")));
        System.out.println(sb.toString());
        try {
            Files.write(calibrationFolder.resolve("awsResults.csv"), sb.toString().getBytes());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }
}


