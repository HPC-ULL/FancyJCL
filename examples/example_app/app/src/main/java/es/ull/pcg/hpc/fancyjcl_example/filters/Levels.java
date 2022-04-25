package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Levels extends Filter {

    final float satMatrix00 = 1.3505f;
    final float satMatrix01 = -0.2935f;
    final float satMatrix02 = -0.057f;
    final float satMatrix10 = -0.1495f;
    final float satMatrix11 = 1.2065f;
    final float satMatrix12 = -0.057f;
    final float satMatrix20 = -0.1495f;
    final float satMatrix21 = -0.2935f;
    final float satMatrix22 = 1.443f;

    final float LEVEL_BLACK_START = 0.0f;
    final float LEVEL_BLACK_END = 40.0f;
    final float LEVEL_WHITE_START = 200.0f;
    final float LEVEL_WHITE_END = 255.0f;


    @Override
    public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    const float satMatrix00 = 1.3505f;
                    const float satMatrix01 = -0.2935f;
                    const float satMatrix02 = -0.057f;
                    const float satMatrix10 = -0.1495f;
                    const float satMatrix11 = 1.2065f;
                    const float satMatrix12 = -0.057f;
                    const float satMatrix20 = -0.1495f;
                    const float satMatrix21 = -0.2935f;
                    const float satMatrix22 = 1.443f;

                    const float LEVEL_BLACK_START = 0.0f;
                    const float LEVEL_BLACK_END = 40.0f;
                    const float LEVEL_WHITE_START = 200.0f;
                    const float LEVEL_WHITE_END = 255.0f;
                    
                    float R = input[d0 * 4 + 0] & 0xff;
                    float G = input[d0 * 4 + 1] & 0xff;
                    float B = input[d0 * 4 + 2] & 0xff;
                    float A = input[d0 * 4 + 3] & 0xff;
                    
                    // Multiply by matrix
                    float outputR = R * satMatrix00 + G * satMatrix01 + B * satMatrix02;
                    float outputG = R * satMatrix10 + G * satMatrix11 + B * satMatrix12;
                    float outputB = R * satMatrix20 + G * satMatrix21 + B * satMatrix22;
                    
                    // Clamp it
                    outputR = clamp(outputR, 0.0f, 255.0f);
                    outputG = clamp(outputG, 0.0f, 255.0f);
                    outputB = clamp(outputB, 0.0f, 255.0f);

                    outputR = (outputR - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);
                    outputG = (outputG - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);
                    outputB = (outputB - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);

                    outputR = outputR * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;
                    outputG = outputG * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;
                    outputB = outputB * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;

                    output[d0 * 4 + 0] =  clamp(outputR, 0.0f, 255.0f);
                    output[d0 * 4 + 1] =  clamp(outputG, 0.0f, 255.0f);
                    output[d0 * 4 + 2] =  clamp(outputB, 0.0f, 255.0f);
                    output[d0 * 4 + 3] =  A;
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h}, new long[]{1024}));
        stage.printSummary();
        // Run
        stage.runSync();
        FancyJCLManager.clear();
    }

    @Override
    public void benchmarkJava() {

    }

    @Override
    public void benchmarkFancyJCL() {

    }

    @Override
    public void runJavaOnce(ByteBuffer input, ByteBuffer output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int offset = (i * w + j) * 4;
                float R = input.get(offset + 0) & 0xff;
                float G = input.get(offset + 1) & 0xff;
                float B = input.get(offset + 2) & 0xff;
                float A = input.get(offset + 3) & 0xff;
                // Multiply by matrix
                float outputR = R * satMatrix00 + G * satMatrix01 + B * satMatrix02;
                float outputG = R * satMatrix10 + G * satMatrix11 + B * satMatrix12;
                float outputB = R * satMatrix20 + G * satMatrix21 + B * satMatrix22;
                // Clamp it
                outputR = Math.max(0, outputR);
                outputG = Math.max(0, outputG);
                outputB = Math.max(0, outputB);
                outputR = Math.min(255, outputR);
                outputG = Math.min(255, outputG);
                outputB = Math.min(255, outputB);

                outputR = (outputR - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);
                outputG = (outputG - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);
                outputB = (outputB - LEVEL_BLACK_START) / (LEVEL_WHITE_START - LEVEL_BLACK_START);

                outputR = outputR * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;
                outputG = outputG * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;
                outputB = outputB * (LEVEL_WHITE_END - LEVEL_BLACK_END) + LEVEL_BLACK_END;

                output.put(offset + 0, (byte) Math.max(Math.min(outputR, 255.0f), 0.0f));
                output.put(offset + 1, (byte) Math.max(Math.min(outputG, 255.0f), 0.0f));
                output.put(offset + 2, (byte) Math.max(Math.min(outputB, 255.0f), 0.0f));
                output.put(offset + 3, (byte) A);

            }
        }
    }
}
