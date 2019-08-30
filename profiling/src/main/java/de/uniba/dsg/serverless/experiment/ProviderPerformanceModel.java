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

public class ProviderPerformanceModel {
    private SimpleRegression regression;

    public ProviderPerformanceModel() {
        regression = new SimpleRegression();
    }

    public ProviderPerformanceModel(Path providerCalibration) throws ProfilingException {
        regression = new SimpleRegression();
        addProviderCalibration(providerCalibration);
    }

    /**
     * Adds a measurement to the regression model.
     *
     * @param providerCalibration Path to the provider calibration CSV file
     * @throws ProfilingException when the file is not well formed.
     */
    public void addProviderCalibration(Path providerCalibration) throws ProfilingException {
        try {
            Reader reader = Files.newBufferedReader(providerCalibration);
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new ProfilingException("Corrupt input file. List of records is empty.");
            }
            CSVRecord performanceMeasurement = records.get(0);
            parser.getHeaderMap()
                    .keySet()
                    .forEach(mem -> regression.addData(Integer.parseInt(mem), Double.parseDouble(performanceMeasurement.get(mem))));
        } catch (IOException e) {
            throw new ProfilingException();
        }
    }

    /**
     * Predicts the gFlops at using a certain memory
     *
     * @param memory setting used
     * @return expected gFlops
     */
    public double getGflops(int memory) {
        return regression.predict(memory);
    }


    @Override
    public String toString() {
        return "Provider Performance Model: f(x) = " + regression.getSlope() + " x + " + regression.getIntercept() + " (R=" + regression.getR() + ")";
    }
}
