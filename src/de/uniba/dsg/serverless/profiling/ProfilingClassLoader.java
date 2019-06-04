package de.uniba.dsg.serverless.profiling;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import de.uniba.dsg.serverless.profiling.mock.ContextMock;

public class ProfilingClassLoader extends ClassLoader {

	public void invokeHandleRequest(String classBinName) {
		try {
			ClassLoader classLoader = this.getClass().getClassLoader();

			Class<RequestHandler> loadedMyClass = (Class<RequestHandler>) classLoader.loadClass(classBinName);

			Constructor<RequestHandler> constructor = loadedMyClass.getConstructor();
			Object myClassObject = constructor.newInstance();
			RequestHandler handler = (RequestHandler) myClassObject;

			Map<String, String> map = new HashMap<>();
			map.put("n", "5");
			handler.handleRequest(map, new ContextMock());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}