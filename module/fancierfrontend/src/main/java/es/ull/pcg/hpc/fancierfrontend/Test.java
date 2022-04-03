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
            output[i] = (byte) i;
        }
        Stage stage = new Stage("test_stage");
        try {
            stage.setKernelSource("""
    output[d0] = output[d0] * kConstant;
            """);
            // Initialization
            Map<String, Object> inputs = Map.of("output", output, "kConstant", kConstant);
            Map<String, Object> outputs = Map.of("output", output);
            stage.setInputs(inputs);
            stage.setOutputs(outputs);
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));
            stage.printSummary();
            stage.prepare();
            // Run
            stage.syncInputsToGPU();
            stage.run();
            stage.waitUntilExecutionEnds();
            stage.syncOutputsToCPU();

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
