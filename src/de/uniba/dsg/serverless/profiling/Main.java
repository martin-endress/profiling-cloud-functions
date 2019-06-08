package de.uniba.dsg.serverless.profiling;


import de.uniba.dsg.serverless.profiling.model.ProfilingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {


    public static void main(String[] args) {
        //System.setProperty("log4j.configurationFile", "log4j.properties");
        final Logger logger = LogManager.getLogger("profiling");

        ProfilingClassLoader loader = new ProfilingClassLoader();
        try {
            loader.invokeHandleRequest("de.uniba.dsg.serverless.functions.Fibonacci", args[0]);
        } catch (ProfilingException e) {
            logger.fatal(e.getMessage());
            if (e.getCause() != null) {
                logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
        }
    }
}
