package es.ull.pcg.hpc.fancierfrontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Stage {
    private final String name;
    private final String kernelName;
    private final Map<String, Parameter> parameters = new HashMap<>();
    // This attribute will be set from JNI when kernel is created in `prepare`
    public long cl_kernel_ptr;
    private String kernelSource = null;
    private RunConfiguration runConfiguration = null;

    public Stage(String name) {
        this.name = name;
        this.kernelName = name;
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
        String signature = "kernel void " + name + "(";
        String[] names = getOrderedParameterNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String oclType = FancierConverter.getOCLType(parameters.get(name).data);
            if (FancierConverter.isBasicType(parameters.get(name).data)) {
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
            Object input = entry.getValue();
            Object converted = FancierConverter.convert(input);
            parameters.put(name, new Parameter(ParameterClass.INPUT, converted,
                    FancierConverter.getType(converted), idx));
            idx += 1;
        }
    }

    public void setOutputs(Map<String, Object> outputElements) throws Exception {
        int idx = parameters.size();
        // Make the outputs fancier
        for (Map.Entry<String, Object> entry : outputElements.entrySet()) {
            String name = entry.getKey();
            Object output = entry.getValue();
            // Check that the output is not an input (in-place operation)
            if (parameters.containsKey(name)) {
                parameters.get(name).parameterClass = ParameterClass.INPUTOUTPUT;
                continue;
            }
            Object converted = FancierConverter.convert(output);
            parameters.put(name, new Parameter(ParameterClass.OUTPUT, converted,
                    FancierConverter.getType(converted), idx));
            idx += 1;
        }
    }

    public Object getParameter(String name) throws Exception {
        return FancierConverter.getArray(parameters.get(name).data);
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
                    parameterData.add(entry.getValue().data);
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
    public void prepare() throws Exception {
        // Compile and set arguments kernel
        prepare(generateKernel(), kernelName, getOrderedParameterNames(), getOrderedParameters(),
                getOrderedParameterTypes());
    }

    public void run() {
        run(cl_kernel_ptr, runConfiguration.getDimensions(), runConfiguration.getParallelization());
    }

    public void syncInputsToGPU() throws Exception {
        for (Parameter param : parameters.values()) {
            if (param.parameterClass == ParameterClass.INPUT || param.parameterClass == ParameterClass.INPUTOUTPUT) {
                FancierConverter.syncToOCL(param.data);
            }
        }
    }

    public void syncOutputsToCPU() throws Exception {
        for (Parameter param : parameters.values()) {
            if (param.parameterClass == ParameterClass.OUTPUT || param.parameterClass == ParameterClass.INPUTOUTPUT) {
                FancierConverter.syncToNative(param.data);
            }
        }
    }

    public void waitUntilExecutionEnds() {
        waitForQueueToFinish();
    }

    public void setRunConfiguration(RunConfiguration runConfiguration) {
        this.runConfiguration = runConfiguration;
    }

    public void printSummary() throws Exception {
        Timber.i("****************************************" +
                "****************************************");
        Timber.i("\t - STAGE NAME: %s", name);
        // Print inputs
        Timber.i("\t - PARAMETERS:");
        for (Map.Entry<String, Parameter> param : parameters.entrySet()) {
            long size = FancierConverter.getSize(param.getValue().data);
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
