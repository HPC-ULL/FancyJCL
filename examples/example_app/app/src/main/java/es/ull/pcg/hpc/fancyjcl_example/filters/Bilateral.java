package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Bilateral extends Filter {
    final int RADIUS = 2;
    final float PRESERVATION = 0.5f;

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h) throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                const int RADIUS =\040""" + RADIUS + """
                ;
                const float PRESERVATION =\040""" + PRESERVATION + """
                    ;
                    int i = (int) d0 / w;
                    int j = (int) d0 % w;
                    int offset = (int) d0 * 4;
                    float centerPixelR = ((float)(input[offset + 0])) / 255.0f;
                    float centerPixelG = ((float)(input[offset + 1])) / 255.0f;
                    float centerPixelB = ((float)(input[offset + 2])) / 255.0f;
                    float sumR = 0.0f;
                    float sumG = 0.0f;
                    float sumB = 0.0f;
                    float totalWeight = 0.0f;
                    for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                        int i2 = clamp(i + ri, 0, h - 1);
                        for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                            int j2 = clamp(j + rj, 0, w - 1);
                            int offset = (i2 * w + j2) * 4;
                            float pixelR = (float)(input[offset + 0]) / 255.0f;
                            float pixelG = (float)(input[offset + 1]) / 255.0f;
                            float pixelB = (float)(input[offset + 2]) / 255.0f;
                            float diffR = centerPixelR - pixelR;
                            float diffG = centerPixelG - pixelG;
                            float diffB = centerPixelB - pixelB;
                            diffR = diffR * diffR;
                            diffG = diffG * diffG;
                            diffB = diffB * diffB;
                            float diffMap = native_exp(-(diffR + diffG + diffB) * PRESERVATION * 100.f);
                            float gaussianWeight = native_exp(-0.5f * ((ri * ri) + (rj * rj)) / (float) RADIUS);
                            float weight = diffMap * gaussianWeight;
                            sumR += pixelR * weight;
                            sumG += pixelG * weight;
                            sumB += pixelB * weight;
                            totalWeight += weight;
                        }
                    }
                    output[offset + 0] = clamp((sumR / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[offset + 1] = clamp((sumG / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[offset + 2] = clamp((sumB / totalWeight) * 255.0f, 0.0f, 255.0f);
                    output[offset + 3] = input[offset + 3];
                """);
        stage.setRunConfiguration(new RunConfiguration(new long[]{w * h}, new long[]{32}));
        jclStages.add(stage);
    }

    @Override
    public void runJavaOnce(byte[] input, byte[] output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                float centerPixelR = ((float) (input[(i * w + j) * 4 + 0] & 0xff)) / 255.0f;
                float centerPixelG = ((float) (input[(i * w + j) * 4 + 1] & 0xff)) / 255.0f;
                float centerPixelB = ((float) (input[(i * w + j) * 4 + 2] & 0xff)) / 255.0f;
                float sumR = 0.0f;
                float sumG = 0.0f;
                float sumB = 0.0f;
                float totalWeight = 0.0f;
                for (int ri = -RADIUS; ri <= RADIUS; ri++) {
                    for (int rj = -RADIUS; rj <= RADIUS; rj++) {
                        int i2 = Math.min(Math.max(i + ri, 0), h - 1);
                        int j2 = Math.min(Math.max(j + rj, 0), w - 1);
                        float pixelR = ((float) (input[(i2 * w + j2) * 4 + 0] & 0xff)) / 255.0f;
                        float pixelG = ((float) (input[(i2 * w + j2) * 4 + 1] & 0xff)) / 255.0f;
                        float pixelB = ((float) (input[(i2 * w + j2) * 4 + 2] & 0xff)) / 255.0f;
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
                output[(i * w + j) * 4 + 0] =
                        (byte) Math.max(Math.min((sumR * 255.0f / totalWeight), 255.0f), 0.0f);
                output[(i * w + j) * 4 + 1] =
                        (byte) Math.max(Math.min((sumG * 255.0f / totalWeight), 255.0f), 0.0f);
                output[(i * w + j) * 4 + 2] =
                        (byte) Math.max(Math.min((sumB * 255.0f / totalWeight), 255.0f), 0.0f);
                output[(i * w + j) * 4 + 3] = input[(i * w + j) * 4 + 3];
            }
        }
    }
}
