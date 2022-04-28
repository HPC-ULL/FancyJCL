package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.Map;

import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.RunConfiguration;
import es.ull.pcg.hpc.fancyjcl.Stage;
import es.ull.pcg.hpc.fancyjcl_example.MainActivity;

public class GrayScale extends Filter {

    @Override
    public void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        FancyJCLManager.initialize(String.valueOf(MainActivity.ctx.getCacheDir()));
        Stage grayscale = new Stage();
        grayscale.setInputs(Map.of("input", input));
        grayscale.setOutputs(Map.of("output", output));
        grayscale.setKernelSource("""
                int offset = (int) d0 * 4;
                int r =  input[offset + 0];
                int g =  input[offset + 1];
                int b =  input[offset + 2];
                int a =  input[offset + 3];
                char gray = clamp(r * 0.299f + g * 0.587f + b * 0.114f, 0.0f, 255.0f);
                output[offset + 0] = gray;
                output[offset + 1] = gray;
                output[offset + 2] = gray;
                output[offset + 3] = a;
                """);
        grayscale.setRunConfiguration(new RunConfiguration(new long[]{w * h}, new long[]{1024}));
        jclStages.add(grayscale);
    }

    @Override
    public void runJavaOnce(ByteBuffer input, ByteBuffer output, int w, int h) {
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int pos2d = (i * w + j) * 4;
                int r = input.get(pos2d) & 0xff;
                int g = input.get(pos2d + 1) & 0xff;
                int b = input.get(pos2d + 2) & 0xff;
                int a = input.get(pos2d + 3) & 0xff;
                float gray = (r * 0.299f + g * 0.587f + b * 0.114f);
                gray = Math.max(0.0f, gray);
                gray = Math.min(255.0f, gray);
                output.put(pos2d, (byte) gray);
                output.put(pos2d + 1, (byte) gray);
                output.put(pos2d + 2, (byte) gray);
                output.put(pos2d + 3, (byte) a);
            }
        }
    }
}
