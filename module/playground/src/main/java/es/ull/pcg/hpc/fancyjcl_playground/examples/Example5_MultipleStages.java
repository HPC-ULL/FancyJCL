package es.ull.pcg.hpc.fancyjcl_playground.examples;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancier.array.FloatArray;
import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import timber.log.Timber;

public class Example5_MultipleStages {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run(Context ctx) {
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 10;
        FloatArray array = new FloatArray(size);
        FloatArray aux = new FloatArray(size);
        FloatArray output = new FloatArray(size);
        for (int i = 0; i < size; i++) {
            array.set(i, (byte) i);
        }
        try {
            Stage power = new Stage();
            power.setKernelSource("""
                    aux[d0] = array[d0] * array[d0];
                            """);
            power.setInputs(Map.of("array", array));
            power.setOutputs(Map.of("aux", aux));
            power.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));
            // Show information
            power.printSummary();

            Stage threshold = new Stage();
            threshold.setKernelSource("""
                    output[d0] = (aux[d0] > 25.0f)? 1.0f : 0.0f;
                            """);
            threshold.setInputs(Map.of("aux", aux));
            threshold.setOutputs(Map.of("output", output));
            threshold.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));
            // Show information
            threshold.printSummary();
            FancyJCLManager.showDebugInfo();

            // Run
            power.syncInputsToGPU();
            power.run();
            threshold.run();
            threshold.syncOutputsToCPU();

            Timber.d("Execution finished");

            // Check results

            for (int i = 0; i < size; i++) {
                Timber.d("out[%d]=%f", i, output.get(i));
            }
        } catch (Exception e) {
            Timber.e(e);
        }

    }
}
