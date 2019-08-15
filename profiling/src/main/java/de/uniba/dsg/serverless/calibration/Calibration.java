package de.uniba.dsg.serverless.calibration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import de.uniba.dsg.serverless.profiling.profiling.ContainerProfiling;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Calibration {

    private static final String LINPACK_DOCKERFILE = "linpack/local/Dockerfile";
    private static final String LINPACK_IMAGE = "mendress/linpack";
    private static final String CONTAINER_RESULT_FOLDER = "/usr/src/linpack/result/"; // specified by linpack benchmark
    private static final int LINPACK_ARRAY_SIZE = 200;

    private static final String BUCKET_NAME = "martin-linpack"; // This needs to be created first

    public static final int[] MEMORY_SIZES = {128, 256, 512, 1024, 2048, 3008};

    private final Path calibrationFolder;

    public Calibration(String name) {
        calibrationFolder = Paths.get("calibration", name);
    }

    public void executeLocalBenchmark() throws ProfilingException {
        ContainerProfiling linpack = new ContainerProfiling(LINPACK_DOCKERFILE, LINPACK_IMAGE);
        linpack.buildContainer();
        linpack.startContainer(Collections.singletonMap("LINPACK_ARRAY_SIZE", String.valueOf(LINPACK_ARRAY_SIZE)));
        int statusCode = linpack.awaitTermination();
        if (statusCode != 0) {
            throw new ProfilingException("Benchmark failed. (status code = " + statusCode + ")");
        }
        linpack.getFilesFromContainer(CONTAINER_RESULT_FOLDER, calibrationFolder);
        Path output = calibrationFolder.resolve("result/output.csv");
        if (!Files.exists(output)) {
            throw new ProfilingException("Benchmark failed. linpack/output.csv does not exist.");
        }
        BenchmarkParser parser = new BenchmarkParser(calibrationFolder, output);
        parser.parseBenchmark();
    }

    public void executeAWSBenchmark() throws ProfilingException {
        Client client = ClientBuilder.newClient();
        AWSConfig config = AWSConfig.getConfig();
        WebTarget webTarget = client.target(config.targetUrl);
        List<String> s3keyNames = new ArrayList<>();
        for (int memory : MEMORY_SIZES) {
            String path = "/linpack_" + memory;
            Response response = webTarget.path(path)
                    .request()
                    .header("x-api-key", config.apiKey)
                    .get();
            if (response.getStatus() != 200) {
                throw new ProfilingException("AWS Benchmark failed. (Response=" + response.getStatus() + "; Time=" + response.getDate().toString() + ")");
            }
            s3keyNames.add(response.readEntity(String.class));
            break;
        }
        for (String s : s3keyNames) {
            System.out.println(s);
            getFileFromBucket(s, "");
        }
    }

    /**
     * Reads a file from S3 and stores it to the output path
     *
     * @param keyName    path inside the S3 bucket
     * @param outputPath local path
     * @throws ProfilingException If the file could not be accessed.
     */
    private void getFileFromBucket(String keyName, String outputPath) throws ProfilingException {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build();

        try {
            S3Object o = s3.getObject(BUCKET_NAME, keyName);
            try (S3ObjectInputStream objectContent = o.getObjectContent(); FileOutputStream fileOutputStream = new FileOutputStream(new File(outputPath))) {
                byte[] readBuffer = new byte[1024];
                int length = 0;
                while ((length = objectContent.read(readBuffer)) > 0) {
                    fileOutputStream.write(readBuffer, 0, length);
                }
            } catch (IOException e) {
                throw new ProfilingException("File could not be read.", e);
            }
        } catch (AmazonServiceException e) {
            throw new ProfilingException("AWS could not be accessed.", e);
        }
    }
    
}

@XmlRootElement
class AWSConfig {
    private static final String ENDPOINT_INFO = "profiling/src/main/resources/awsEndpointInfo.json";

    @Expose
    public String targetUrl;
    @Expose
    public String apiKey;

    public AWSConfig(String targetUrl, String apiKey) {
        this.targetUrl = targetUrl;
        this.apiKey = apiKey;
    }

    public static AWSConfig getConfig() throws ProfilingException {
        Gson parser = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        ;
        try {
            Reader reader = new BufferedReader(new FileReader(ENDPOINT_INFO));
            return parser.fromJson(reader, AWSConfig.class);
        } catch (IOException e) {
            throw new ProfilingException("Resource limits could not be read. ", e);
        }
    }

}
