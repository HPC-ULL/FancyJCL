package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Convolution5x5 extends Filter {
    final float[] k = {0, 0, -1, 0, 0,
                       0, -1, -2, -1, 0,
                       -1, -2, 16, -2, -1,
                       0, -1, -2, -1, 0,
                       0, 0, -1, 0, 0};
;

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage conv3x3 = new Stage();
        conv3x3.setInputs(Map.of("input", input, "w", w, "h", h));
        conv3x3.setOutputs(Map.of("output", output));
        conv3x3.setKernelSource("""
                const float k[25] = {0, 0, -1, 0, 0, 0, -1, -2, -1, 0, -1, -2, 16, -2, -1, 0, -1, -2, -1, 0, 0, 0, -1, 0, 0};
                                        ;
                int c = (int) d0 % 4;
                int j = ((int) d0 / 4) % w;
                int i = ((int) d0 / 4) / w;
                if (c == 3) {
                    output[d0] = input[d0];
                } else {
                    float pixel = 0.0f;
                    
                    pixel += k[0] * input[(max(i - 2, 0) * w + max(j - 2, 0)) * 4 + c];
                    pixel += k[1] * input[(max(i - 2, 0) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[2] * input[(max(i - 2, 0) * w + j) * 4 + c];
                    pixel += k[3] * input[(max(i - 2, 0) * w + min(j + 1, w - 1)) * 4 + c];
                    pixel += k[4] * input[(max(i - 2, 0) * w + min(j + 2, w - 1)) * 4 + c];
                    
                    pixel += k[5] * input[(max(i - 1, 0) * w + max(j - 2, 0)) * 4 + c];
                    pixel += k[6] * input[(max(i - 1, 0) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[7] * input[(max(i - 1, 0) * w + j) * 4 + c];
                    pixel += k[8] * input[(max(i - 1, 0) * w + min(j + 1, w - 1)) * 4 + c];
                    pixel += k[9] * input[(max(i - 1, 0) * w + min(j + 2, w - 1)) * 4 + c];
                    
                    pixel += k[10] * input[(i * w + max(j - 2, 0)) * 4 + c];
                    pixel += k[11] * input[(i * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[12] * input[(i * w + j) * 4 + c];
                    pixel += k[13] * input[(i * w + min(j + 1, w - 1)) * 4 + c];
                    pixel += k[14] * input[(i * w + min(j + 2, w - 1)) * 4 + c];
                    
                    pixel += k[15] * input[(min(i + 1, h - 1) * w + max(j - 2, 0)) * 4 + c];
                    pixel += k[16] * input[(min(i + 1, h - 1) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[17] * input[(min(i + 1, h - 1) * w + j) * 4 + c];
                    pixel += k[18] * input[(min(i + 1, h - 1) * w + min(j + 1, w - 1)) * 4 + c];
                    pixel += k[19] * input[(min(i + 1, h - 1) * w + min(j + 2, w - 1)) * 4 + c];
                    
                    pixel += k[20] * input[(min(i + 2, h - 1) * w + max(j - 2, 0)) * 4 + c];
                    pixel += k[21] * input[(min(i + 2, h - 1) * w + max(j - 1, 0)) * 4 + c];
                    pixel += k[22] * input[(min(i + 2, h - 1) * w + j) * 4 + c];
                    pixel += k[23] * input[(min(i + 2, h - 1) * w + min(j + 1, w - 1)) * 4 + c];
                    pixel += k[24] * input[(min(i + 2, h - 1) * w + min(j + 2, w - 1)) * 4 + c];
                    
                    output[d0] = clamp(pixel, 0.0f, 255.0f);
                }
                """);
        conv3x3.setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{1024}));
        jclStages.add(conv3x3);
    }

    @Override
    public void runJavaOnce(ByteBuffer input, ByteBuffer output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    int position = (i * w + j) * 4 + c;
                    if (c == 3) {
                        output.put(position, input.get(position));
                    } else {
                        int j0 = Math.max(j - 2, 0);
                        int j1 = Math.max(j - 1, 0);
                        int j2 = Math.min(j + 1, w - 1);
                        int j3 = Math.min(j + 2, w - 1);

                        int i0 = Math.max(i - 2, 0);
                        int i1 = Math.max(i - 1, 0);
                        int i2 = Math.min(i + 1, h - 1);
                        int i3 = Math.min(i + 2, h - 1);

                        float pixel = 0.0f;

                        pixel += k[0] * (input.get((i0 * w + j0) * 4 + c) & 0xff);
                        pixel += k[1] * (input.get((i0 * w + j1) * 4 + c) & 0xff);
                        pixel += k[2] * (input.get((i0 * w + j) * 4 + c) & 0xff);
                        pixel += k[3] * (input.get((i0 * w + j2) * 4 + c) & 0xff);
                        pixel += k[4] * (input.get((i0 * w + j3) * 4 + c) & 0xff);

                        pixel += k[5] * (input.get((i1 * w + j0) * 4 + c) & 0xff);
                        pixel += k[6] * (input.get((i1 * w + j1) * 4 + c) & 0xff);
                        pixel += k[7] * (input.get((i1 * w + j) * 4 + c) & 0xff);
                        pixel += k[8] * (input.get((i1 * w + j2) * 4 + c) & 0xff);
                        pixel += k[9] * (input.get((i1 * w + j3) * 4 + c) & 0xff);

                        pixel += k[10] * (input.get((i * w + j0) * 4 + c) & 0xff);
                        pixel += k[11] * (input.get((i * w + j1) * 4 + c) & 0xff);
                        pixel += k[12] * (input.get(position) & 0xff);
                        pixel += k[13] * (input.get((i * w + j2) * 4 + c) & 0xff);
                        pixel += k[14] * (input.get((i * w + j3) * 4 + c) & 0xff);

                        pixel += k[15] * (input.get((i2 * w + j0) * 4 + c) & 0xff);
                        pixel += k[16] * (input.get((i2 * w + j1) * 4 + c) & 0xff);
                        pixel += k[17] * (input.get((i2 * w + j) * 4 + c) & 0xff);
                        pixel += k[18] * (input.get((i2 * w + j2) * 4 + c) & 0xff);
                        pixel += k[19] * (input.get((i2 * w + j3) * 4 + c) & 0xff);

                        pixel += k[20] * (input.get((i3 * w + j0) * 4 + c) & 0xff);
                        pixel += k[21] * (input.get((i3 * w + j1) * 4 + c) & 0xff);
                        pixel += k[22] * (input.get((i3 * w + j) * 4 + c) & 0xff);
                        pixel += k[23] * (input.get((i3 * w + j2) * 4 + c) & 0xff);
                        pixel += k[24] * (input.get((i3 * w + j3) * 4 + c) & 0xff);

                        pixel = Math.max(0,pixel);
                        pixel = Math.min(255,pixel);
                        output.put(position, (byte) pixel);
                    }
                }
            }
        }
    }
}
