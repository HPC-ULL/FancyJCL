package es.ull.pcg.hpc.fancyjcl;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class Benchmark {
    public static void perform(Process process, Process synchronization, int iterations)
            throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            process.run();
        }
        synchronization.run();
        long end = System.nanoTime();
        float elapsed = (end - start) / (float) iterations / 1e6f;
        String elapsedStr = String.format("%.2f", elapsed);
        Timber.d("Elapsed time is %s milliseconds.",  elapsedStr);
    }

    public interface Process {
        void run() throws Exception;
    }
}
