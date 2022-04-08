package es.ull.pcg.hpc.fancyjcl;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

import es.ull.pcg.hpc.fancier.array.ByteArray;
import es.ull.pcg.hpc.fancier.array.DoubleArray;
import es.ull.pcg.hpc.fancier.array.FloatArray;
import es.ull.pcg.hpc.fancier.array.IntArray;
import es.ull.pcg.hpc.fancier.array.ShortArray;
import es.ull.pcg.hpc.fancier.image.RGBAImage;
import timber.log.Timber;

public class FancierConverter {

    static public Object convert(Object input) throws Exception {
        // Already fancier or basic
        if (isFancierType(input) || isBasicType(input)) {
            return input;
        }
        // Bitmap
        if (input.getClass().getCanonicalName().equals("android.graphics.Bitmap")) {
            return new RGBAImage((Bitmap) input);
        }
        // Java buffers
        if (input.getClass().getCanonicalName().contains("Buffer")) {
            String className = input.getClass().getCanonicalName();
            className = className.replaceAll(".*[.]", "");
            className = className.replaceAll("Buffer", "");
            className = className.replaceAll("Direct", "");
            return switch (className.toLowerCase()) {
                case "byte" -> new ByteArray((ByteBuffer) input);
                case "short" -> new ShortArray((ShortBuffer) input);
                case "int" -> new IntArray((IntBuffer) input);
                case "float" -> new FloatArray((FloatBuffer) input);
                case "double" -> new DoubleArray((DoubleBuffer) input);
                default -> throw new Exception("Provided parameter has an unknown type: " +
                        input.getClass().getComponentType());
            };
        }
        // Java arrays
        return switch (input.getClass().getComponentType().getCanonicalName()) {
            case "byte" -> new ByteArray((byte[]) input);
            case "short" -> new ShortArray((short[]) input);
            case "int" -> new IntArray((int[]) input);
            case "float" -> new FloatArray((float[]) input);
            case "double" -> new DoubleArray((double[]) input);
            default -> throw new Exception("Provided parameter has an unknown type: " +
                    input.getClass().getComponentType());
        };
    }

    static public void syncToOCL(Object input) throws Exception {
        String className = FancierConverter.getType(input);
        switch (className) {
            case "ByteArray" -> ((ByteArray) input).syncToOCL();
            case "ShortArray" -> ((ShortArray) input).syncToOCL();
            case "IntArray" -> ((IntArray) input).syncToOCL();
            case "FloatArray" -> ((FloatArray) input).syncToOCL();
            case "DoubleArray" -> ((DoubleArray) input).syncToOCL();
        }
    }

    static public Object getArray(Object input) throws Exception {
        String className = FancierConverter.getType(input);
        return switch (className) {
            case "bytearray" -> ((ByteArray) input).getArray();
            case "shortarray" -> ((ShortArray) input).getArray();
            case "intarray" -> ((IntArray) input).getArray();
            case "floatarray" -> ((FloatArray) input).getArray();
            case "doublearray" -> ((DoubleArray) input).getArray();
            default -> throw new Exception("Provided parameter has an unknown type: " +
                    input.getClass().getComponentType());
        };
    }

    static public void syncToNative(Object input) throws Exception {
        String className = FancierConverter.getType(input);
        switch (className) {
            case "bytearray" -> ((ByteArray) input).syncToNative();
            case "shortarray" -> ((ShortArray) input).syncToNative();
            case "intarray" -> ((IntArray) input).syncToNative();
            case "floatarray" -> ((FloatArray) input).syncToNative();
            case "doublearray" -> ((DoubleArray) input).syncToNative();
            case "rgbaimage" -> ((RGBAImage) input).syncToNative();
            default -> throw new Exception("Provided parameter has an unknown type: " +
                    input.getClass().getCanonicalName());
        }
    }

    static public long getSize(Object input) {
        String className = input.getClass().getCanonicalName();
        if (className.contains("ByteArray"))
            return ((ByteArray) input).length();
        if (className.contains("ShortArray"))
            return ((ShortArray) input).length();
        if (className.contains("IntArray"))
            return ((IntArray) input).length();
        if (className.contains("FloatArray"))
            return ((FloatArray) input).length();
        if (className.contains("DoubleArray"))
            return ((DoubleArray) input).length();
        if (className.contains("ByteBuffer"))
            return ((ByteBuffer) input).capacity();
        if (className.contains("ShortBuffer"))
            return ((ShortBuffer) input).capacity();
        if (className.contains("IntBuffer"))
            return ((IntBuffer) input).capacity();
        if (className.contains("FloatBuffer"))
            return ((FloatBuffer) input).capacity();
        if (className.contains("DoubleBuffer"))
            return ((DoubleBuffer) input).capacity();
        // Basic type
        return 0;
    }

    static public String getType(Object input) {
        String className = input.getClass().getCanonicalName();
        className = className.replaceAll(".*[.]", "");
        className = className.replaceAll("\\[|\\]", "Array");
        className = className.replaceAll("Buffer", "Array");
        className = className.replaceAll("Direct", "");
        className = className.replaceAll("Bitmap", "rgbaimage");
        className = className.toLowerCase();
        return className;
    }

    static public String getOCLType(Object input) {
        String type = getType(input);
        type = type.replace("array", "*");
        type = type.replace("byte", "char");
        type = type.replace("rgbaimage", "uchar4*");
        return type;
    }

    static public boolean isFancierType(Object input) {
        String className = Objects.requireNonNull(input.getClass().getCanonicalName());
        return className.contains("fancier");
    }

    public static boolean isBasicType(Object data) {
        if (isFancierType(data))
            return false;
        String className = data.getClass().getCanonicalName();
        if (className.contains("["))
            return false;
        if (className.contains("Buffer"))
            return false;
        if (className.contains("Bitmap"))
            return false;
        return true;
    }
}
