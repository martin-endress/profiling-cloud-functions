package de.uniba.dsg.serverless.profiling.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class MetricsUtil {

    /**
     * Parses from and to and returns the difference between the two instances in milliseconds.
     *
     * @param from Start to formatted as an ISO_DATE_TIME (e.g. 2019-01-01T10:10:30.1337Z)
     * @param to   Start to formatted as an ISO_DATE_TIME
     * @return difference in milliseconds
     * @throws ProfilingException when from or to are not formatted as ISO_DATE_TIME
     */
    public static long timeDifference(String from, String to) throws ProfilingException {
        try {
            LocalDateTime startTime = LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime currentTime = LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME);
            return Duration.between(startTime, currentTime).toMillis();
        } catch (DateTimeParseException e) {
            throw new ProfilingException("Date does not have the correct format. from:" + from + " to:" + to, e);
        }
    }

    /**
     * Parses the time as ISO_DATE_TIME
     *
     * @param time Time to be formatted as an ISO_DATE_TIME (e.g. 2019-01-01T10:10:30.1337Z)
     * @return time as long
     * @throws ProfilingException when the time is not of ISO_DATE_TIME format
     */
    public static long parseTime(String time) throws ProfilingException {
        try {
            return LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.ofHours(0)).toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new ProfilingException("Date does not have the correct format. time:" + time);
        }
    }

    /**
     * Executes a given command on the bash. (only non root commands are possible)
     *
     * @param command command to execute.
     * @return the output of the command
     * @throws ProfilingException if the execution resulted in an error or the execution failed.
     */
    public static String executeCommand(String command) throws ProfilingException {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            process.waitFor();

            String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
            String errorOutput = CharStreams.toString(new InputStreamReader(process.getErrorStream(), Charsets.UTF_8));
            if (!errorOutput.isEmpty()) {
                throw new ProfilingException("Command resulted in an Error: " + errorOutput);
            }
            return output;
        } catch (IOException | InterruptedException e) {
            throw new ProfilingException("Could not execute command.", e);
        }
    }

}
