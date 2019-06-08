package de.uniba.dsg.serverless.profiling.model;

public class ProfilingException extends Exception {

    public ProfilingException() {
        super();
    }

    public ProfilingException(String message) {
        super(message);
    }

    public ProfilingException(Throwable cause) {
        super(cause);
    }

    public ProfilingException(String message, Throwable cause) {
        super(message, cause);
    }
}
