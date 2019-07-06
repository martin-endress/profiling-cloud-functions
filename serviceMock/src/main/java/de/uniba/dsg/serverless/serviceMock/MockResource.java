package de.uniba.dsg.serverless.serviceMock;

import de.uniba.dsg.serverless.serviceMock.model.ResponseGenerator;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api/")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
public class MockResource {

    public static final int BAD_REQUEST = 400;

    @GET
    @Path("/getResponse")
    public String getResponse(@QueryParam("delay") String delay, @QueryParam("size") String size) {
        if (delay == null) {
            delay = "0";
        }
        if (size == null) {
            size = "1";
        }
        ResponseGenerator r = new ResponseGenerator(size, delay);
        return r.getDelayedResponse();
    }


}
