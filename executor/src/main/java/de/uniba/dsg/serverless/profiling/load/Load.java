package de.uniba.dsg.serverless.profiling.load;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import de.uniba.dsg.serverless.profiling.executor.ProfilingException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class Load {
    @Expose
    public final CPULoad cpuLoad;
    @Expose
    public final MemoryLoad memoryLoad;
    @Expose
    public final IOLoad networkLoad;

    public static final Path FOLDER = Paths.get("executor", "src", "main", "resources");

    public Load(CPULoad cpuLoad, MemoryLoad memoryLoad, IOLoad networkLoad) {
        this.cpuLoad = cpuLoad;
        this.memoryLoad = memoryLoad;
        this.networkLoad = networkLoad;
    }

    public static Load fromFile(Path fileName) throws ProfilingException {
        Gson parser = new Gson();
        try {
            Reader reader = new BufferedReader(new FileReader(FOLDER.resolve(fileName).toString()));
            return parser.fromJson(reader, Load.class);
        } catch (IOException e) {
            throw new ProfilingException("Resource limits could not be read. ", e);
        }
    }

}

