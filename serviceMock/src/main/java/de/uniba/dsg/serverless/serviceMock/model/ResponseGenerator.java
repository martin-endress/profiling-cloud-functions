package de.uniba.dsg.serverless.serviceMock.model;


import com.google.common.util.concurrent.Uninterruptibles;
import de.uniba.dsg.serverless.serviceMock.MockResource;
import org.apache.commons.lang.RandomStringUtils;

import javax.ws.rs.WebApplicationException;
import java.util.concurrent.TimeUnit;

public class ResponseGenerator {

    private final String response;
    private final int delay;
    private final int size;

    public ResponseGenerator(String size, String delay) throws WebApplicationException {
        try {
            this.delay = Integer.parseInt(delay);
            this.size = Integer.parseInt(size);
            response = RandomStringUtils.random(this.size, true, true);
        } catch (NumberFormatException e) {
            throw new WebApplicationException(e, MockResource.BAD_REQUEST);
        }
    }

    public String getDelayedResponse() {
        Uninterruptibles.sleepUninterruptibly(delay, TimeUnit.MILLISECONDS);
        return response;
    }
}
