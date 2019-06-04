package de.uniba.dsg.serverless.profiling;

public class Main {
	public static void main(String[] args) {
		ProfilingClassLoader loader = new ProfilingClassLoader();
		loader.invokeHandleRequest("de.uniba.dsg.serverless.profiling.example.Fibonacci");
	}
}
