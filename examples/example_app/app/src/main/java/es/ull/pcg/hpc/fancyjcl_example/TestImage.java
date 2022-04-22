package es.ull.pcg.hpc.fancyjcl_example;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.nio.ByteBuffer;

public class TestImage {
    static Bitmap bitmap =
            BitmapFactory.decodeResource(MainActivity.ctx.getResources(), R.drawable.test_image);
    static Bitmap notComputedBitmap =
            BitmapFactory.decodeResource(MainActivity.ctx.getResources(), R.drawable.not_computed);

    static ByteBuffer get(int w, int h) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, w, h, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
        resized.copyPixelsToBuffer(buffer);
        buffer.flip();
        return buffer;
    }

    static Bitmap bufferToBitmap(ByteBuffer buffer, int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        buffer.flip();
        return bitmap;
    }

}
