package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class Profile {

    public static final Path OUTPUT_FOLDER = Paths.get("profiles");

    public final int length;
    public final List<Metrics> metrics;

    private final InspectContainerResponse additional;
    public final LocalDateTime started;
    public final LocalDateTime finished;

    public Profile(List<Metrics> metrics, InspectContainerResponse additional) throws ProfilingException {
        if (metrics == null || metrics.isEmpty()) {
            throw new ProfilingException("Metrics must be a non empty List.");
        }
        validateProfile(additional);
        this.length = metrics.size();
        this.metrics = metrics;
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
        lines.add(getHeader());
        for (Metrics m : metrics) {
            lines.add(m.toString());
        }
        Path path = OUTPUT_FOLDER.resolve(getUniqueFileName());
        try {
            Files.createDirectories(OUTPUT_FOLDER);
            Files.write(path, lines);
        } catch (IOException e) {
            throw new ProfilingException("Could not write to " + path, e);
        }
    }

    private String getHeader() {
        return String.join(",", metrics.get(0).relevantMetrics);
    }

    private String getUniqueFileName() {
        String resource = metrics.get(0).getClass().getSimpleName();
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
        return "Profile of container " + additional.getImageId() + " was created at " + started + ". size: " + metrics.size() + ".";
    }
}
