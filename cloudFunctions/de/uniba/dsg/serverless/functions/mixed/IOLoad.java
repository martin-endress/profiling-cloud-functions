package de.uniba.dsg.serverless.functions.mixed;

import de.uniba.dsg.serverless.profiling.model.ProfilingException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IOLoad implements Runnable {

    private final int from;
    private final int to;
    private final long time;

    // http://www.mocky.io/v2/5d09fe3e3400001129d831d0
    private final Client client;
    private static final String MOCKY_URL = "http://www.mocky.io";
    private final WebTarget target;
    private final Invocation.Builder invocationBuilder;

    /**
     * @param from start interval (number of requests per second)
     * @param to   end interval (number of requests per second)
     * @param size Size of the request/response payload
     * @param time total time
     */
    public IOLoad(int from, int to, int size, long time) {
        this.from = Math.max(0, from);
        this.to = Math.max(0, to);
        this.time = time;

        client = ClientBuilder.newClient();
        target = client.target(MOCKY_URL);
        invocationBuilder = target.path("v2/5d0ce1cb3500004d00b89b84")
                //.queryParam("mocky-delay", "1s")
                .request(MediaType.APPLICATION_JSON);
    }

    @Override
    public void run() {
        List<Integer> loadLog = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time;
        for (long invocation = startTime; System.currentTimeMillis() < endTime; invocation += 1000) {
            float progress = (System.currentTimeMillis() - startTime) / (1F * (endTime - startTime));
            int load = Math.round(from + (to - from) * progress);
            IntStream.range(0, load)
                    .parallel()
                    .forEach(a -> invocationBuilder.get());

            long toNextInvocation = invocation - System.currentTimeMillis();
            if (toNextInvocation < 0) {
                // TODO: do something
            }
            loadLog.add(load);

            toNextInvocation = Math.max(toNextInvocation, 0);

            try {
                Thread.sleep(toNextInvocation);
            } catch (InterruptedException ignored) {
            }
        }
        writeLoadToFile(loadLog, "load");
    }

    private Path writeLoadToFile(List<Integer> timestamps, String fileName) {
        Path path = Paths.get("logs", fileName + ".csv");

        try {
            Files.createDirectories(path.getParent());
            List<String> lines = timestamps.stream().map(t -> t.toString()).collect(Collectors.toList());
            return Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Unable to write load to file: " + e.getMessage());
        }
        return path;
    }

}
