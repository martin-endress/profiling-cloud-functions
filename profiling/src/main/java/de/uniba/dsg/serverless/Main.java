package de.uniba.dsg.serverless;

import de.uniba.dsg.serverless.calibration.Calibration;
import de.uniba.dsg.serverless.profiling.StatsRetriever;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Main {

    private static final Path OUTPUT_FOLDER = Paths.get("profiles");

    public static void main(String[] args) {
        final String profileName = "Profile_" + DateTimeFormatter.ofPattern("MM.dd_HH.mm.ss").format(LocalDateTime.now());
        try {
            Calibration calibration = new Calibration("test3");
            //calibration.executeLocalBenchmark();
            calibration.executeAWSBenchmark();

            //new StatsRetriever(OUTPUT_FOLDER,profileName).retrieveStats();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            Optional.ofNullable(e.getCause())
                    .map(Throwable::getMessage)
                    .ifPresent(System.err::println);
            e.printStackTrace();
        }
    }
}
