package es.ull.pcg.hpc.fancierfrontend_testapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Executable;
import java.util.concurrent.Executors;

import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example1_Basic;
import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example2_InPlace;
import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example3_Bitmap;
import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example4_FancierTypes;
import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example5_MultipleStages;
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
//            Example1_Basic.run(ctx);
//            Example2_InPlace.run(ctx);
//            Example3_Bitmap.run(ctx);
//            Example4_FancierTypes.run(ctx);
            Example5_MultipleStages.run(ctx);
        });
    }
}