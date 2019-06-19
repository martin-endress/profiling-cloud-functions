package de.uniba.dsg.serverless.functions.mocky;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.Map;

public class Mocky implements RequestHandler<Map<String, String>, Response> {

    public static void main(String[] args) {
        new Mocky().handleRequest(null, null);
    }

    @Override
    public Response handleRequest(Map<String, String> input, Context context) {
        Client client = ClientBuilder.newClient();

        // http://www.mocky.io/v2/5d09fe3e3400001129d831d0
        WebTarget target = client.target("http://www.mocky.io/v2/5d0a0b883400005d29d83280");
        Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON);
        invocationBuilder.accept(MediaType.APPLICATION_JSON);
        String s = invocationBuilder.get(String.class);
        System.out.println(s);
        return null;
    }
}
