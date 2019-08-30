package de.uniba.dsg.serverless.calibration;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Calibration {

    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/output/"; // specified by linpack benchmark

    public static final int[] MEMORY_SIZES = {128, 256, 512, 1024, 2048, 3008};

    private final List<Double> quotas;
    private final Path calibrationFolder;
    private final String name;
    private final ContainerProfiling linpack;

    DecimalFormat decimalFormat = new DecimalFormat("#.###");


    public Calibration(String name, List<Double> quotas) throws ProfilingException {
        this.name = name;
        calibrationFolder = Paths.get("calibration", name);
        this.quotas = quotas;
        linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);
        try {
            Files.createDirectories(calibrationFolder);
        } catch (IOException e) {
            throw new ProfilingException("Could not create directories.", e);
        }
    }

    public void executeLocalBenchmarks() throws ProfilingException {
        if (Files.exists(calibrationFolder.resolve("results.csv"))) {
            System.out.println("Calibration already performed.");
            return;
        }
        System.out.println("building Container");
        linpack.buildContainer();
        List<Double> results = new ArrayList<>();
        for (double quota : quotas) {
            System.out.println("running calibration" + quota);
            double result = executeLocalBenchmark(quota);
            results.add(result);
        }
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(quotas.stream().map(decimalFormat::format).collect(Collectors.joining(",")));
        stringBuilder.append("\n");
        stringBuilder.append(results.stream().map(String::valueOf).collect(Collectors.joining(",")));
        stringBuilder.append("\n");

        try {
            Files.write(calibrationFolder.resolve("results.csv"), stringBuilder.toString().getBytes());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }

    }

    private double executeLocalBenchmark(double limit) throws ProfilingException {
        linpack.startContainer(new ResourceLimits(limit, 0));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        Path output = calibrationFolder.resolve(limit + "").resolve("out.txt");
        try {
            Files.move(calibrationFolder.resolve("output"), output);
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. output/out.txt does not exist.");
        }
        BenchmarkResult result = new BenchmarkParser(output).parseBenchmark();
        return result.average;
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


