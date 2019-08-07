package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.profiling.model.Metrics;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.profiling.ControlGroupProfiling;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatsRetriever {
    private long containerStartTime = 0L;

    private static final Path OUTPUT_FOLDER = Paths.get("profiles");

    private static final int LINPACK_ARRAY_SIZE = 200;

    private static final String EXECUTOR_DOCKERFILE = "executor/Dockerfile";
    private static final String EXECUTOR_IMAGE = "mendress/executor";

    private static final String SERVICE_MOCK_DOCKERFILE = "serviceMock/Dockerfile";
    private static final String SERVICE_MOCK_IMAGE = "mendress/servicemock";

    private static final String LINEPACK_DOCKERFILE = "linpack/Dockerfile";
    private static final String LINEPACK_IMAGE = "mendress/linepack";

    public static void main(String[] args) {
        try {
            new StatsRetriever().retrieveStats();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .ifPresent(System.err::println);
            e.printStackTrace();
        }
    }

    private void retrieveStats() throws ProfilingException {
        final String profileName = "Profile_" + DateTimeFormatter.ofPattern("MM.dd_HH.mm.ss").format(LocalDateTime.now());
        final Path profileFolder = OUTPUT_FOLDER.resolve(profileName);

        ContainerProfiling serviceMock = new ContainerProfiling(SERVICE_MOCK_DOCKERFILE, SERVICE_MOCK_IMAGE);
        ContainerProfiling executor = new ContainerProfiling(EXECUTOR_DOCKERFILE, EXECUTOR_IMAGE);
        ContainerProfiling linpack = new ContainerProfiling(LINEPACK_DOCKERFILE, LINEPACK_IMAGE);
        //build(serviceMock);
        //build(executor);
        //build(linpack);

        double kFlops = 2532596.;//getKFlops(linpack, profileFolder);
        Map<String, String> environment = getParameters();

        environment.put("MOCK_PORT", "9000");
        serviceMock.startContainer(environment);
        environment.put("MOCK_IP", serviceMock.getIpAddress());

        ResourceLimits limits = ResourceLimits.fromFile("limits.json");

        List<Profile> profiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String containerId = executor.startContainer(environment, limits);
            containerStartTime = System.currentTimeMillis();
            System.out.println("Container started. (id=" + containerId + "/ startedAt=" + containerStartTime + ")");
            Profile p = getProfileUsingBoth(executor, containerId);
            p.setkFlops(kFlops);
            p.save(profileFolder.resolve("profile" + i));
            profiles.add(p);
        }

        serviceMock.kill();
    }

    private Map<String, String> getParameters() {
        Map<String, String> map = new HashMap<>();
        map.put("LOAD_TIME", "3000");
        map.put("CPU_FROM", "0.2");
        map.put("CPU_TO", "0.4");
        map.put("MEMORY_TO", "5000");
        map.put("IO_FROM", "1");
        map.put("IO_TO", "2");
        map.put("IO_SIZE", "5000");
        return map;
    }

    private double getKFlops(ContainerProfiling linpack, Path profileFolder) throws ProfilingException {
        linpack.startContainer(Collections.singletonMap("LINPACK_ARRAY_SIZE", String.valueOf(LINPACK_ARRAY_SIZE)));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer("/usr/src/linpack/", profileFolder);
        Path output = profileFolder.resolve("linpack/output.csv");
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. linpack/output.csv does not exist.");
        }
        try {
            CSVParser parser = CSVParser.parse(output, Charsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            return parser.getRecords()
                    .stream()
                    .map(r -> r.get("KFLOPS"))
                    .mapToDouble(Double::parseDouble)
                    .average()
                    .orElseThrow(() -> new ProfilingException("Kflops could not be calculated."));
        } catch (IOException e) {
            throw new ProfilingException("Could not read output file.", e);
        }
    }

    private void build(ContainerProfiling c) throws ProfilingException {
        System.out.println("building container: " + c.imageName);
        System.out.println("finished: " + c.buildContainer());
    }

    private Profile getProfileUsingBoth(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        Optional<Statistics> statistics = profiling.getNextStatistics();
        while (statistics.isPresent()) {
            Metrics apiMetrics = new Metrics(statistics.get(), containerStartTime);
            Metrics cgMetrics = controlGroupProfiling.getMetric();
            // delay between metrics is ~max 3 ms
            cgMetrics.addMetrics(apiMetrics);
            metrics.add(cgMetrics);
            statistics = profiling.getNextStatistics();
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }

    private Profile getProfileUsingControlGroups(ContainerProfiling profiling, String containerId) throws ProfilingException {
        List<Metrics> metrics = new ArrayList<>();
        ControlGroupProfiling controlGroupProfiling = new ControlGroupProfiling(containerId, containerStartTime);
        while (controlGroupProfiling.metricsAvailable()) {
            metrics.add(controlGroupProfiling.getMetric());
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS);
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }

    private Profile getProfileUsingDockerApi(ContainerProfiling profiling) throws ProfilingException {
        List<Statistics> stats = profiling.logStatistics();
        List<Metrics> metrics = new ArrayList<>();
        for (Statistics s : stats) {
            metrics.add(new Metrics(s, containerStartTime));
        }
        InspectContainerResponse additional = profiling.inspectContainer();
        return new Profile(metrics, additional);
    }
}
