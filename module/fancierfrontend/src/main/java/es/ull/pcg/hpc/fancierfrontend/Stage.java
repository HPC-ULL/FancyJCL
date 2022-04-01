package es.ull.pcg.hpc.fancierfrontend;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import timber.log.Timber;

public class Stage {
    private final String name;
    // These two attributes will be set from JNI when kernel is created in `prepare`
    public long cl_program_ptr;
    public long cl_kernel_ptr;
    private String kernelSource = null;
    private final String kernelName;
    private ArrayList<Object> inputs = null;
    private ArrayList<String> inputTypes = null;
    private ArrayList<String> inputNames = null;
    private ArrayList<Object> outputs = null;
    private ArrayList<String> outputTypes = null;
    private ArrayList<String> outputNames = null;
    private boolean inputsInGpu = false;

    public Stage(String name) {
        this.name = name;
        this.kernelName = name;
    }

    private native long prepare(String kernel_source, String kernel_name, Object[] inputs,
                                Object[] outputs, Object[] input_types, Object[] output_types);

    private native long run(long cl_kernel_ptr, int[] dimensions);
    private native long waitForQueueToFinish();

    public void setKernelSource(String kernelSource) {
        this.kernelSource = kernelSource;
    }

    private String generateKernel() {
        String signature = "kernel void " + name + "(";
        for (int i = 0; i < inputs.size(); i++) {
            signature += "global const " + FancierConverter.getOCLType(inputs.get(i)) + " " + inputNames.get(i);
            signature += ", ";
        }
        for (int i = 0; i < outputs.size(); i++) {
            signature += "global " + FancierConverter.getOCLType(outputs.get(i)) + " " + outputNames.get(i);
            if (i < (outputs.size() - 1))
                signature += ", ";
            else
                signature += ") {\n";
        }
        String kernelEnd = "\n}\n";
        String modifiedKernelSource = kernelSource;
        // Shorthand for global id (dimensions) d0, d1, ... d9
        for (int i = 0; i < 10; i++) {
            modifiedKernelSource = modifiedKernelSource.replaceAll("(\\W+)(d" + i + ")(\\W+)",
                    "$1get_global_id(" + i + ")$3");
        }
        return signature + modifiedKernelSource + kernelEnd;
    }

    public void setInputs(Object... inputElements) throws Exception {
        inputs = new ArrayList<>();
        inputTypes = new ArrayList<>();
        // Make the inputs fancier
        for (Object input : inputElements) {
            Object converted = FancierConverter.convert(input);
            inputs.add(converted);
            inputTypes.add(FancierConverter.getType(converted));
        }
        if (inputNames == null) {
            generateInputNames();
        }
    }

    public void setOutputs(Object... outputElements) throws Exception {
        outputs = new ArrayList<>();
        outputTypes = new ArrayList<>();
        // Make the outputs fancier
        for (Object output : outputElements) {
            Object converted = FancierConverter.convert(output);
            outputs.add(converted);
            outputTypes.add(FancierConverter.getType(converted));
        }
        if (outputNames == null) {
            generateOutputNames();
        }
    }

    public Object getOutput(int index) throws Exception {
        return FancierConverter.getArray(outputs.get(index));
    }

    private void generateInputNames() {
        inputNames = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            inputNames.add("input_" + Integer.valueOf(i).toString());
        }
    }

    private void generateOutputNames() {
        outputNames = new ArrayList<>();
        for (int i = 0; i < outputs.size(); i++) {
            outputNames.add("output_" + Integer.valueOf(i).toString());
        }
    }

    public void setInputNames(String... names) {
        inputNames = null;
        inputNames = new ArrayList<>();
        Collections.addAll(inputNames, names);
    }

    public void setOutputNames(String... names) {
        outputNames = null;
        outputNames = new ArrayList<>();
        Collections.addAll(outputNames, names);
    }

    public void prepare() throws Exception {
        // Compile and set arguments kernel
        if (!(kernelSource != null && inputs != null && outputs != null && inputNames != null && outputNames != null)) {
            throw new Exception("Input or output parameters are not defined yet.");
        }
        prepare(generateKernel(), kernelName, inputs.toArray(), outputs.toArray(),
                inputTypes.toArray(), outputTypes.toArray());
    }

    public void run(int... dimensions) {
        run(cl_kernel_ptr, dimensions);
    }

    public void syncInputsToGPU() throws Exception {
        for (Object input : inputs) {
            FancierConverter.syncToOCL(input);
        }
    }

    public void syncOutputsToCPU() throws Exception {
        for (Object output : outputs) {
            FancierConverter.syncToNative(output);
        }
    }

    public void waitUntilExecutionEnds() {
        waitForQueueToFinish();
    }

    public void printSummary() throws Exception {
        Timber.i("****************************************" +
                "****************************************");
        Timber.i("\tSTAGE NAME: %s", name);
        // Print inputs
        Timber.i("\tINPUTS:");
        if (inputs == null) {
            Timber.i("\t\t no inputs");
        } else {
            for (int i = 0; i < inputs.size(); i++) {
                long size = FancierConverter.getSize(inputs.get(i));
                Timber.i("\t\t%d: \"%s\" (%s)[%d]", i, inputNames.get(i),
                        inputTypes.get(i), size);
            }
        }
        // Print outputs
        Timber.i("\tOUTPUTS:");
        if (outputs == null) {
            Timber.i("\t\t no outputs");
        } else {
            for (int i = 0; i < outputs.size(); i++) {
                long size = FancierConverter.getSize(outputs.get(i));
                Timber.i("\t\t%d: \"%s\" (%s)[%d]", i, outputNames.get(i),
                        outputTypes.get(i), size);
            }
        }
        // Print kernel
        Timber.i("\tKERNEL:");
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
        Timber.i("****************************************" +
                "****************************************");
    }

}
