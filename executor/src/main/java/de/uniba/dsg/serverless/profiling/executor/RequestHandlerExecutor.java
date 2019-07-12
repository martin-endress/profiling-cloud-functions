package de.uniba.dsg.serverless.profiling.executor;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class RequestHandlerExecutor {

    private final RequestHandler handler;

    /**
     * @param className
     */
    public RequestHandlerExecutor(String className) throws ProfilingException {
        this.handler = getRequestHandler(className);
    }

    /**
     * Parse and invoke the given RequestHandler with the parameters.
     *
     * @param param
     * @throws ProfilingException
     */
    public void invokeHandleRequest(String param) throws ProfilingException {
        try {
            System.out.println(handler.handleRequest(param, new ContextMock()));
        } catch (RuntimeException e) {
            throw new ProfilingException("RuntimeException handleRequest", e);
        }
    }

    private RequestHandler getRequestHandler(String classBinName) throws ProfilingException {
        try {
            Class<?> loadedMyClass = this.getClass().getClassLoader().loadClass(classBinName);
            Constructor<?> constructor = loadedMyClass.getConstructor();
            Object myClassObject = constructor.newInstance();
            if (myClassObject instanceof RequestHandler) {
                return (RequestHandler) myClassObject;
            } else {
                throw new ProfilingException(String.format("The class %s is not a RequestHandler", classBinName));
            }
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new ProfilingException("Could not create RequestHandler.", e);
        } catch (ClassNotFoundException e) {
            throw new ProfilingException(String.format("Class %s was not found.", classBinName), e);
        }
    }
}