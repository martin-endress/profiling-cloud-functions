package de.uniba.dsg.serverless.profiling;


import de.uniba.dsg.serverless.profiling.model.ProfilingException;

public class Main {

    //System.setProperty("log4j.configurationFile", "log4j.properties");
    //private final static Logger logger = LogManager.getLogger("profiling");

    public static void main(String[] args) {
        try {
            RequestHandlerExecutor loader = new RequestHandlerExecutor("de.uniba.dsg.serverless.functions.fibonacci.Fibonacci");
            loader.invokeHandleRequest(args[0]);
        } catch (ProfilingException e) {
            //logger.fatal(e.getMessage());
            if (e.getCause() != null) {
                //logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
        }
    }
}
