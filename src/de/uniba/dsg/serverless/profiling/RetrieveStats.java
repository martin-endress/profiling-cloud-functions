package de.uniba.dsg.serverless.profiling;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import de.uniba.dsg.serverless.profiling.docker.ContainerProfiling;
import de.uniba.dsg.serverless.profiling.model.Profile;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.util.List;

public class RetrieveStats {


    public static void main(String[] args) {
        try {
            ContainerProfiling profiling = new ContainerProfiling();
            profiling.startContainer("JAVA_PARAMS=5");
            System.out.println("Container started.");
            List<Statistics> stats = profiling.logStatistics();
            InspectContainerResponse additional = profiling.inspectContainer();
            new Profile(stats, additional);
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            return;
        }
    }

}
