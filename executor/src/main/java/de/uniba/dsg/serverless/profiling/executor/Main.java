package de.uniba.dsg.serverless.profiling.executor;


public class Main {

    //System.setProperty("log4j.configurationFile", "log4j.properties");
    //private final static Logger logger = LogManager.getLogger("profiling");

    public static final String CLOUD_FUNCTION_NAME = "de.uniba.dsg.serverless.functions.mixed.Mixed";

    public static void main(String[] args) {
        System.out.println("handling request");
        try {
            RequestHandlerExecutor loader = new RequestHandlerExecutor(CLOUD_FUNCTION_NAME);
            loader.invokeHandleRequest("");
        } catch (ProfilingException e) {
            //logger.fatal(e.getMessage());
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println(e.getCause().getMessage());
                //logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
            e.printStackTrace();
        }
    }


}
