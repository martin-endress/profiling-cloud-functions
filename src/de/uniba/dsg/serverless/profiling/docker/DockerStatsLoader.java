package de.uniba.dsg.serverless.profiling.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.BuildImageResultCallback;

import java.io.File;

public class DockerStatsLoader {

    private final DockerClient client;

    private String directory;

    private String repository;

    private String containerId;

    public DockerStatsLoader(DockerClient client, String directory) {
        this.client = client;
        this.directory = directory;
    }

    public void open() {
        client.buildImageCmd(new File(directory))
                .withNoCache(true)
                .exec(new BuildImageResultCallback())
                .awaitImageId();

        Image lastCreatedImage = client.listImagesCmd().exec().get(0);

        repository = lastCreatedImage.getRepoTags()[0];
        containerId = client.createContainerCmd(lastCreatedImage.getId()).exec().getId();
    }

    public String getContainerId() {
        return containerId;
    }
}