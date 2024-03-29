package de.uniba.dsg.serverless.experiment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.calibration.Calibration;
import de.uniba.dsg.serverless.profiling.StatsRetriever;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.model.ResourceLimits;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Experiment {

    // Constants
    private static final Gson PARSER = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    public static final Path EXPERIMENTS_FOLDER = Paths.get("experiments");

    // General Information
    @Expose
    String experimentName;

    // Local specific information
    @Expose
    List<Double> quotas;

    // AWS specific information
    @Expose
    String targetUrl;
    @Expose
    String apiKey;
    @Expose
    String bucketName;
    @Expose
    List<Integer> memorySizes;
    @Expose
    int numberOfAWSExecutions;

    // Simulation
    @Expose
    List<Integer> simulatedMemory;
    @Expose
    int numberOfLoads;
    @Expose
    Map<String, String> loadPattern;

    //
    private Calibration calibration;
    private LocalPerformanceModel localPerformanceModel;
    private ProviderPerformanceModel providerPerformanceModel;

    private Experiment() {
        // hide default Constructor
    }

    /**
     * Loads an experiment configuration from a file
     *
     * @param fileName experiment file
     * @return Experiment
     * @throws ProfilingException if the file is corrupt or does not exist
     */
    public static Experiment fromFile(String fileName) throws ProfilingException {
        Experiment experiment;
        try {
            Reader reader = new BufferedReader(new FileReader(EXPERIMENTS_FOLDER.resolve(fileName).toString()));
            experiment = PARSER.fromJson(reader, Experiment.class);
            experiment.providerPerformanceModel = ProviderPerformanceModel.withFixedGflops(0.0);
            experiment.localPerformanceModel = LocalPerformanceModel.withFixedLimit(0.0);
        } catch (IOException e) {
            throw new ProfilingException("File does not exist or is corrupt.", e);
        }
        experiment.calibration = new Calibration(experiment.experimentName);
        return experiment;
    }

    /**
     * Runs local and provider benchmark to find mapping between provider and local machine.
     * The calibration will be skipped, if it has already been performed.
     * If no calibration takes place, the local model will always return 0.0 (no limit).
     *
     * @throws ProfilingException
     */
    public void calibrate() throws ProfilingException {
        System.out.println("Starting Calibration");
        calibration.calibrateLocal(1);
        calibration.calibrateProvider(targetUrl, apiKey, bucketName, memorySizes, numberOfAWSExecutions);

        localPerformanceModel = new LocalPerformanceModel(calibration.localCalibrationOutput);
        providerPerformanceModel = new ProviderPerformanceModel(calibration.providerCalibrationOutput);

        System.out.println("Successfully created Performance Models: ");
        System.out.println(localPerformanceModel.toString());
        System.out.println(providerPerformanceModel.toString());
    }

    public void profile() throws ProfilingException {
        profile(true);
    }

    public void profile(boolean withBuild) throws ProfilingException {
        StatsRetriever statsRetriever = new StatsRetriever(experimentName, withBuild);
        for (int memory : simulatedMemory) {
            double gFlops = providerPerformanceModel.getGflops(memory);
            double quota = localPerformanceModel.estimateQuota(gFlops);
            if (quota > 1) {
                System.out.println("Cannot simulate memory " + memory + " using this machine. Too weak :( ...  quota=" + quota);
                return;
            }
            ResourceLimits limits = new ResourceLimits(quota, true, memory);
            System.out.println("Start profiling..  limits=" + limits.toString());
            statsRetriever.profile(loadPattern, limits, numberOfLoads);
        }
    }

}
