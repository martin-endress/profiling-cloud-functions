package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Profile {

    public final int length;
    public final List<Metrics> metrics;
    public final List<Metrics> deltaMetrics;
    public final InspectContainerResponse additional;
    public final Metrics lastMetrics;
    public final LocalDateTime started;
    public final LocalDateTime finished;

    public double kFlops; // TODO maybe change this, quite ugly

    public Profile(List<Metrics> metrics, InspectContainerResponse additional) throws ProfilingException {
        if (metrics == null || metrics.isEmpty()) {
            throw new ProfilingException("Metrics must be a non empty List.");
        }
        validateProfile(additional);
        this.length = metrics.size();
        this.metrics = metrics;
        this.deltaMetrics = calculateDeltaMetrics();
        this.additional = additional;
        this.lastMetrics = metrics.get(length - 1);

        try {
            started = LocalDateTime.parse(additional.getState().getStartedAt(), DateTimeFormatter.ISO_DATE_TIME);
            finished = LocalDateTime.parse(additional.getState().getFinishedAt(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ProfilingException(e);
        }
    }

    private List<Metrics> calculateDeltaMetrics() throws ProfilingException {
        List<Metrics> l = new ArrayList<>();
        l.add(metrics.get(0));
        l.addAll(IntStream.range(0, metrics.size() - 1)
                .mapToObj(i -> new Metrics(metrics.get(i), metrics.get(i + 1)))
                .collect(Collectors.toList()));
        return l;
    }

    public void setkFlops(double kFlops) {
        this.kFlops = kFlops;
    }

    public Path save(Path folder) throws ProfilingException {
        try {
            Files.createDirectories(folder);
            saveCSV(folder);
            saveAdditionalInformation(folder);
            return folder;
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }

    private void saveCSV(Path folder) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(toString());          // python doesn't read first line (TODO -> python)
        lines.add(getHeader());         // header
        lines.add(getEmptyMetrics());   // adds empty metrics with (<start time>,0,0,..)
        for (Metrics m : metrics) {
            lines.add(m.toString());
        }
        Files.write(folder.resolve("metrics.csv"), lines);
    }

    private void saveAdditionalInformation(Path folder) throws IOException, ProfilingException {
        Gson jsonParser = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        ProfileMetaInfo info = new ProfileMetaInfo(this);
        Files.write(folder.resolve("meta.json"), jsonParser.toJson(info).getBytes());
    }

    private String getHeader() {
        List<String> headerEntries = new ArrayList<>();
        headerEntries.add("timeStamp");
        headerEntries.addAll(metrics.get(0).relevantMetrics);
        return String.join(",", headerEntries);
    }

    private String getEmptyMetrics() {
        return additional.getState().getStartedAt() + StringUtils.repeat(",0", metrics.get(0).relevantMetrics.size());
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
