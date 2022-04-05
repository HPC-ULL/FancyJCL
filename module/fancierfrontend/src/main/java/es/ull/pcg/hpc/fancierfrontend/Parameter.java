package es.ull.pcg.hpc.fancierfrontend;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

import es.ull.pcg.hpc.fancier.image.RGBAImage;

enum ParameterClass {
    INPUT,
    INPUTOUTPUT,
    OUTPUT
}

public class Parameter {
    ParameterClass parameterClass;
    Object fancierData;
    String type;
    int index;
    Object javaData;

    public Parameter(ParameterClass parameterClass, Object javaData, Object fancierData,
                     String type, int index) {
        this.parameterClass = parameterClass;
        this.fancierData = fancierData;
        this.javaData = javaData;
        this.type = type;
        this.index = index;
    }

    public void syncToJava() throws Exception {
        if (type.equals("RGBAImage")) {
            RGBAImage fancierDataImage = ((RGBAImage) fancierData);
            ByteBuffer bb = fancierDataImage.getBuffer();
            ((Bitmap) javaData).copyPixelsFromBuffer(bb);
            return;
        }
        Object jdata = FancierConverter.getArray(fancierData);
        long length = FancierConverter.getSize(fancierData);
        System.arraycopy(jdata, 0, javaData, 0, (int) length);
    }

}
