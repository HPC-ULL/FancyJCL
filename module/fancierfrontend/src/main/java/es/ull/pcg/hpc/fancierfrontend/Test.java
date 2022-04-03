package es.ull.pcg.hpc.fancierfrontend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancier.Fancier;
import timber.log.Timber;

public class Test {

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void doTest(String basePath) {
        Timber.d("test");
        System.loadLibrary("fancierfrontend");
        Fancier.init(basePath);

        int size = 25;
        float kConstant = -2;

        // Have the data in java
        byte[] input = new byte[size];
        byte[] output = new byte[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }
        try {
            // Initialization
            Stage stage = new Stage("test_stage");
            stage.setKernelSource("""
    output[d0] = input[d0] * kConstant;
            """);
            stage.setInputs(Map.of("input", input, "kConstant", kConstant));
            stage.setOutputs(Map.of("output", output));
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));

            // Show information
            stage.printSummary();

            // Run
            stage.runSync();

            Timber.d("Execution finished");
            output = (byte[]) stage.getParameter("output");

            // Check the results
            for (int i = 0; i < output.length; i++) {
                Timber.d("[%d]=%d", i, output[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
