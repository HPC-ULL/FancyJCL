package es.ull.pcg.hpc.fancierfrontend_testapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import java.util.Map;

import es.ull.pcg.hpc.fancierfrontend.FancierManager;
import es.ull.pcg.hpc.fancierfrontend.RunConfiguration;
import es.ull.pcg.hpc.fancierfrontend.Stage;
import es.ull.pcg.hpc.fancierfrontend.Test;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Timber.plant(new LinkingTree());
        }
        Timber.d("Starting app");
        setContentView(R.layout.activity_main);
        AsyncTask.execute(() -> {
            FancierManager.initialize(getCacheDir().getAbsolutePath());
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
        });
    }
}