package de.uniba.dsg.serverless.calibration;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;

public class Main implements RequestHandler<Dummy, Dummy> {

    private static final String BUCKET_NAME = "calibration-linpack";
    private final AmazonS3 s3Client = AmazonS3ClientBuilder
            .standard()
            .withRegion(Regions.EU_CENTRAL_1)
            .build();

    @Override
    public Dummy handleRequest(Dummy input, Context context) {
        String fileName = "linpack_aff/" + context.getMemoryLimitInMB();
        executeLinpack(fileName, false);
        executeLinpack(fileName + "_aff", true);
        return new Dummy();
    }

    /**
     * Executes a linpack benchmark and saves the results to S3 bucket
     *
     * @param fileName   file name
     * @param limitedCpu limit CPUs of the available process to 1 (instead of default 2)
     */
    private void executeLinpack(String fileName, boolean limitedCpu) {
        try {
            Process linpack = new ProcessBuilder("./linpack", "input").start();
            if (limitedCpu) {
                long pid = getPidOfProcess(linpack);
                new ProcessBuilder("taskset", "-pc", "0", String.valueOf(pid)).start();
            }
            linpack.waitFor();
            try (final Reader reader = new InputStreamReader(linpack.getInputStream())) {
                String text = CharStreams.toString(reader);
                s3Client.putObject(BUCKET_NAME, fileName, text);
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("error:" + e.getMessage());
        }
    }

    /**
     * Returns the PID of a process. can be replaced by process.pid() in Java 9. But this is not Java 9...
     *
     * @param p process
     * @return pid
     * @see <a href="https://stackoverflow.com/a/33171840">https://stackoverflow.com/a/33171840</a>
     */
    private static synchronized long getPidOfProcess(Process p) {
        if (p == null) {
            return -1;
        }
        Field f = null;
        try {
            f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            return f.getLong(p);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            return -1;
        } finally {
            if (f != null) {
                f.setAccessible(false);
            }
        }
    }

}
