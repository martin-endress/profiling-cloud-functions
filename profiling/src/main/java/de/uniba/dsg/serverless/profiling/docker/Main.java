package de.uniba.dsg.serverless.profiling.docker;


import de.uniba.dsg.serverless.profiling.model.ProfilingException;

public class Main {

    //System.setProperty("log4j.configurationFile", "log4j.properties");
    //private final static Logger logger = LogManager.getLogger("profiling");

    public static void main(String[] args) {
        try {
            System.out.println("handling request");
            runMixed();
            //runFibonacci(args);
            //runMocky();
        } catch (ProfilingException e) {
            //logger.fatal(e.getMessage());
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println(e.getCause().getMessage());
                //logger.fatal(String.format("Caused By: %s", e.getCause().getMessage()));
            }
        }
    }

    private static void runMixed() throws ProfilingException {
        RequestHandlerExecutor loader = new RequestHandlerExecutor("de.uniba.dsg.serverless.functions.mixed.Mixed");
        loader.invokeHandleRequest("");
    }

    private static void runFibonacci(String[] args) throws ProfilingException {
        String n = "0";
        if (args.length > 0) {
            n = args[0];
        }
        RequestHandlerExecutor loader = new RequestHandlerExecutor("de.uniba.dsg.serverless.functions.fibonacci.Fibonacci");
        loader.invokeHandleRequest(n);
    }

    private static void runMocky() throws ProfilingException {
        RequestHandlerExecutor loader = new RequestHandlerExecutor("de.uniba.dsg.serverless.functions.mocky.Mocky");
        loader.invokeHandleRequest("");
    }
}
