package de.uniba.dsg.serverless.serviceMock;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class Main {

    private static final String serverUri = "http://localhost:9000/";

    public static void main(String[] args) {
        URI baseUri = UriBuilder.fromUri(serverUri).build();
        ResourceConfig config = new ResourceConfig(MockResource.class);
        JdkHttpServerFactory.createHttpServer(baseUri, config);
    }

}
