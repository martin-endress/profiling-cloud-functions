package de.uniba.dsg.serverless.serviceMock;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class Main {

    public static void main(String[] args) {
        String port = System.getenv("MOCK_PORT");
        if (port == null || port.isEmpty()) {
            throw new RuntimeException("Environment parameters MOCK_IP and MOCK_PORT must be present.");
        }
        String serverUri = "http://localhost:" + port + "/";
        URI baseUri = UriBuilder.fromUri(serverUri).build();
        ResourceConfig config = new ResourceConfig(MockResource.class);
        JdkHttpServerFactory.createHttpServer(baseUri, config);
    }

}
