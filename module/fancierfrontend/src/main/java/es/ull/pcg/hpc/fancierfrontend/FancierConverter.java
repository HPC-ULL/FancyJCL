package es.ull.pcg.hpc.fancierfrontend;

import android.graphics.Bitmap;

import java.util.Objects;

import es.ull.pcg.hpc.fancier.array.ByteArray;
import es.ull.pcg.hpc.fancier.array.DoubleArray;
import es.ull.pcg.hpc.fancier.array.FloatArray;
import es.ull.pcg.hpc.fancier.array.IntArray;
import es.ull.pcg.hpc.fancier.array.ShortArray;
import es.ull.pcg.hpc.fancier.image.RGBAImage;
import timber.log.Timber;

public class FancierConverter {
    // TODO a Base class for all fancier arrays would avoid having this class

    static public Object convert(Object input) throws Exception {
        // Check that class is an array
        if (!input.getClass().isArray()) {
            if (input.getClass().getCanonicalName().equals("android.graphics.Bitmap")) {
                return FancierConverter.convert(((Bitmap) input));
            }
            return input;
        }
        return switch (input.getClass().getComponentType().getCanonicalName()) {
            case "byte" -> FancierConverter.convert((byte[]) input);
            case "short" -> FancierConverter.convert((short[]) input);
            case "int" -> FancierConverter.convert((int[]) input);
            case "float" -> FancierConverter.convert((float[]) input);
            case "double" -> FancierConverter.convert((double[]) input);
            default -> throw new Exception("Provided parameter has an unknown type: " + input.getClass().getComponentType());
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
            case "ByteArray" -> ((ByteArray) input).getArray();
            case "ShortArray" -> ((ShortArray) input).getArray();
            case "IntArray" -> ((IntArray) input).getArray();
            case "FloatArray" -> ((FloatArray) input).getArray();
            case "DoubleArray" -> ((DoubleArray) input).getArray();
            default -> throw new Exception("Provided parameter has an unknown type: " + input.getClass().getComponentType());
        };
    }

    static public void syncToNative(Object input) throws Exception {
        String className = FancierConverter.getType(input);
        switch (className) {
            case "ByteArray" -> ((ByteArray) input).syncToNative();
            case "ShortArray" -> ((ShortArray) input).syncToNative();
            case "IntArray" -> ((IntArray) input).syncToNative();
            case "FloatArray" -> ((FloatArray) input).syncToNative();
            case "DoubleArray" -> ((DoubleArray) input).syncToNative();
            case "RGBAImage" -> ((RGBAImage) input).syncToNative();
            default -> throw new Exception("Provided parameter has an unknown type: " + input.getClass().getComponentType());
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
        // Basic type
        return 0;
    }

    static public String getType(Object input) throws Exception {
        String className = input.getClass().getCanonicalName();
        if (!isBasicType(input)) {
            return className.replaceAll(".*[.]", "");
        }
        // Basic type
        switch (className) {
            case "java.lang.Byte" -> {
                return "char";
            }
            case "java.lang.Short" -> {
                return "short";
            }
            case "java.lang.Integer" -> {
                return "int";
            }
            case "java.lang.Float" -> {
                return "float";
            }
            case "java.lang.Double" -> {
                return "double";
            }
            default -> throw new Exception("Provided parameter has an unknown type: " + className);
        }
    }

    static public boolean isBasicType(Object input) {
        return !input.getClass().getCanonicalName().contains("fancier");
    }

    static public String getOCLType(Object input) throws Exception {
        if (!isBasicType(input)) {
            String className = Objects.requireNonNull(input.getClass().getCanonicalName());
            className = className.replaceAll(".*[.]", ""); // Remove
            className = className.toLowerCase();
            className = className.replace("array", "*");
            className = className.replace("byte", "char");
            className = className.replace("rgbaimage", "uchar4*");
            return className;
        }
        return FancierConverter.getType(input);
    }

    static public ByteArray convert(byte[] input) {
        return new ByteArray(input);

    }

    static public ShortArray convert(short[] input) {
        return new ShortArray(input);
    }

    static public IntArray convert(int[] input) {
        return new IntArray(input);
    }

    static public FloatArray convert(float[] input) {
        return new FloatArray(input);
    }

    static public DoubleArray convert(double[] input) {
        return new DoubleArray(input);
    }

    static public RGBAImage convert(Bitmap input) {
        return new RGBAImage(input);
    }
}
