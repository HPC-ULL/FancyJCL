package es.ull.pcg.hpc.fancyjcl_playground.examples;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_playground.MainActivity;
import es.ull.pcg.hpc.fancyjcl_playground.R;
import timber.log.Timber;

public class Example3_Bitmap {
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void run(Context ctx) {
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        Bitmap imgIn = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.lenna_512);
        Bitmap imgOut = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        MainActivity.setImgBefore(imgIn);
        try {
            Stage stage = new Stage();
            stage.setKernelSource("""
                    img_out[d0] = img_in[d0] + (uchar4)(100, 0, 0, 0);
                            """);
            stage.setInputs(Map.of("img_in", imgIn));
            stage.setOutputs(Map.of("img_out", imgOut));
            stage.setRunConfiguration(new RunConfiguration(new long[]{512 * 512}, new long[]{1}));

            // Show information
            stage.printSummary();

            // Run
            stage.runSync();
            Timber.d("Execution finished");

            MainActivity.setImgAfter(imgOut);
        } catch (Exception e) {
            Timber.e(e);
        }
        FancyJCLManager.release();
    }
}
