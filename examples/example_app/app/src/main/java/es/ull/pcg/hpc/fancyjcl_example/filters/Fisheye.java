package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class Fisheye extends Filter {
    final float STRENGTH = 1.4f;

    @Override
    public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage stage = new Stage();
        stage.setInputs(Map.of("input", input, "w", w, "h", h));
        stage.setOutputs(Map.of("output", output));
        stage.setKernelSource("""
                    const float STRENGTH = 1.4f;
                    int j = d0 % w;
                    int i = d0 / w;
                    float outputCoordX = ((float) j / (float) (w - 1) - 0.5f);
                    float outputCoordY = ((float) i / (float) (h - 1) - 0.5f);
                    float dist = sqrt(outputCoordX * outputCoordX + outputCoordY * outputCoordY);
                    float theta = atan2(outputCoordY, outputCoordX);
                    dist = pow(dist, STRENGTH);

                    float inputCoordX = (float) (dist * cos(theta));
                    float inputCoordY = (float) (dist * sin(theta));
                    float newCoordY = ((float) (h - 1)) * (inputCoordY + 0.5f);
                    float newCoordX = ((float) (w - 1)) * (inputCoordX + 0.5f);

                    // Bilinear sampling
                    int y0 = max((int)floor(newCoordY), 0);
                    int x0 = max((int)floor(newCoordX), 0);
                    y0 = min(y0, h - 1);
                    x0 = min(x0, w - 1);
                    int y1 = min(y0 + 1, h - 1);
                    int x1 = min(x0 + 1, w - 1);
                    float slopeY = newCoordY - y0;
                    float slopeX = newCoordX - x0;

                    for (int c = 0; c < 4; c++) {
                        float pixelY0 = (input[(y0 * w + x0) * 4 + c] & 0xff) * (1 - slopeY) + \
                            (input[(y1 * w + x0) * 4 + c] & 0xff) * slopeY;
                        float pixelY1 = (input[(y0 * w + x1) * 4 + c] & 0xff) * (1 - slopeY) + \
                            (input[(y1 * w + x1) * 4 + c] & 0xff) * slopeY;
                        float pixel = pixelY0 * (1 - slopeX) + pixelY1 * slopeX;
                        output[(i * w + j) * 4 + c] = clamp(pixel, 0.0f, 255.0f);
                    }
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
                float outputCoordX = ((float) j / (float) (w - 1) - 0.5f);
                float outputCoordY = ((float) i / (float) (h - 1) - 0.5f);
                float dist = (float) Math
                        .sqrt(outputCoordX * outputCoordX + outputCoordY * outputCoordY);
                float theta = (float) Math.atan2(outputCoordY, outputCoordX);
                dist = (float) (Math.pow(dist, STRENGTH));

                float inputCoordX = (float) (dist * Math.cos(theta));
                float inputCoordY = (float) (dist * Math.sin(theta));
                float newCoordY = (float) (h - 1) * (inputCoordY + 0.5f);
                float newCoordX = (float) (w - 1) * (inputCoordX + 0.5f);

                // Bilinear sampling
                int y0 = (int) Math.max(Math.floor(newCoordY), 0);
                int x0 = (int) Math.max(Math.floor(newCoordX), 0);
                y0 = Math.min(y0, h - 1);
                x0 = Math.min(x0, w - 1);
                int y1 = Math.min(y0 + 1, h - 1);
                int x1 = Math.min(x0 + 1, w - 1);
                float slopeY = newCoordY - y0;
                float slopeX = newCoordX - x0;

                for (int c = 0; c < 4; c++) {
                    float pixelY0 = (input.get((y0 * w + x0) * 4 + c) & 0xff) * (1 - slopeY) +
                            (input.get((y1 * w + x0) * 4 + c) & 0xff) * slopeY;
                    float pixelY1 = (input.get((y0 * w + x1) * 4 + c) & 0xff) * (1 - slopeY) +
                            (input.get((y1 * w + x1) * 4 + c) & 0xff) * slopeY;
                    float pixel = pixelY0 * (1 - slopeX) + pixelY1 * slopeX;
                    pixel = Math.max(0, pixel);
                    pixel = Math.min(255, pixel);
                    int position = (i * w + j) * 4 + c;
                    output.put(position, (byte) pixel);
                }
            }
        }
    }
}
