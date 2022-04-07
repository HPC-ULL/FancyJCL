package es.ull.pcg.hpc.fancyjcl_playground.examples;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import timber.log.Timber;

public class Example6_Buffer {

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run(Context ctx) {
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());

        int size = 25;
        float kConstant = -2;

        // Have the data in java
        ByteBuffer input = ByteBuffer.allocateDirect(size);
        ByteBuffer output = ByteBuffer.allocateDirect(size);

        for (int i = 0; i < input.capacity(); i++) {
            input.put(i, (byte) i);
        }
        try {
            // Initialization
            Stage stage = new Stage();
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

            // Check the results
            for (int i = 0; i < output.capacity(); i++) {
                Timber.d("[%d]=%d", i, output.get(i));
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }
}
