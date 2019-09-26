package de.uniba.dsg.serverless.profiling.load;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.profiling.executor.ProfilingException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IOLoad implements Runnable {

    @Expose
    public final int from;
    @Expose
    public final int to;
    @Expose
    public final long size;
    @Expose
    public final long time;

    private final WebTarget target;

    /**
     * @param from start interval (number of requests per second)
     * @param to   end interval (number of requests per second)
     * @param size Size of the request/response payload
     * @param time total time
     * @throws ProfilingException if respective env parameters are not set
     */
    public IOLoad(int from, int to, int size, long time) throws ProfilingException {
        String ip = System.getenv("MOCK_IP");
        String port = System.getenv("MOCK_PORT");
        if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
            throw new ProfilingException("Environment parameters MOCK_IP and MOCK_PORT must be present.");
        }
        String url = "http://" + ip + ":" + port + "/api/getResponse/";

        this.from = Math.max(0, from);
        this.to = Math.max(0, to);
        this.size = size;
        this.time = time;

        target = ClientBuilder.newClient().target(url);
    }

    @Override
    public void run() {
        List<Integer> loadLog = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time;

        long interval = 1_000;

        long nextInvocation = startTime;
        System.out.println(startTime);
        System.out.println(endTime);

        while (System.currentTimeMillis() < endTime) {
            float progress = (nextInvocation - startTime) / (1F * (endTime - startTime));
            int load = Math.round(from + (to - from) * progress);
            IntStream.range(0, load)
                    .parallel()
                    .forEach(a -> invokeFunction(0, (int) size));

            nextInvocation += interval;
            long toNextInvocation = nextInvocation - System.currentTimeMillis();
            if (toNextInvocation < 0) {
                System.out.println("DELTA NEXT INVOCATION" + toNextInvocation);
            }
            loadLog.add(load);

            toNextInvocation = Math.max(toNextInvocation, 0);

            Uninterruptibles.sleepUninterruptibly(toNextInvocation, TimeUnit.MILLISECONDS);
        }
        writeLoadToFile(loadLog, "load");
    }

    private String invokeFunction(int delay, int size) {
        return target
                .queryParam("delay", delay)
                .queryParam("size", size)
                .request(MediaType.TEXT_PLAIN)
                .get()
                .toString();
    }

    private Path writeLoadToFile(List<Integer> invocations, String fileName) {
        Path path = Paths.get("logs", fileName + ".csv");

        try {
            Files.createDirectories(path.getParent());
            List<String> lines1 = IntStream.range(0, invocations.size())
                    .mapToObj(i -> i + "," + invocations.get(i))
                    .collect(Collectors.toList());
            List<String> lines = invocations.stream().map(t -> t.toString()).collect(Collectors.toList());
            lines1.add(0, "time,invocations");
            return Files.write(path, lines1, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Unable to write load to file: " + e.getMessage());
            return path;
        }
    }

}
