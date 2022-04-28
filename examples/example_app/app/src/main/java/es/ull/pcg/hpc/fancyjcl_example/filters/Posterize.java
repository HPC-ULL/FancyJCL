package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Posterize extends Filter {

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    int offset = (int) d0 * 4;
                    float R = input[offset + 0] / 255.0f;
                    float G = input[offset + 1] / 255.0f;
                    float B = input[offset + 2] / 255.0f;
                    float A = input[offset + 3];

                    float intensity = R * 0.299f + G * 0.587f + B * 0.114f;
                    intensity = clamp(intensity, 0.0f, 255.0f);
                    
                    if (intensity <= 0.2f) {
                        output[offset + 0] = 255;
                        output[offset + 1] = 0;
                        output[offset + 2] = 0;
                    } else if(intensity <= 0.4) {
                        output[offset + 0] = 0;
                        output[offset + 1] = 255;
                        output[offset + 2] = 0;
                    } else if(intensity <= 0.8) {
                        output[offset + 0] = 0;
                        output[offset + 1] = 0;
                        output[offset + 2] = 255;
                    } else { // YELLOW
                        output[offset + 0] = 0;
                        output[offset + 1] = 255;
                        output[offset + 2] = 255;
                    }
                    output[offset + 3] = A;
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h}, new long[]{1024}));
        jclStages.add(stage);
    }

    @Override
    public void runJavaOnce(byte[] input, byte[] output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int offset = (i * w + j) * 4;
                float R = (input[offset + 0] & 0xff) / 255.0f;
                float G = (input[offset + 1] & 0xff) / 255.0f;
                float B = (input[offset + 2] & 0xff) / 255.0f;
                float A = input[offset + 3] & 0xff;

                float intensity = R * 0.299f + G * 0.587f + B * 0.114f;
                intensity = Math.max(0, intensity);
                intensity = Math.min(255, intensity);

                if (intensity <= 0.2f) { // RED
                    output[offset + 0] = (byte) 255;
                    output[offset + 1] = (byte) 0;
                    output[offset + 2] = (byte) 0;
                } else if (intensity <= 0.4) { // GREEN
                    output[offset + 0] = (byte) 0;
                    output[offset + 1] = (byte) 255;
                    output[offset + 2] = (byte) 0;
                } else if (intensity <= 0.8) { // BLUE
                    output[offset + 0] = (byte) 0;
                    output[offset + 1] = (byte) 0;
                    output[offset + 2] = (byte) 255;
                } else { // YELLOW
                    output[offset + 0] = (byte) 0;
                    output[offset + 1] = (byte) 255;
                    output[offset + 2] = (byte) 255;
                }
                output[offset + 3] = (byte) A;
            }
        }
    }
}
