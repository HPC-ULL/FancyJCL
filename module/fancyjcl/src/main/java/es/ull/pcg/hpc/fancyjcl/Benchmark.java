package es.ull.pcg.hpc.fancyjcl;

import timber.log.Timber;


/**
 * Small utility to perform benchmarks.
 */
public class Benchmark {
    /**
     * Performs a benchmark of an asynchronous portion of code and a synchronization code. This
     * allows for enqueuing {@code iterations} calls to the code to benchmark and perform a single
     * synchronization call.
     *
     * @param process         Code to benchmark.
     * @param synchronization Code to synchronize the `process`
     * @param iterations      Number of iterations to perform
     * @return Time in milliseconds
     * @throws Exception Any failed execution will throw an exception
     */
    public static float perform(Process process, Process synchronization, int iterations)
            throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            process.run();
        }
        synchronization.run();
        long end = System.nanoTime();
        return (end - start) / (float) iterations / 1e6f;
    }

    /**
     * Interface that can be used to pass a lambda to the benchmark.
     */
    public interface Process {
        void run() throws Exception;
    }
}
