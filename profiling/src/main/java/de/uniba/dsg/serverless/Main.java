package de.uniba.dsg.serverless;

import de.uniba.dsg.serverless.experiment.Experiment;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        runExperiment("AWSPrimaryExperiment7.json");
    }

    public static void runExperiment(String ex) {
        try {
            Experiment experiment = Experiment.fromFile(ex);
            experiment.calibrate();
            experiment.profile();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .ifPresent(System.err::println);
            e.printStackTrace();
        }
    }
}
