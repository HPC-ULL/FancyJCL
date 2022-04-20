package es.ull.pcg.hpc.fancyjcl_playground;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example1_Basic;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example2_InPlace;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example3_Bitmap;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example5_MultipleStages;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example6_Buffers;
import es.ull.pcg.hpc.fancyjcl_playground.examples.Example7_Benchmark;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static ImageView imageViewBefore;
    private static ImageView imageViewAfter;
    private static MainActivity instance;

    public static void setImgBefore(Bitmap img) {
        instance.runOnUiThread(() -> MainActivity.imageViewBefore.setImageBitmap(img));
    }

    public static void setImgAfter(Bitmap img) {
        instance.runOnUiThread(() -> MainActivity.imageViewAfter.setImageBitmap(img));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Timber.plant(new LinkingTree());
        }
        Context ctx = getApplicationContext();
        instance = this;
        setContentView(R.layout.activity_main);
        imageViewBefore = findViewById(R.id.imgBefore);
        imageViewAfter = findViewById(R.id.imgAfter);
        Executors.newSingleThreadExecutor().execute(() -> {
            FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
            Example1_Basic.run();
            Example2_InPlace.run();
            Example3_Bitmap.run(ctx);
            Example5_MultipleStages.run();
            Example6_Buffers.run();
            Example7_Benchmark.run();
        });
        FancyJCLManager.release();
    }
}