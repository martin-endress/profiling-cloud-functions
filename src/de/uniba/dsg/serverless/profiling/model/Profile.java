package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;

import java.util.List;

public class Profile {

    private final int length;
    private final List<Statistics> stats;
    private final InspectContainerResponse additional;

    private String started;
    private String finished;

    public Profile(List<Statistics> stats, InspectContainerResponse additional) throws ProfilingException {
        validateProfile(additional);
        this.length = stats.size();
        this.stats = stats;
        this.additional = additional;
        setAdditional(additional);
    }

    public void print() {
        System.out.println("len " + length);
        for (Statistics s : stats) {
            System.out.println(s.getCpuStats().getCpuUsage().getTotalUsage());
        }
        System.out.println(started);
        System.out.println(finished);
    }

    private void validateProfile(InspectContainerResponse additional) throws ProfilingException {
        if (additional.getState().getExitCode() != 0 && "exited".equals(additional.getState().getStatus())) {
            String exitCode = "exit code: " + additional.getState().getExitCode();
            String status = "status: " + additional.getState().getStatus();
            throw new ProfilingException(exitCode + " - " + status);
        }
    }

    private void setAdditional(InspectContainerResponse additional) {
        started = additional.getState().getStartedAt();
        finished = additional.getState().getFinishedAt();
    }
}
