package de.uniba.dsg.serverless.functions.mocky;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class Mocky implements RequestHandler<String, Response> {

    public static final String MOCKY_URL = "http://www.mocky.io";

    // http://www.mocky.io/v2/5d09fe3e3400001129d831d0
    private Client client = ClientBuilder.newClient();
    private WebTarget target = client.target(MOCKY_URL);


    @Override
    public Response handleRequest(String input, Context context) {
        Invocation.Builder invocationBuilder = target.path("v2/5d0ce1cb3500004d00b89b84")
                .queryParam("mocky-delay", "100ms")
                .request(MediaType.APPLICATION_JSON);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 30_000;
        long currentTime = System.currentTimeMillis();
        while (currentTime < endTime) {
            System.out.println("requesting resource... \nremaining time =" + (endTime - currentTime));
            invocationBuilder.get(String.class);
            currentTime = System.currentTimeMillis();
        }
        return new Response();
    }
}
