package de.uniba.dsg.serverless.profiling;

public class Main {
    public static void main(String[] args) {
        System.out.println(args.length);

        ProfilingClassLoader loader = new ProfilingClassLoader();
        loader.invokeHandleRequest("de.uniba.dsg.serverless.functions.Fibonacci");
    }
}
