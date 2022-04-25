package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Posterize extends Filter {

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
                    float R = (input[d0 * 4 + 0] & 0xff) / 255.0f;
                    float G = (input[d0 * 4 + 1] & 0xff) / 255.0f;
                    float B = (input[d0 * 4 + 2] & 0xff) / 255.0f;
                    float A = input[d0 * 4 + 3] & 0xff;

                    float intensity = R * 0.299f + G * 0.587f + B * 0.114f;
                    intensity = clamp(intensity, 0.0f, 255.0f);
                    
                    if (intensity <= 0.2f) { // RED
                        output[d0 * 4 + 0] = 255;
                        output[d0 * 4 + 1] = 0;
                        output[d0 * 4 + 2] = 0;
                    } else if(intensity <= 0.4) { // GREEN
                        output[d0 * 4 + 0] = 0;
                        output[d0 * 4 + 1] = 255;
                        output[d0 * 4 + 2] = 0;
                    } else if(intensity <= 0.8) { // BLUE
                        output[d0 * 4 + 0] = 0;
                        output[d0 * 4 + 1] = 0;
                        output[d0 * 4 + 2] = 255;
                    } else { // YELLOW
                        output[d0 * 4 + 0] = 0;
                        output[d0 * 4 + 1] = 255;
                        output[d0 * 4 + 2] = 255;
                    }
                    output[d0 * 4 + 3] = A;
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
                float R = (input.get(offset + 0) & 0xff) / 255.0f;
                float G = (input.get(offset + 1) & 0xff) / 255.0f;
                float B = (input.get(offset + 2) & 0xff) / 255.0f;
                float A = input.get(offset + 3) & 0xff;

                float intensity = R * 0.299f + G * 0.587f + B * 0.114f;
                intensity = Math.max(0, intensity);
                intensity = Math.min(255, intensity);

                if (intensity <= 0.2f) { // RED
                    output.put(offset + 0, (byte) 255);
                    output.put(offset + 1, (byte) 0);
                    output.put(offset + 2, (byte) 0);
                } else if(intensity <= 0.4) { // GREEN
                    output.put(offset + 0, (byte) 0);
                    output.put(offset + 1, (byte) 255);
                    output.put(offset + 2, (byte) 0);
                } else if(intensity <= 0.8) { // BLUE
                    output.put(offset + 0, (byte) 0);
                    output.put(offset + 1, (byte) 0);
                    output.put(offset + 2, (byte) 255);
                } else { // YELLOW
                    output.put(offset + 0, (byte) 0);
                    output.put(offset + 1, (byte) 255);
                    output.put(offset + 2, (byte) 255);
                }
                output.put(offset + 3, (byte) A);
            }
        }
    }
}
