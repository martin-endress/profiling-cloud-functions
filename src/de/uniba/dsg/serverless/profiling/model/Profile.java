package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;

import java.util.List;

public class Profile {

    private final int length;
    private final List<Statistics> stats;
    private final InspectContainerResponse additional;

    public Profile(List<Statistics> stats, InspectContainerResponse additional) {
        length = stats.size();
        this.stats = stats;
        this.additional = additional;
    }
}
