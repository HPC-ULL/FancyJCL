package es.ull.pcg.hpc.fancierfrontend;

import es.ull.pcg.hpc.fancier.Fancier;
import timber.log.Timber;

public class Test {
    private native static int test();
    public static void doTest(String basePath) {
        Timber.d("test");
//        System.loadLibrary("fancier");
        System.loadLibrary("fancierfrontend");
        Fancier.init(basePath);
        test();
    }
}
