package es.ull.pcg.hpc.fancierfrontend;

import es.ull.pcg.hpc.fancier.Fancier;

public class FancierManager {
    private static boolean initialized = false;
    public static int kernelCount = 0;
    public static void initialize(String basePath) {
        if (! initialized) {
            System.loadLibrary("fancierfrontend");
            Fancier.init(basePath);
            initialized = true;
        }
    }

}
