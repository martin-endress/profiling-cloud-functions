package de.uniba.dsg.serverless.profiling.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import java.io.File;
import java.io.IOException;
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

    public String startContainer(String envParams) {
        CreateContainerResponse container = client.createContainerCmd(IMAGE_NAME).withEnv(envParams).withAttachStdin(true).exec();
        client.startContainerCmd(container.getId()).exec();
        containerId = container.getId();
        return containerId;
    }

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

    public InspectContainerResponse inspectContainer() throws ProfilingException {
        try {
            InspectContainerResponse response = client.inspectContainerCmd(containerId).exec();
            return response;
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
    private Optional<Statistics> getNextStatistics() throws ProfilingException {
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

}