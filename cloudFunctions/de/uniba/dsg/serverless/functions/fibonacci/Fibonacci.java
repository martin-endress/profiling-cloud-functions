package de.uniba.dsg.serverless.functions.fibonacci;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public class Fibonacci implements RequestHandler<String, Response> {

    private static final String CONTAINER_ID = UUID.randomUUID().toString();

    @Override
    public Response handleRequest(String param, Context context) {
        // String nString = parameters.getOrDefault("n", "");

        if (!isNumeric(param)) {
            return createErrorResponse("Please pass a valid number 'n'.", context.getAwsRequestId());
        }

        long n = Long.parseLong(param);
        long result = fibonacci(n);
        System.out.println(result);

        return createSuccessResponse(String.valueOf(result), context.getAwsRequestId());
    }

    public long fibonacci(long n) {
        if (n <= 1) {
            return 1;
        } else {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }

    public Response createErrorResponse(String message, String requestId) {
        Response response = new Response("[400] " + message, requestId, CONTAINER_ID);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String responseJSON = mapper.writeValueAsString(response);
            throw new LambdaException(responseJSON);
        } catch (JsonProcessingException e) {
            throw new LambdaException("{ \"result\": \"[400] Error while creating JSON response.\" }");
        }
    }

    public Response createSuccessResponse(String message, String requestId) {
        return new Response(message, requestId, CONTAINER_ID);
    }

    public boolean isNumeric(String value) {
        return value.matches("\\d+");
    }

}
