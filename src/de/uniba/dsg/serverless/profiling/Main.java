package de.uniba.dsg.serverless.profiling;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import de.uniba.dsg.serverless.profiling.docker.DockerStatsLoader;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

public class Main {

    //System.setProperty("log4j.configurationFile", "log4j.properties");
    //private final static Logger logger = LogManager.getLogger("profiling");

    public static void main(String[] args) {
        //testContainerDeploy();
        testInvocation(args);

    }

    private static void testContainerDeploy() {
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();
        DockerClient client = DockerClientBuilder.getInstance().build();
        DockerStatsLoader loader = new DockerStatsLoader(client, ".");
        loader.open();
    }

    private static void testInvocation(String[] args) {
        ProfilingClassLoader loader = new ProfilingClassLoader();
        try {
            loader.invokeHandleRequest("de.uniba.dsg.serverless.functions.Fibonacci", args[0]);
        } catch (ProfilingException e) {
            //logger.fatal(e.getMessage());
            if (e.getCause() != null) {
                //logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
        }
    }
}
