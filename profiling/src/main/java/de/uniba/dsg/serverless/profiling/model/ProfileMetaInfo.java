package de.uniba.dsg.serverless.profiling.model;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;

public class ProfileMetaInfo {

    public final String created;
    public final HostConfig hostConfig;
    public final String imageId;
    public final InspectContainerResponse.ContainerState state;

    public ProfileMetaInfo(InspectContainerResponse additional) {
        created = additional.getCreated();
        hostConfig = additional.getHostConfig();
        imageId = additional.getImageId();
        state = additional.getState();
    }


}
