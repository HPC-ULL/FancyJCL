package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Contrast extends Filter {

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    const float enhancement = 1.41421f;
                    int c = (int) d0 % 4;
                    if (c == 3) {
                        output[d0] = input[d0];
                    } else {
                        float pixel = ((float)(input[d0]) * enhancement) + 127.f * (1 - enhancement);
                        pixel = clamp(pixel, 0.0f, 255.0f);
                        output[d0] = pixel;
                    }
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{1024}));
        jclStages.add(stage);
    }

    @Override
    public void runJavaOnce(byte[] input, byte[] output, int w, int h) {
        final float enhancement = 1.41421f;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    int position = (i * w + j) * 4 + c;
                    if (c == 3) {
                        output[position] = input[position];
                    } else {
                        float pixel =
                                (input[position] & 0xff) * enhancement + 127.0f * (1 - enhancement);
                        pixel = Math.max(pixel, 0);
                        pixel = Math.min(pixel, 255.0f);
                        output[position] = (byte) pixel;
                    }
                }
            }
        }
    }
}
