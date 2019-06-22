package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Profile {

    public static final Path OUTPUT_FOLDER = Paths.get("profiles");

    public final int length;
    public final List<MemoryMetrics> stats;

    private final InspectContainerResponse additional;
    public final String started;
    public final String finished;

    public Profile(List<MemoryMetrics> stats, InspectContainerResponse additional) throws ProfilingException {
        validateProfile(additional);
        this.length = stats.size();
        this.stats = stats;
        this.additional = additional;

        started = additional.getState().getStartedAt();
        finished = additional.getState().getFinishedAt();
    }

    public void save() throws ProfilingException {
        List<String> lines = new ArrayList<>();
        String header = MemoryMetrics.RELEVANT_METRICS.stream().collect(Collectors.joining(","));
        lines.add(header);
        for (MemoryMetrics m : stats) {
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
        //TODO improve
        return started;
    }

    private void validateProfile(InspectContainerResponse additional) throws ProfilingException {
        if (additional.getState().getExitCode() != 0 && "exited".equals(additional.getState().getStatus())) {
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
