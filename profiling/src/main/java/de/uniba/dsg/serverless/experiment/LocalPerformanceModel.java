package de.uniba.dsg.serverless.experiment;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LocalPerformanceModel {

    private SimpleRegression regression = new SimpleRegression();

    private LocalPerformanceModel() {
    }

    /**
     * Creates a LocalPerformanceModel
     *
     * @param localCalibration Path to the local calibration CSV file
     * @throws ProfilingException when the file is not well formed.
     */
    public LocalPerformanceModel(Path localCalibration) throws ProfilingException {
        try {
            Reader reader = Files.newBufferedReader(localCalibration);
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new ProfilingException("Corrupt input file. " + localCalibration.toString());
            }
            Map<String, String> measurements = records.get(0).toMap();
            for (String key : measurements.keySet()) {
                regression.addData(Double.parseDouble(key), Double.parseDouble(measurements.get(key)));
            }
        } catch (IOException e) {
            throw new ProfilingException();
        }
    }

    public static LocalPerformanceModel withFixedLimit(double limit) {
        return new LocalPerformanceModel() {
            @Override
            public double estimateQuota(double gFlops) {
                return limit;
            }
        };
    }

    /**
     * Predict gFlops based on the CPU quota
     *
     * @param quota CPU quota
     * @return predicted gFlops performance
     */
    public double getGFlops(double quota) {
        return regression.predict(quota);
    }

    /**
     * Estimates the required quota to match gFlops performance on the local machine.
     *
     * @param gFlops desired gFlops
     * @return CPU quota to achieve gFlops
     */
    public double estimateQuota(double gFlops) {
        return (gFlops - regression.getIntercept()) / regression.getSlope();
    }

    @Override
    public String toString() {
        return "Local Performance Model: f(x) = " + regression.getSlope() + " x + " + regression.getIntercept() + " (R=" + regression.getR() + ")";
    }
}