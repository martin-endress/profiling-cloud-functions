package de.uniba.dsg.serverless.profiling.util;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsUtil {

    /**
     * Creates a map containing performance metrics
     *
     * @param lines performance metrics as key value pairs separated by spaces <br>Example: <br>cache 1337L<br>cpu_usage 420L
     * @return Map
     * @throws ProfilingException input does not have the correct format
     */
    public Map<String, Long> fromLines(List<String> lines) throws ProfilingException {
        HashMap<String, Long> map = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length != 2) {
                throw new ProfilingException("Each line must be a key value pair separated by \" \". line:" + line);
            }
            long val;
            try {
                val = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                throw new ProfilingException("Parse error in line: " + line, e);
            }
            map.put(parts[0], val);
        }
        return map;
    }

    /**
     * Parses from and to and returns the difference between the two instances in milliseconds.
     *
     * @param from Start to formatted as an ISO_DATE_TIME (e.g. 2019-01-01T10:10:30.1337Z)
     * @param to   Start to formatted as an ISO_DATE_TIME
     * @return difference in milliseconds
     * @throws ProfilingException when from or to are not formatted as ISO_DATE_TIME
     */
    public long timeDifference(String from, String to) throws ProfilingException {
        try {
            LocalDateTime startTime = LocalDateTime.parse(from, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime currentTime = LocalDateTime.parse(to, DateTimeFormatter.ISO_DATE_TIME);
            return Duration.between(startTime, currentTime).toMillis();
        } catch (DateTimeParseException e) {
            throw new ProfilingException("Date does not have the correct format. from:" + from + " to:" + to, e);
        }
    }

}
