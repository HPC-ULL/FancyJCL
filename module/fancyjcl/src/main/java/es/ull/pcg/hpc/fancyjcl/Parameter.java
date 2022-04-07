package es.ull.pcg.hpc.fancyjcl;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import es.ull.pcg.hpc.fancier.image.RGBAImage;
import timber.log.Timber;

enum ParameterClass {
    INPUT("INPUT"),
    INPUTOUTPUT("INPUTOUTPUT"),
    OUTPUT("OUTPUT");

    private final String name;

    ParameterClass(String name) {
        this.name = name;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}

// Each parameter has only one reference per Stage
class Reference {
    ParameterClass parameterClass;
    int parameterIndex;

    public Reference(ParameterClass parameterClass, int parameterIndex) {
        this.parameterClass = parameterClass;
        this.parameterIndex = parameterIndex;
    }
}

public class Parameter {
    Map<String, Reference> references; // Key is the stage where the param it is referenced from
    Object fancierData;
    String type;
    Object javaData;
    boolean hasJavaData;
    String name;

    public Parameter(String name, Object javaData, Object fancierData, String type) {
        this.fancierData = fancierData;
        hasJavaData = javaData != null;
        this.javaData = javaData;
        this.type = type;
        this.name = name;
        this.references = new HashMap<>();
    }

    public void syncToJava() throws Exception {
        if (!hasJavaData)
            return;
        if (type.equals("rgbaimage")) {
            RGBAImage fancierDataImage = ((RGBAImage) fancierData);
            ByteBuffer bb = fancierDataImage.getBuffer();
            ((Bitmap) javaData).copyPixelsFromBuffer(bb);
            return;
        }
        // Buffers are already sync
        if (javaData.getClass().getCanonicalName().contains("Buffer"))
            return;
        Object jdata = FancierConverter.getArray(fancierData);
        long length = FancierConverter.getSize(fancierData);
        System.arraycopy(jdata, 0, javaData, 0, (int) length);
    }

    public void addReference(String stageName, ParameterClass parameterClass, int index) {
        // If there is a reference within this, it means that parameter is INPUT/OUTPUT
        if (references.containsKey(stageName)) {
            references.get(stageName).parameterClass = ParameterClass.INPUTOUTPUT;
        } else {
            references.put(stageName, new Reference(parameterClass, index));
        }
    }

    public boolean isPresentInStage(String stageName) {
        for (String stage : references.keySet()) {
            if (stage.equals(stageName)) {
                return true;
            }
        }
        return false;
    }

    public Reference getReferenceInStage(String stage) throws Exception {
        for (Map.Entry<String, Reference> entry : references.entrySet()) {
            String entryName = entry.getKey();
            if (entryName.equals(stage)) {
                return entry.getValue();
            }
        }
        throw new Exception("Reference of parameter " + name + " in stage " + stage + " not found" +
                ".");
    }

    public void showDebugInfo() {
        Timber.d("**");
        Timber.d("\tParam name: %s", name);
        Timber.d("\tParam type: %s", type);
        if (hasJavaData) {
            Timber.d("\tHas Java data = YES");
        } else {
            Timber.d("\tHas Java data = YES");
        }
        Timber.d("\tReferenced for %d stages:", references.size());
        for (Map.Entry<String, Reference> entry : references.entrySet()) {
            Timber.d("\t - %s", entry.getKey());
            Timber.d("\t\t idx=%d", entry.getValue().parameterIndex);
            Timber.d("\t\t class=%s", entry.getValue().parameterClass);

        }
    }
}
