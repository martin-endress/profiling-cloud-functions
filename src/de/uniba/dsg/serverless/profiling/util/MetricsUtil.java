package de.uniba.dsg.serverless.profiling.util;

import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetricsUtil {

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
