package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Bilateral extends Filter {
    final int RADIUS = 10;
    final float PRESERVATION = 0.5f;

    @Override
    public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    const int RADIUS = 10;
                    const float PRESERVATION = 0.5f;
                    int i = d0 / w;
                    int j = d0 % w;
                    float centerPixelR = ((float)(input[d0 * 4 + 0] & 0xff)) / 255.0f;
                    float centerPixelG = ((float)(input[d0 * 4 + 1] & 0xff)) / 255.0f;
                    float centerPixelB = ((float)(input[d0 * 4 + 2] & 0xff)) / 255.0f;
                    float sumR = 0.0f;
                    float sumG = 0.0f;
                    float sumB = 0.0f;
                    float totalWeight = 0.0f;
                    for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                        for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                            int i2 = clamp(i + ri, 0, h - 1);
                            int j2 = clamp(j + rj, 0, w - 1);
                            float pixelR = ((float)(input[(i2 * w + j2) * 4 + 0] & 0xff)) / 255.0f;
                            float pixelG = ((float)(input[(i2 * w + j2) * 4 + 1] & 0xff)) / 255.0f;
                            float pixelB = ((float)(input[(i2 * w + j2) * 4 + 2] & 0xff)) / 255.0f;
                            float diffR = centerPixelR - pixelR;
                            float diffG = centerPixelG - pixelG;
                            float diffB = centerPixelB - pixelB;
                            diffR = diffR * diffR;
                            diffG = diffG * diffG;
                            diffB = diffB * diffB;
                            float diffMap = exp(-(diffR + diffG + diffB) * PRESERVATION * 100.f);
                            float gaussianWeight = exp(-0.5f * ((ri * ri) + (rj * rj)) / (float) RADIUS);
                            float weight = diffMap * gaussianWeight;
                            sumR += pixelR * weight;
                            sumG += pixelG * weight;
                            sumB += pixelB * weight;
                            totalWeight += weight;
                        }
                    }
                    output[d0 * 4 + 0] = clamp((sumR / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[d0 * 4 + 1] = clamp((sumG / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[d0 * 4 + 2] = clamp((sumB / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[d0 * 4 + 3] = input[d0 * 4 + 3];
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h}, new long[]{1024}));
        stage.printSummary();
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
                float centerPixelR = ((float) (input.get((i * w + j) * 4 + 0) & 0xff)) / 255.0f;
                float centerPixelG = ((float) (input.get((i * w + j) * 4 + 1) & 0xff)) / 255.0f;
                float centerPixelB = ((float) (input.get((i * w + j) * 4 + 2) & 0xff)) / 255.0f;
                float sumR = 0.0f;
                float sumG = 0.0f;
                float sumB = 0.0f;
                float totalWeight = 0.0f;
                for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                    for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                        int i2 = Math.min(Math.max(i + ri, 0), h - 1);
                        int j2 = Math.min(Math.max(j + rj, 0), w - 1);
                        float pixelR = ((float) (input.get((i2 * w + j2) * 4 + 0) & 0xff)) / 255.0f;
                        float pixelG = ((float) (input.get((i2 * w + j2) * 4 + 1) & 0xff)) / 255.0f;
                        float pixelB = ((float) (input.get((i2 * w + j2) * 4 + 2) & 0xff)) / 255.0f;
                        float diffR = centerPixelR - pixelR;
                        float diffG = centerPixelG - pixelG;
                        float diffB = centerPixelB - pixelB;
                        diffR = diffR * diffR;
                        diffG = diffG * diffG;
                        diffB = diffB * diffB;
                        float diffMap =
                                (float) Math.exp(-(diffR + diffG + diffB) * PRESERVATION * 100.f);
                        float gaussianWeight =
                                (float) Math.exp(-0.5f * ((ri * ri) + (rj * rj)) / (float) RADIUS);
                        float weight = diffMap * gaussianWeight;
                        sumR += pixelR * weight;
                        sumG += pixelG * weight;
                        sumB += pixelB * weight;
                        totalWeight += weight;
                    }
                }
                output.put((i * w + j) * 4 + 0,
                        (byte) Math.max(Math.min((sumR * 255.0f / totalWeight), 255.0f), 0.0f));
                output.put((i * w + j) * 4 + 1,
                        (byte) Math.max(Math.min((sumG * 255.0f / totalWeight), 255.0f), 0.0f));
                output.put((i * w + j) * 4 + 2,
                        (byte) Math.max(Math.min((sumB * 255.0f / totalWeight), 255.0f), 0.0f));
                output.put((i * w + j) * 4 + 3, input.get((i * w + j) * 4 + 3));
            }
        }
    }
}
