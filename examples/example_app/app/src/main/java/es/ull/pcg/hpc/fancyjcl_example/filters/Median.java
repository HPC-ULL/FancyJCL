package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Median extends Filter {

    final int RADIUS = 2;
    final int medianPosition = ((RADIUS * 2 + 1) * (RADIUS * 2 + 1)) / 2;

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                const uchar RADIUS =\040""" + RADIUS + """
                ;
                const uchar medianPosition =\040""" + medianPosition + """
                    ;
                    char accum[256] = {0};
                    int c = (int) d0 % 4;
                    int j = ((int) d0 / 4) % w;
                    int i = ((int) d0 / 4) / w;
                    if (c == 3) {
                        output[d0] = input[d0];
                    } else {
                        for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                            int i2 = clamp(i + ri, 0, h - 1);
                            for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                                int j2 = clamp(j + rj, 0, w - 1);
                                uchar pixel = input[(i2 * w + j2) * 4 + c];
                                accum[pixel] += 1;
                            }
                        }
                        uchar count = 0;
                        int accumIdx = 0;
                        while (count <= medianPosition) {
                            count += accum[accumIdx];
                            accumIdx += 1;
                        }
                        output[d0] = accumIdx;
                    }
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{4}));
        jclStages.add(stage);
    }

    @Override
    public void runJavaOnce(byte[] input, byte[] output, int w, int h) {
        short[] accum = new short[256];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    int position = (i * w + j) * 4 + c;
                    if (c == 3) {
                        output[position] = input[position];
                    } else {
                        Arrays.fill(accum, (byte) 0);
                        for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                            for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                                int i2 = Math.min(Math.max(i + ri, 0), h - 1);
                                int j2 = Math.min(Math.max(j + rj, 0), w - 1);
                                int pixel = input[(i2 * w + j2) * 4 + c] & 0xff;
                                accum[pixel] += 1;
                            }
                        }
                        int accumIdx = 0;
                        int count = 0;
                        while (count <= medianPosition) {
                            count += accum[accumIdx];
                            accumIdx += 1;
                        }
                        output[position] = (byte) accumIdx;
                    }
                }
            }
        }
    }
}
