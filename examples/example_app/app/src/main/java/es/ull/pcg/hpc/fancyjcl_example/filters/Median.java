package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Median extends Filter {

    final int RADIUS = 7;

    @Override
    public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    const char RADIUS = 7;
                    const unsigned char medianPosition = ((RADIUS * 2 + 1) * (RADIUS * 2 + 1)) / 2;
                    unsigned char accum[256] = {0};
                    int c = d0 % 4;
                    int j = (d0 / 4) % w;
                    int i = (d0 / 4) / w;
                    if (c == 3) {
                        output[d0] = input[d0];
                    } else {
                        for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                            for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                                int i2 = clamp(i + ri, 0, h - 1);
                                int j2 = clamp(j + rj, 0, w - 1);
                                int pixel = input[(i2 * w + j2) * 4 + c] & 0xff;
                                accum[pixel] += 1;
                            }
                        }
                        unsigned char accumIdx = 0;
                        unsigned char count = 0;
                        while (count <= medianPosition) {
                            count += accum[accumIdx];
                            accumIdx += 1;
                        }
                        output[d0] = accumIdx;
                    }
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{4}));
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
        short[] accum = new short[256];
        int medianPosition = ((RADIUS * 2 + 1) * (RADIUS * 2 + 1)) / 2;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    int position = (i * w + j) * 4 + c;
                    if (c == 3) {
                        output.put(position, input.get(position));
                    } else {
                        Arrays.fill(accum, (byte) 0);
                        for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                            for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                                int i2 = Math.min(Math.max(i + ri, 0), h - 1);
                                int j2 = Math.min(Math.max(j + rj, 0), w - 1);
                                int pixel = input.get((i2 * w + j2) * 4 + c) & 0xff;
                                accum[pixel] += 1;
                            }
                        }
                        int accumIdx = 0;
                        int count = 0;
                        while (count <= medianPosition) {
                            count += accum[accumIdx];
                            accumIdx += 1;
                        }
                        output.put(position, (byte) accumIdx);
                    }
                }
            }
        }
    }
}
