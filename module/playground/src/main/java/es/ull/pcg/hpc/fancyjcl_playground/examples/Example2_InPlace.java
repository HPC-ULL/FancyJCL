package es.ull.pcg.hpc.fancyjcl_playground.examples;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import timber.log.Timber;

public class Example2_InPlace {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run(Context ctx) {
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 25;
        float kConstant = -2;

        // Have the data in java
        byte[] data = new byte[size];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        try {
            // Initialization
            Stage stage = new Stage();
            stage.setKernelSource("""
                    data[d0] = data[d0] * kConstant;
                            """);
            stage.setInputs(Map.of("data", data, "kConstant", kConstant));
            stage.setOutputs(Map.of("data", data));
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));

            // Show information
            stage.printSummary();

            // Run
            stage.runSync();
            Timber.d("Execution finished");

            // Check the results
            for (int i = 0; i < data.length; i++) {
                Timber.d("[%d]=%d", i, data[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
