package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class GaussianBlur extends Filter {
    final int BLUR_RADIUS = 5;
    final float[] kernel = {0.00571868f, 0.02112909f, 0.05837343f, 0.12061129f, 0.18640801f,
            0.21551899f, 0.18640801f, 0.12061129f, 0.05837343f, 0.02112909f,
            0.00571868f};

    private byte [] aux = null;

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        // Horizontal
        Stage horizontalGaussianStage = new Stage();
        ByteBuffer aux = ByteBuffer.allocateDirect(w * h * 4);
        horizontalGaussianStage.setInputs(Map.of("input", input, "w", w));
        horizontalGaussianStage.setOutputs(Map.of("aux", aux));
        horizontalGaussianStage.setKernelSource("""
                    const int BLUR_RADIUS = 5;
                    const float k[11] = {0.00571868f, 0.02112909f, 0.05837343f, 0.12061129f, 0.18640801f,
                0.21551899f, 0.18640801f, 0.12061129f, 0.05837343f, 0.02112909f, 0.00571868f};
                    int c = (int) d0 % 4;
                    int j = ((int) d0 / 4) % w;
                    int i = ((int) d0 / 4) / w;
                    float blurredPixel = 0.0f;
                    for (int r = -BLUR_RADIUS; r <= BLUR_RADIUS; r++) {
                        int clampedJ = max(min(j + r, w - 1), 0);
                        float sourcePixel = input[(i * w + clampedJ) * 4 + c];
                        blurredPixel += sourcePixel * k[r + BLUR_RADIUS];
                    }
                    aux[d0] = blurredPixel;
                    """);
        horizontalGaussianStage
                .setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{1024}));
        // Vertical
        Stage verticalGaussianStage = new Stage();
        verticalGaussianStage.setInputs(Map.of("aux", aux, "w", w, "h", h));
        verticalGaussianStage.setOutputs(Map.of("output", output));
        verticalGaussianStage.setKernelSource("""
                    const int BLUR_RADIUS = 5;
                    const float k[11] = {0.00571868f, 0.02112909f, 0.05837343f, 0.12061129f, 0.18640801f,
                0.21551899f, 0.18640801f, 0.12061129f, 0.05837343f, 0.02112909f, 0.00571868f};
                    int c = (int) d0 % 4;
                    int j = ((int) d0 / 4) % w;
                    int i = ((int) d0 / 4) / w;
                    float blurredPixel = 0.0f;
                    for (int r = -BLUR_RADIUS; r <= BLUR_RADIUS; r++) {
                        int clampedI = max(min(i + r, h - 1), 0);
                        float sourcePixel = aux[(clampedI * w + j) * 4 + c];
                        blurredPixel += sourcePixel * k[r + BLUR_RADIUS];
                    }
                    output[d0] = blurredPixel;
                    """);
        verticalGaussianStage
                .setRunConfiguration(new RunConfiguration(new long[]{w * h * 4}, new long[]{1024}));
        jclStages.add(horizontalGaussianStage);
        jclStages.add(verticalGaussianStage);
    }

    @Override
    public void runJavaOnce(byte [] input, byte [] output, int w, int h) {
        if (aux == null) {
            aux = new byte[w * h * 4];
        }
        if (aux.length != (w * h * 4)) {
            aux = new byte[w * h * 4];
        }
        // Horizontal
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    float blurredPixel = 0.0f;
                    for (int r = -BLUR_RADIUS; r <= BLUR_RADIUS; r++) {
                        int clampedJ = Math.max(Math.min(j + r, w - 1), 0);
                        float sourcePixel = input[(i * w + clampedJ) * 4 + c] & 0xff;
                        blurredPixel += sourcePixel * kernel[r + BLUR_RADIUS];
                    }
                    blurredPixel = Math.min(blurredPixel, 255.0f);
                    aux[(i * w + j) * 4 + c] = Float.valueOf(blurredPixel).byteValue();
                }
            }
        }
        // Vertical
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                for (int c = 0; c < 4; c++) {
                    float blurredPixel = 0.0f;
                    for (int r = -BLUR_RADIUS; r <= BLUR_RADIUS; r++) {
                        int clampedI = Math.max(Math.min(i + r, h - 1), 0);
                        float sourcePixel = aux[(clampedI * w + j) * 4 + c] & 0xff;
                        blurredPixel += sourcePixel * kernel[r + BLUR_RADIUS];
                    }
                    blurredPixel = Math.min(blurredPixel, 255.0f);
                    output[(i * w + j) * 4 + c] = (byte) blurredPixel;
                }
            }
        }
    }
}
