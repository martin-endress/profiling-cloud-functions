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
import java.util.List;
import java.util.stream.Collectors;

public class Calibration {

    // Constants
    private static final String LOCAL_CALIBRATION_FILE_NAME = "localCalibration.csv";
    private static final String PROVIDER_CALIBRATION_FILE_NAME = "providerCalibration.csv";
    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/output/"; // specified by linpack benchmark
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

    private final String name;
    private final Path calibrationFolder;
    public final Path localCalibrationOutput;
    public final Path providerCalibrationOutput;

    // Docker specific
    private final ContainerProfiling linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);


    public Calibration(String name) throws ProfilingException {
        this.name = name;
        calibrationFolder = Paths.get("calibration", name);
        localCalibrationOutput = calibrationFolder.resolve(LOCAL_CALIBRATION_FILE_NAME);
        providerCalibrationOutput = calibrationFolder.resolve(PROVIDER_CALIBRATION_FILE_NAME);
        try {
            Files.createDirectories(calibrationFolder);
        } catch (IOException e) {
            throw new ProfilingException("Could not create directories.", e);
        }
    }

    public void calibrateLocal(List<Double> quotas) throws ProfilingException {
        if (Files.exists(localCalibrationOutput)) {
            System.out.println("Local calibration already performed.");
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

        stringBuilder.append(quotas.stream().map(DECIMAL_FORMAT::format).collect(Collectors.joining(",")));
        stringBuilder.append("\n");
        stringBuilder.append(results.stream().map(String::valueOf).collect(Collectors.joining(",")));
        stringBuilder.append("\n");

        try {
            Files.write(localCalibrationOutput, stringBuilder.toString().getBytes());
        } catch (IOException e) {
            throw new ProfilingException("Could not write local calibration to " + localCalibrationOutput.toString(), e);
        }
    }

    private double executeLocalBenchmark(double limit) throws ProfilingException {
        linpack.startContainer(new ResourceLimits(limit, 0));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        Path output = calibrationFolder.resolve("local" + limit).resolve("out.txt");
        try {
            Files.move(calibrationFolder.resolve("output"), output.getParent());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. output/out.txt does not exist.");
        }
        BenchmarkResult result = new BenchmarkParser(output).parseBenchmark();
        return result.average;
    }

    public void calibrateProvider(String targetUrl, String apiKey, String bucketName, List<Integer> memorySizes, int numberOfCalibrations) throws ProfilingException {
        if (Files.exists(providerCalibrationOutput)) {
            System.out.println("Provider calibration already performed.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(memorySizes.stream().map(String::valueOf).collect(Collectors.joining(",")));
        sb.append("\n");

        AWSClient client = new AWSClient(targetUrl, apiKey, bucketName);
        for (int i = 0; i < numberOfCalibrations; i++) {
            List<BenchmarkResult> results = executeProviderCalibration(i, client, memorySizes);
            sb.append(results.stream().map(r -> String.valueOf(r.average)).collect(Collectors.joining(",")));
            sb.append("\n");
        }
        try {
            Files.write(providerCalibrationOutput, sb.toString().getBytes());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }

    // While using a map would be more clean, the guaranteed order of a List is enough in this case. -> makes serialisation easier :)
    private List<BenchmarkResult> executeProviderCalibration(int i, AWSClient client, List<Integer> memorySizes) throws ProfilingException {
        for (int memory : memorySizes) {
            client.invokeBenchmarkFunctions(memory, name + i);
        }
        List<BenchmarkResult> results = new ArrayList<>();
        for (int memory : memorySizes) {
            Path out = calibrationFolder.resolve(memory + "_" + i + ".csv");
            String keyName = "linpack/" + memory + "/" + (name + i);
            client.waitForBucketObject(keyName, 600);
            client.getFileFromBucket(keyName, out);
            results.add(new BenchmarkParser(out).parseBenchmark());
        }
        return results;
    }
}


