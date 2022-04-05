package es.ull.pcg.hpc.fancierfrontend;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.ull.pcg.hpc.fancier.image.RGBAImage;
import timber.log.Timber;

public class Stage {
    private final String kernelName;
    private final Map<String, Parameter> parameters = new HashMap<>();
    // This attribute will be set from JNI when kernel is created in `prepare`
    public long cl_kernel_ptr;
    private String kernelSource = null;
    private RunConfiguration runConfiguration = null;

    public Stage() {
        this.kernelName = "kernel_" + FancierManager.kernelCount;
        FancierManager.kernelCount += 1;
    }

    private native long prepare(String kernel_source, String kernel_name,
                                Object[] parameter_names, Object[] parameters,
                                Object[] parameter_types);

    private native long run(long cl_kernel_ptr, long[] dimensions, long[] parallelization);

    private native long waitForQueueToFinish();

    public void setKernelSource(String kernelSource) {
        this.kernelSource = kernelSource;
    }

    private String generateKernel() throws Exception {
        String signature = "kernel void " + kernelName + "(";
        String[] names = getOrderedParameterNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String oclType = FancierConverter.getOCLType(parameters.get(name).fancierData);
            if (FancierConverter.isBasicType(parameters.get(name).fancierData)) {
                signature += "const " + oclType + " " + name;
            } else if (parameters.get(names[i]).parameterClass == ParameterClass.INPUT) {
                signature += "global const " + oclType + " " + name;
            } else {
                signature += "global " + oclType + " " + name;
            }
            if (i < (parameters.size() - 1)) {
                signature += ", ";
            }
        }
        signature += ") {\n";
        String kernelEnd = "\n}\n";
        String modifiedKernelSource = kernelSource;
        // Shorthand for global id (dimensions) d0, d1, ... d9
        for (int i = 0; i < 10; i++) {
            modifiedKernelSource = modifiedKernelSource.replaceAll("(\\W+)(d" + i + ")(\\W+)",
                    "$1get_global_id(" + i + ")$3");
        }
        return signature + modifiedKernelSource + kernelEnd;
    }

    public void setInputs(Map<String, Object> inputElements) throws Exception {
        int idx = 0;
        // Make the inputs fancier
        for (Map.Entry<String, Object> entry : inputElements.entrySet()) {
            String name = entry.getKey();
            if (FancierConverter.isFancierType(entry.getValue())) {
                Object fancierData = entry.getValue();
                String type = FancierConverter.getType(fancierData);
                parameters.put(name, new Parameter(ParameterClass.INPUT, null, fancierData, type, idx));
            } else {
                Object javaData = entry.getValue();
                Object fancierData = FancierConverter.convert(javaData);
                String type = FancierConverter.getType(fancierData);
                parameters.put(name, new Parameter(ParameterClass.INPUT, javaData, fancierData, type, idx));
            }
            idx += 1;
        }
    }

    public void setOutputs(Map<String, Object> outputElements) throws Exception {
        int idx = parameters.size();
        // Make the outputs fancier
        for (Map.Entry<String, Object> entry : outputElements.entrySet()) {
            String name = entry.getKey();
            if (parameters.containsKey(name)) {
                parameters.get(name).parameterClass = ParameterClass.INPUTOUTPUT;
                continue;
            }
            if (FancierConverter.isFancierType(entry.getValue())) {
                Object fancierData = entry.getValue();
                String type = FancierConverter.getType(fancierData);
                parameters.put(name, new Parameter(ParameterClass.OUTPUT, null, fancierData, type, idx));
            } else {
                Object javaData = entry.getValue();
                // Check that the output is not an input (in-place operation)
                Object fancierData = FancierConverter.convert(javaData);
                String type = FancierConverter.getType(fancierData);
                parameters.put(name, new Parameter(ParameterClass.OUTPUT, javaData, fancierData, type, idx));
            }
            idx += 1;
        }
    }

    private String[] getOrderedParameterNames() {
        ArrayList<String> parameterNames = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                if (entry.getValue().index == i) {
                    parameterNames.add(entry.getKey());
                }
            }
        }
        String[] array = new String[parameterNames.size()];
        parameterNames.toArray(array);
        return array;
    }

    private Object[] getOrderedParameters() {
        ArrayList<Object> parameterData = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                if (entry.getValue().index == i) {
                    parameterData.add(entry.getValue().fancierData);
                }
            }
        }
        return parameterData.toArray();
    }

    private String[] getOrderedParameterTypes() {
        ArrayList<String> parameterTypes = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
                if (entry.getValue().index == i) {
                    parameterTypes.add(entry.getValue().type);
                }
            }
        }
        String[] array = new String[parameterTypes.size()];
        parameterTypes.toArray(array);
        return array;
    }

    /**
     * Prepare.
     * Compiles the kernel and sets its parameters
     *
     * @throws Exception the exception
     */
    private void prepare() throws Exception {
        // Compile and set arguments kernel
        prepare(generateKernel(), kernelName, getOrderedParameterNames(), getOrderedParameters(),
                getOrderedParameterTypes());
    }

    public void run() {
        run(cl_kernel_ptr, runConfiguration.getDimensions(), runConfiguration.getParallelization());
    }

    public void runSync() throws Exception {
        syncInputsToGPU();
        run(cl_kernel_ptr, runConfiguration.getDimensions(), runConfiguration.getParallelization());
        waitUntilExecutionEnds();
        syncOutputsToCPU();
    }

    public void syncInputsToGPU() throws Exception {
        for (Parameter param : parameters.values()) {
            if (param.parameterClass == ParameterClass.INPUT || param.parameterClass == ParameterClass.INPUTOUTPUT) {
                FancierConverter.syncToOCL(param.fancierData);
            }
        }
    }

    public void syncOutputsToCPU() throws Exception {
        for (Parameter param : parameters.values()) {
            if (param.parameterClass == ParameterClass.OUTPUT || param.parameterClass == ParameterClass.INPUTOUTPUT) {
                FancierConverter.syncToNative(param.fancierData);
                param.syncToJava();
            }
        }
    }

    public void waitUntilExecutionEnds() {
        waitForQueueToFinish();
    }

    public void setRunConfiguration(RunConfiguration runConfiguration) throws Exception {
        this.runConfiguration = runConfiguration;
        prepare();
    }

    public void printSummary() throws Exception {
        Timber.i("****************************************" +
                "****************************************");
        Timber.i("\t - STAGE NAME: %s", kernelName);
        // Print inputs
        Timber.i("\t - PARAMETERS:");
        for (Map.Entry<String, Parameter> param : parameters.entrySet()) {
            long size = FancierConverter.getSize(param.getValue().fancierData);
            String pclass = "INPUT";
            if (param.getValue().parameterClass == ParameterClass.INPUTOUTPUT)
                pclass = "INPUTOUTPUT";
            else if (param.getValue().parameterClass == ParameterClass.OUTPUT)
                pclass = "OUTPUT";

            if (size != 0) {
                Timber.i("\t\t%d: [%s] \"%s\" (%s)[%d]", param.getValue().index, pclass,
                        param.getKey(),
                        param.getValue().type, size);
            } else {
                Timber.i("\t\t%d: [%s]\"%s\" (%s)", param.getValue().index, pclass, param.getKey(),
                        param.getValue().type);
            }
        }
        // Print kernel
        Timber.i("\t - KERNEL:");
        if (kernelSource == null) {
            Timber.i("\t\t no kernel defined");
        } else {
            Timber.i("----------------------------------------" +
                    "----------------------------------------");
            for (String line : generateKernel().split("\n")) {
                Timber.i(line);
            }
            Timber.i("----------------------------------------" +
                    "----------------------------------------");
        }

        Timber.i("\t - RUN CONFIGURATION:\n");
        Timber.i("\t\t%s", runConfiguration.getDimensionsAsString());
        Timber.i("\t\t%s", runConfiguration.getParallelizationAsString());
        Timber.i("****************************************" +
                "****************************************");
    }

}
