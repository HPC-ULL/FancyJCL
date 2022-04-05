package es.ull.pcg.hpc.fancierfrontend_testapp.examples;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancierfrontend.FancierManager;
import es.ull.pcg.hpc.fancierfrontend.RunConfiguration;
import es.ull.pcg.hpc.fancierfrontend.Stage;
import es.ull.pcg.hpc.fancierfrontend_testapp.MainActivity;
import es.ull.pcg.hpc.fancierfrontend_testapp.R;
import timber.log.Timber;

public class Example3_Bitmap {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run(Context ctx) {
        FancierManager.initialize(ctx.getCacheDir().getAbsolutePath());
        Bitmap img = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.lenna_512);
        MainActivity.setImgBefore(img);
        try {
            Stage stage = new Stage("test_stage");
            stage.setKernelSource("""
                    img[d0] = img[d0] + (uchar4)(100, 0, 0, 0);
                            """);
            stage.setInputs(Map.of("img", img));
            stage.setOutputs(Map.of("img", img));
            stage.setRunConfiguration(new RunConfiguration(new long[]{512 * 512}, new long[]{1}));

            // Show information
            stage.printSummary();

            // Run
            stage.runSync();

            Timber.d("Execution finished");
            Bitmap img2 = (Bitmap) stage.getParameter("img");
            MainActivity.setImgAfter(img2);
        } catch (Exception e) {
            Timber.e(e);
        }

    }
}
