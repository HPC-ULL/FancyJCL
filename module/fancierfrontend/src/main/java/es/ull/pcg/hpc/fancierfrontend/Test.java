package es.ull.pcg.hpc.fancierfrontend;

import es.ull.pcg.hpc.fancier.Fancier;
import timber.log.Timber;

public class Test {
    private native static int test();

    public static void doTest(String basePath) {
        Timber.d("test");
        System.loadLibrary("fancierfrontend");
        Fancier.init(basePath);

        int size = 100;

        // Have the data in java
        byte[] input = new byte[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }
        byte[] output = new byte[size];
        Stage stage = new Stage("test_stage");
        try {
            stage.setKernelSource("""
    output[d0] = input[d0] * 2;
            """);
            // Initialization
            stage.setInputs(input);
            stage.setInputNames("input");
            stage.setOutputs(output);
            stage.setOutputNames("output");
            stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));
            stage.printSummary();
            stage.prepare();
            // Run
            stage.syncInputsToGPU();
            stage.run();
            stage.waitUntilExecutionEnds();
            stage.syncOutputsToCPU();

            Timber.d("Execution finished");
            output = (byte[]) stage.getOutput(0);


            // Check the results
//            for (int i = 0; i < output.length; i++) {
//                Timber.d("[%d]=%d", i, output[i]);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
