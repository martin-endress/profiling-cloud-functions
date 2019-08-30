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
    private final AmazonS3 s3;
    private final WebTarget lambdaTarget;

    private final String bucketName;
    private final String apiKey;

    /**
     * Creates an AWSClient
     *
     * @param targetUrl  target URL of linpack calibration
     * @param apiKey     AWS api key
     * @param bucketName bucket name where results are stored
     * @throws ProfilingException if the AWS S3 Client could not be created (check credentials and resources/awsEndpointInfo.json)
     */
    public AWSClient(String targetUrl, String apiKey, String bucketName) throws ProfilingException {
        try {
            s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();
        } catch (AmazonServiceException e) {
            throw new ProfilingException("AWS could not be accessed.", e);
        }
        ClientConfig configuration = new ClientConfig();
        // Avoid AWS Gateway Timeout by using local timeout of 1s
        configuration = configuration.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        configuration = configuration.property(ClientProperties.READ_TIMEOUT, 1000);
        Client client = ClientBuilder.newClient(configuration);
        lambdaTarget = client.target(targetUrl);
        this.bucketName = bucketName;
        this.apiKey = apiKey;
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
                    .header("x-api-key", apiKey)
                    .get();
        } catch (ProcessingException ignored) {
            // expected, since the AWS gateway timeout < function execution
            return;
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
            ListObjectsV2Result result = s3.listObjectsV2(bucketName);
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
        S3Object o = s3.getObject(bucketName, keyName);
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
