package de.uniba.dsg.serverless.functions.mixed;

public class CPULoad implements Runnable {

    private final double from;
    private final double to;
    private final long time;


    /**
     * CPULoad which runs for a defined amount of time.
     * <p>
     * Example: <code>CPULoad(0.5,1.0,10_000)</code> will create a load which goes from 50% CPU usage to 100% linearly in 10 seconds.
     *
     * @param from start load as a percentage
     * @param to   end load as a percentage
     * @param time total running time
     */
    public CPULoad(double from, double to, long time) {
        this.from = Math.max(0., Math.min(from, 1.));
        this.to = Math.max(0., Math.min(to, 1.));
        this.time = time;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + time;

        try {
            while (System.currentTimeMillis() < endTime) {
                if (System.currentTimeMillis() % 100 == 0) {
                    double progress = (System.currentTimeMillis() - startTime) / (1. * (endTime - startTime));
                    double load = from + (to - from) * progress;
                    Thread.sleep((long) Math.floor((1 - load) * 100));
                }
            }
        } catch (InterruptedException e) {
            System.err.println("CPULoad interrupted, terminating...");
        }
    }
}