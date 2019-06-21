package de.uniba.dsg.serverless.profiling;


import de.uniba.dsg.serverless.profiling.model.ProfilingException;

public class Main {

    //System.setProperty("log4j.configurationFile", "log4j.properties");
    //private final static Logger logger = LogManager.getLogger("profiling");

    public static void main(String[] args) {
        try {
            System.out.println("handling request");
            RequestHandlerExecutor loader = new RequestHandlerExecutor("de.uniba.dsg.serverless.functions.mocky.Mocky");
            loader.invokeHandleRequest("");
        } catch (ProfilingException e) {
            //logger.fatal(e.getMessage());
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println(e.getCause().getMessage());
                //logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
        }
    }
}
