package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Convolution3x3 extends Filter {
    final float[] k = {0, -1, 0, -1, 5, -1, 0, -1, 0};

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage conv3x3 = new Stage();
        conv3x3.setInputs(Map.of("input", input, "w", w, "h", h));
        conv3x3.setOutputs(Map.of("output", output));
        conv3x3.setKernelSource("""
                const float k[9] = {0, -1, 0, -1, 5, -1, 0, -1, 0};
                int c = (int) d0 % 4;
                int j = ((int) d0 / 4) % w;
                int i = ((int) d0 / 4) / w;
                if (c == 3) {
                    output[d0] = input[d0] & 0xff;
                } else {
                    float pixel = 0.0f;
                    
                    pixel += k[0] * input[(max(i - 1, 0) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[1] * input[(max(i - 1, 0) * w + j) * 4 + c];
                    pixel += k[2] * input[(max(i - 1, 0) * w + min(j + 1, w - 1)) * 4 + c];
                    
                    pixel += k[3] * input[(i * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[4] * input[(i * w + j) * 4 + c];
                    pixel += k[5] * input[(i * w + min(j + 1, w - 1)) * 4 + c];
                    
                    pixel += k[6] * input[(min(i + 1, h - 1) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[7] * input[(min(i + 1, h - 1) * w + j) * 4 + c];
                    pixel += k[8] * input[(min(i + 1, h - 1) * w + min(j + 1, w - 1)) * 4 + c];
                                    
                    
                    output[d0] = clamp(pixel, 0.0f, 255.0f);
                }
                """);
        conv3x3.setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{1024}));
        jclStages.add(conv3x3);
    }

    @Override
    public void runJavaOnce(byte[] input, byte[] output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    int position = (i * w + j) * 4 + c;
                    if (c == 3) {
                        output[position] = input[position];
                    } else {
                        float pixel = 0.0f;

                        pixel += k[0] * (input[(Math.max(i - 1, 0) * w + Math.max(j - 1, 0)) * 4 + c] & 0xff);
                        pixel += k[1] * (input[(Math.max(i - 1, 0) * w + j) * 4 + c] & 0xff);
                        pixel += k[2] * (input[(Math.max(i - 1, 0) * w + Math.min(j + 1, w - 1)) * 4 + c] & 0xff);

                        pixel += k[3] * (input[(i * w + Math.max(j - 1, 0)) * 4 + c] & 0xff);
                        pixel += k[4] * (input[(i * w + j) * 4 + c] & 0xff);
                        pixel += k[5] * (input[(i * w + Math.min(j + 1, w - 1)) * 4 + c] & 0xff);

                        pixel += k[6] * (input[(Math.min(i + 1, h - 1) * w + Math.max(j - 1, 0)) * 4 + c] & 0xff);
                        pixel += k[7] * (input[(Math.min(i + 1, h - 1) * w + j) * 4 + c] & 0xff);
                        pixel += k[8] * (input[(Math.min(i + 1, h - 1) * w + Math.min(j + 1, w - 1)) * 4 + c] & 0xff);

                        pixel = Math.max(0, pixel);
                        pixel = Math.min(255, pixel);
                        output[position] = (byte) pixel;
                    }
                }
            }
        }
    }
}
