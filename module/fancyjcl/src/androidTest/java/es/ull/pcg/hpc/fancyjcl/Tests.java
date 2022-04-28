package es.ull.pcg.hpc.fancyjcl;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Map;

public class Tests extends TestCase {
    public void testBasic() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 25;
        short kConstant = 10;
        byte[] input = new byte[size];
        byte[] output = new byte[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) i;
        }
        Stage stage = new Stage();
        stage.setKernelSource("""
                output[d0] = input[d0] * kConstant;
                        """);
        stage.setInputs(Map.of("input", input, "kConstant", kConstant));
        stage.setOutputs(Map.of("output", output));
        stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));
        stage.runSync();
        for (int i = 0; i < output.length; i++) {
            assertEquals(output[i] & 0xff, input[i] * kConstant, 0.0f);
        }
        FancyJCLManager.clear();
    }

    public void testInPlace() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 25;
        float kConstant = 5;
        // Have the data in java
        byte[] data = new byte[size];
        byte[] outputGt = new byte[size];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
            outputGt[i] = (byte) (data[i] * kConstant);
        }
        Stage stage = new Stage();
        stage.setKernelSource("""
                data[d0] = data[d0] * kConstant;
                        """);
        stage.setInputs(Map.of("data", data, "kConstant", kConstant));
        stage.setOutputs(Map.of("data", data));
        stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));
        stage.runSync();
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], outputGt[i], 0.0f);
        }
        FancyJCLManager.clear();
    }

    public void testBitmap() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        Bitmap imgIn = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Bitmap imgOut = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        Stage stage = new Stage();
        stage.setKernelSource("""
                img_out[d0] = img_in[d0] + (uchar4)(124, 125, 126, 220);
                        """);
        stage.setInputs(Map.of("img_in", imgIn));
        stage.setOutputs(Map.of("img_out", imgOut));
        stage.setRunConfiguration(new RunConfiguration(new long[]{512 * 512}, new long[]{1}));
        stage.runSync();
        ByteBuffer buffer = ByteBuffer.allocate(imgOut.getByteCount());
        imgOut.copyPixelsToBuffer(buffer);
        for (int i = 0; i < imgOut.getHeight(); i++) {
            for (int j = 0; j < imgOut.getWidth(); j++) {
                assertEquals(124,
                        Byte.toUnsignedInt(buffer.get((i * imgIn.getWidth() + j) * 4 + 0)), 0.0f);
                assertEquals(125,
                        Byte.toUnsignedInt(buffer.get((i * imgIn.getWidth() + j) * 4 + 1)), 0.0f);
                assertEquals(126,
                        Byte.toUnsignedInt(buffer.get((i * imgIn.getWidth() + j) * 4 + 2)), 0.0f);
                assertEquals(220,
                        Byte.toUnsignedInt(buffer.get((i * imgIn.getWidth() + j) * 4 + 3)), 0.0f);
            }
        }
        FancyJCLManager.clear();
    }

    public void testMultipleStages() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 10;
        float[] array = new float[size];
        float[] aux = new float[size];
        float[] output = new float[size];
        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        Stage power = new Stage();
        power.setKernelSource("""
                aux[d0] = array[d0] * array[d0];
                        """);
        power.setInputs(Map.of("array", array));
        power.setOutputs(Map.of("aux", aux));
        power.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));
        Stage threshold = new Stage();
        threshold.setKernelSource("""
                output[d0] = (aux[d0] > 25.0f)? 1.0f : 0.0f;
                        """);
        threshold.setInputs(Map.of("aux", aux));
        threshold.setOutputs(Map.of("output", output));
        threshold.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{1}));
        power.syncInputsToGPU();
        power.run();
        threshold.run();
        threshold.syncOutputsToCPU();
        for (int i = 0; i < output.length; i++) {
            assertEquals(((array[i] * array[i]) > 25.f) ? 1.0f : 0.0f, output[i], 0.0f);
        }
        FancyJCLManager.clear();
    }


    public void testBuffers() throws Exception {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FancyJCLManager.initialize(ctx.getCacheDir().getAbsolutePath());
        int size = 25;
        float kConstant = 3;
        ByteBuffer input = ByteBuffer.allocateDirect(size);
        ByteBuffer output = ByteBuffer.allocateDirect(size);
        for (int i = 0; i < input.capacity(); i++) {
            input.put(i, (byte) i);
        }
        Stage stage = new Stage();
        stage.setKernelSource("""
                output[d0] = input[d0] * kConstant;
                        """);
        stage.setInputs(Map.of("input", input, "kConstant", kConstant));
        stage.setOutputs(Map.of("output", output));
        stage.setRunConfiguration(new RunConfiguration(new long[]{size}, new long[]{size}));
        stage.runSync();
        for (int i = 0; i < output.capacity(); i++) {
            assertEquals(input.get(i) * kConstant, output.get(i), 1.0f);
        }
        FancyJCLManager.clear();
    }
}
