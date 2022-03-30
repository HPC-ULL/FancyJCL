package es.ull.pcg.hpc.fancierfrontend_testapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import es.ull.pcg.hpc.fancierfrontend.Test;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Timber.plant(new LinkingTree());
        }
        Timber.d("Starting app");
        setContentView(R.layout.activity_main);
        Test.doTest(getCacheDir().getAbsolutePath());
    }
}