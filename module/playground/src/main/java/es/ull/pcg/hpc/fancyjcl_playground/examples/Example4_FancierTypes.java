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

public class Example4_FancierTypes {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run() {
        int size = 10;
        FloatArray array = new FloatArray(size);
        for (int i = 0; i < size; i++) {
            array.set(i, (byte) i);
        }
        try {
            Stage stage = new Stage();
            stage.setKernelSource("""
                    array[d0] = array[d0] * array[d0];
                            """);
            stage.setInputs(Map.of("array", array));
            stage.setOutputs(Map.of("array", array));
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));

            // Show information
            stage.printSummary();

            // Run
            stage.runSync();
            Timber.d("Execution finished");

            // Check results

            for (int i = 0; i < size; i++) {
                Timber.d("array[%d]=%f", i, array.get(i));
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        FancyJCLManager.clear();
    }
}
