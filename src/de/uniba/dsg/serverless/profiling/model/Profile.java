package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Profile {

    public static final Path OUTPUT_FOLDER = Paths.get("profiles");

    public final int length;
    public final List<Metrics> stats;

    private final InspectContainerResponse additional;
    public final LocalDateTime started;
    public final LocalDateTime finished;

    public Profile(List<Metrics> stats, InspectContainerResponse additional) throws ProfilingException {
        if (stats.isEmpty()) {
            throw new ProfilingException("Stats must not be null.");
        }
        validateProfile(additional);
        this.length = stats.size();
        this.stats = stats;
        this.additional = additional;

        try {
            started = LocalDateTime.parse(additional.getState().getStartedAt(), DateTimeFormatter.ISO_DATE_TIME);
            finished = LocalDateTime.parse(additional.getState().getFinishedAt(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ProfilingException(e);
        }
    }

    public void save() throws ProfilingException {
        List<String> lines = new ArrayList<>();
        lines.add(stats.get(0).getHeader());
        for (Metrics m : stats) {
            lines.add(m.toString());
        }
        Path path = OUTPUT_FOLDER.resolve(getUniqueFileName());
        try {
            Files.write(path, lines);
        } catch (IOException e) {
            throw new ProfilingException("could not write to " + path, e);
        }
    }

    private String getUniqueFileName() {
        String resource = stats.get(0).getClass().getSimpleName();
        String started = this.started.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return resource + " " + started;
    }

    private void validateProfile(InspectContainerResponse additional) throws ProfilingException {
        if (additional.getState().getExitCode() != 0 || !"exited".equals(additional.getState().getStatus())) {
            String exitCode = "exit code: " + additional.getState().getExitCode();
            String status = "status: " + additional.getState().getStatus();
            throw new ProfilingException(exitCode + " - " + status);
        }
    }

    @Override
    public String toString() {
        return "Profile of container " + additional.getImageId() + " was created at " + started + ". size: " + stats.size() + ".";
    }
}
