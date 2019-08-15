package de.uniba.dsg.serverless.calibration;

public class BenchmarkResult {

    public double average;
    public double max;

    public BenchmarkResult(double average, double max) {
        this.average = average;
        this.max = max;
    }

}
