package de.uniba.dsg.serverless.calibration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AWSClient {

    private final AWSConfig config;
    private final AmazonS3 s3;
    private final WebTarget lambdaTarget;

    /**
     * Creates an AWSClient
     *
     * @throws ProfilingException if the AWS S3 Client could not be created (check credentials and resources/awsEndpointInfo.json)
     */
    public AWSClient() throws ProfilingException {
        config = AWSConfig.getConfig();
        try {
            s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
        } catch (AmazonServiceException e) {
            throw new ProfilingException("AWS could not be accessed.", e);
        }
        ClientConfig configuration = new ClientConfig();
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, 1000);
        Client client = ClientBuilder.newClient(configuration);
        lambdaTarget = client.target(config.targetUrl);
    }

    /**
     * Invokes the Benchmark function with respective memory and returns the response.
     *
     * @param memory memory setting (specified in linpack/aws)
     * @throws ProfilingException if the response code is not 200
     */
    public void invokeBenchmarkFunctions(int memory, String folderName) throws ProfilingException {
        String path = "/linpack_" + memory;
        try {
            lambdaTarget.path(path)
                    .queryParam("experiment", folderName)
                    .request()
                    .header("x-api-key", config.apiKey)
                    .get();
        } catch (ProcessingException expected) {
            // expected, since the timeout < function execution
            // TODO remove this antipattern...
        }
    }

    /**
     * Blocking function that waits for a file to be created in the S3 bucket. If the timeout is reached, an exception is thrown
     *
     * @param keyName key of the file
     * @param timeout timeout in seconds
     * @throws ProfilingException thrown if the S3 bucket is not available or the timeout is exceeded.
     */
    public void waitForBucketObject(String keyName, long timeout) throws ProfilingException {
        timeout = timeout * 1_000; // convert to ms
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + timeout) {
            ListObjectsV2Result result = s3.listObjectsV2(config.bucketName);
            List<S3ObjectSummary> objects = result.getObjectSummaries();
            if (objects.stream().anyMatch(a -> keyName.equals(a.getKey()))) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        }
        throw new ProfilingException("Timeout exceeded, no object found.");
    }

    /**
     * Reads a file from S3 and stores it to the output path
     *
     * @param keyName    path inside the S3 bucket
     * @param outputPath local path
     * @throws ProfilingException If the file could not be accessed.
     */
    public void getFileFromBucket(String keyName, Path outputPath) throws ProfilingException {
        S3Object o = s3.getObject(config.bucketName, keyName);
        try (S3ObjectInputStream objectContent = o.getObjectContent(); FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile())) {
            byte[] readBuffer = new byte[1024];
            int length = 0;
            while ((length = objectContent.read(readBuffer)) > 0) {
                fileOutputStream.write(readBuffer, 0, length);
            }
        } catch (IOException e) {
            throw new ProfilingException("File could not be read.", e);
        }
    }

}

@XmlRootElement
class AWSConfig {
    private static final String ENDPOINT_INFO = "profiling/src/main/resources/awsEndpointInfo.json";

    @Expose
    String targetUrl;
    @Expose
    String apiKey;
    @Expose
    String bucketName;

    public AWSConfig(String targetUrl, String apiKey) {
        this.targetUrl = targetUrl;
        this.apiKey = apiKey;
    }

    static AWSConfig getConfig() throws ProfilingException {
        Gson parser = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            Reader reader = new BufferedReader(new FileReader(ENDPOINT_INFO));
            return parser.fromJson(reader, AWSConfig.class);
        } catch (IOException e) {
            throw new ProfilingException("Resource limits could not be read. ", e);
        }
    }

}
