package de.uniba.dsg.serverless.profiling.profiling;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;
import jdk.internal.util.xml.impl.Input;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ContainerProfiling {

    public final String IMAGE_NAME = "mendress/profiling";
    private final String IS_RUNNING = "running";
    private final DockerClient client;
    private String containerId;

    public ContainerProfiling() {
        client = DockerClientBuilder.getInstance().build();
        this.containerId = "";
    }

    public ContainerProfiling(String containerId) {
        client = DockerClientBuilder.getInstance().build();
        this.containerId = containerId;
    }

    public String buildContainer() throws ProfilingException {
        // FIXME
        AsyncResultCallback<BuildResponseItem> resultCallback = new AsyncResultCallback();
        client.buildImageCmd(new File("."))
                .withTags(new HashSet<>(Arrays.asList(IMAGE_NAME)))
                .exec(resultCallback);
        BuildResponseItem buildResponse;
        try {
            buildResponse = resultCallback.awaitResult();
            System.out.println(buildResponse.toString());
            System.out.println(buildResponse.getAux().getDigest());
            System.out.println(buildResponse.getAux().toString());
        } catch (RuntimeException e) {
            throw new ProfilingException(e);
        }
        if (!buildResponse.isBuildSuccessIndicated()) {
            String error = Optional
                    .ofNullable(buildResponse.getStream())
                    .orElse("");
            throw new ProfilingException("Build was unsuccessful. " + error);
        }
        return buildResponse.toString();
    }

    public String startContainer() {
        return startContainer("");
    }

    /**
     * @param envParams
     * @return
     */
    public String startContainer(String envParams) {
        CreateContainerResponse container = client
                .createContainerCmd(IMAGE_NAME)
                .withEnv(envParams)
                //.withHostConfig(new HostConfig().withNetworkMode("host"))
                .withAttachStdin(true)
                .exec();
        containerId = container.getId();
        client.startContainerCmd(containerId).exec();
        return containerId;
    }

    public long getStartedAt() throws ProfilingException {
        String startedAt = client.inspectContainerCmd(containerId)
                .exec()
                .getState()
                .getStartedAt();
        return new MetricsUtil().parseTime(startedAt);
    }

    /**
     * Blocking function that retrieves all statistics (docker stats) of the container until it terminates.
     *
     * @return List of statistics
     * @throws ProfilingException When the Thread is interrupted or
     */
    public List<Statistics> logStatistics() throws ProfilingException {
        List<Statistics> statistics = new ArrayList<>();
        Optional<Statistics> nextRead = getNextStatistics();
        while (nextRead.isPresent()) {
            Statistics next = nextRead.get();
            System.out.println(next.getRead());
            statistics.add(next);
            nextRead = getNextStatistics();
        }
        return statistics;
    }

    /**
     * Blocking function that returns the current properties of the container (docker inspect)
     *
     * @return the inspect result
     * @throws ProfilingException when the container is not found
     */
    public InspectContainerResponse inspectContainer() throws ProfilingException {
        try {
            return client.inspectContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            throw new ProfilingException(e);
        }
    }

    private String getLatestRunningContainerId() throws ProfilingException {
        for (Container c : this.client.listContainersCmd().exec()) {
            if (IMAGE_NAME.equals(c.getImage()) && IS_RUNNING.equals(c.getState())) {
                return c.getId();
            }
        }
        throw new ProfilingException("No running containers found. Container must be a running " + IMAGE_NAME + " image.");
    }

    /**
     * Blocking function that waits until the next statistics of the container is present. (docker stats)
     *
     * @return If present, statistics of next read. Empty otherwise.
     * @throws ProfilingException Exception in callback termination.
     */
    public Optional<Statistics> getNextStatistics() throws ProfilingException {
        AsyncResultCallback<Statistics> callback = new AsyncResultCallback<>();
        client.statsCmd(containerId).exec(callback);
        Statistics s;
        try {
            s = callback.awaitResult();
            callback.close();
        } catch (RuntimeException | IOException e) {
            throw new ProfilingException(e);
        }
        if (s.getMemoryStats() == null || s.getMemoryStats().getUsage() == null) {
            // Container has terminated
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public void getFileFromContainer(String containerPath, Path localPath) throws ProfilingException {
        // TODO FIX THIS
        try (InputStream input = client.copyArchiveFromContainerCmd(containerId, containerPath).exec()) {
            //byte[] buffer = new byte[input.available()];
            //input.read(buffer);

            //Files.write(localPath, buffer);
            FileUtils.copyInputStreamToFile(input, localPath.toFile());
        } catch (IOException e) {
            throw new ProfilingException(e);
        }
    }

}