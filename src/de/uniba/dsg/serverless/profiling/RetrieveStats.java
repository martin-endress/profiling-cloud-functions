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
            profiling.startContainer();
            System.out.println("Container started.");

            List<Statistics> stats = profiling.logStatistics();
            InspectContainerResponse additional = profiling.inspectContainer();
            Profile p = new Profile(stats, additional);
            System.out.println("Profile created");

            p.print();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            return;
        }
    }

}
