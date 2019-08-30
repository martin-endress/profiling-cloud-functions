package de.uniba.dsg.serverless.experiment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.calibration.Calibration;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Experiment {

    private static final Gson PARSER = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    public static final Path EXPERIMENTS_FOLDER = Paths.get("experiments");

    @Expose
    public String experimentName;
    @Expose
    public String localCalibrationFolder;
    @Expose
    public List<Double> quotas;
    @Expose
    public List<String> providerCalibrationFolders;


    private Calibration calibration;

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
        } catch (IOException e) {
            throw new ProfilingException("File does not exist or is corrupt.", e);
        }
        experiment.calibration = new Calibration(experiment.experimentName, experiment.quotas);
        return experiment;
    }

    public void calibrate() throws ProfilingException {
        calibration.executeLocalBenchmarks();
    }

}
