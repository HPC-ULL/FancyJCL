package es.ull.pcg.hpc.fancierfrontend_testapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import es.ull.pcg.hpc.fancierfrontend_testapp.examples.Example3_Bitmap;
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
        AsyncTask.execute(() -> {
//            Example1_Basic.run(ctx);
//            Example2_InPlace.run(ctx);
            Example3_Bitmap.run(ctx);
        });
    }
}