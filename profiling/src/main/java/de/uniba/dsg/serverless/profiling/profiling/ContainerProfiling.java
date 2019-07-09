package de.uniba.dsg.serverless.profiling.profiling;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.util.MetricsUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ContainerProfiling {

    public final String IMAGE_NAME = "mendress/profiling";
    private final String IS_RUNNING = "running";
    private final DockerClient client;
    private String containerId;

    public ContainerProfiling() {
        this("");
    }

    public ContainerProfiling(String containerId) {
        client = DockerClientBuilder.getInstance().build();
        this.containerId = containerId;
    }

    /**
     * Build the execution image defined in executor/Dockerfile.
     *
     * @return Image id
     * @throws ProfilingException if either
     */
    public String buildContainer() throws ProfilingException {
        File baseDir = new File(".");
        File dockerFile = new File("executor/Dockerfile");
        if (!baseDir.isDirectory() || !dockerFile.isFile()) {
            throw new ProfilingException("Failed to build docker image.");
        }
        try {
            return client.buildImageCmd(dockerFile)
                    .withBaseDirectory(baseDir)
                    .withDockerfile(dockerFile)
                    .withTags(new HashSet<>(Arrays.asList(IMAGE_NAME)))
                    .exec(new BuildImageResultCallback())
                    .awaitImageId();
        } catch (DockerClientException e) {
            throw new ProfilingException("Failed to build docker image.", e);
        }
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

    /**
     * Copies all Files in a directory (or a single file) to the desired location.
     * <p>
     * Example Usage:<br>
     * <code>profiling.getFilesFromContainer("/app/logs/", Paths.get("profiles/profileXYZ/logs/");</code>
     *
     * @param containerFolder absolute path to the folder or file inside the container
     * @param localFolder     path to folder for resulting file(s)
     * @throws ProfilingException
     */
    public void getFilesFromContainer(String containerFolder, Path localFolder) throws ProfilingException {
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
                client.copyArchiveFromContainerCmd(containerId, containerFolder).exec())) {
            unTar(tarStream, localFolder);
        } catch (IOException e) {
            throw new ProfilingException("Copying file(s) failed.", e);
        }
    }


    /**
     * Adapted from https://github.com/docker-java/docker-java/issues/991
     * <p>
     * Copies files to destination folder while creating subdirectories
     *
     * @param tarInputStream    input stream
     * @param destinationFolder destination
     * @throws IOException
     */
    private void unTar(TarArchiveInputStream tarInputStream, Path destinationFolder) throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
            if (tarEntry.isFile()) {
                Path filePath = destinationFolder.resolve(tarEntry.getName());
                Files.createDirectories(filePath.getParent());
                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    IOUtils.copy(tarInputStream, fos);
                }
            }
        }
        tarInputStream.close();
    }

}