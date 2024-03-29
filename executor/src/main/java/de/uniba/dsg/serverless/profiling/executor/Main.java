package de.uniba.dsg.serverless.profiling.executor;


import de.uniba.dsg.serverless.profiling.load.Mixed;

public class Main {

    public static void main(String[] args) {
        try {
            new Mixed().simulateLoad();
        } catch (ProfilingException e) {
            System.err.println(e.getMessage());
            if (e.getCause() != null) {
                System.err.println(e.getCause().getMessage());
            }
        }
    }
}
