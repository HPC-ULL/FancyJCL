package es.ull.pcg.hpc.fancyjcl_playground.examples;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.Benchmark;
import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import timber.log.Timber;

public class Example7_Benchmark {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run() {
        int size = 1080 * 1920;
        float kConstant = -2;

        // Have the data in java
        byte[] input = new byte[size];
        byte[] output = new byte[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }
        try {
            // Initialization
            Stage stage = new Stage();
            stage.setKernelSource("""
                    output[d0] = input[d0] * kConstant;
                            """);
            stage.setInputs(Map.of("input", input, "kConstant", kConstant));
            stage.setOutputs(Map.of("output", output));
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1024}));

            // Show information
            stage.printSummary();

            // Run
            float milliseconds = Benchmark.perform(() -> {
                stage.run();
            }, () -> {
                stage.syncOutputsToCPU();
            }, 10000);

            stage.runSync();

            String elapsedStr = String.format("%.2f", milliseconds);
            Timber.d("Computation time is %s milliseconds.", elapsedStr);

            Timber.d("Execution finished");
        } catch (Exception e) {
            Timber.e(e);
        }
        FancyJCLManager.clear();
    }
}
