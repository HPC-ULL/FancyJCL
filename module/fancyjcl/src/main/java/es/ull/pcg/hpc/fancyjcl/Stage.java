package es.ull.pcg.hpc.fancyjcl;

import java.util.ArrayList;
import java.util.Map;

import timber.log.Timber;

public class Stage {
    private final String stageName;
    // This attribute will be set from JNI when kernel is created in `prepare`
    public long cl_kernel_ptr;
    private String kernelSource = null;
    private RunConfiguration runConfiguration = null;

    public Stage() {
        this.stageName = "kernel_" + FancyJCLManager.kernelCount;
        FancyJCLManager.kernelCount += 1;
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
        String signature = "kernel void " + stageName + "(";
        ArrayList<Parameter> parameters = FancyJCLManager.getParametersForStage(stageName);
        int idx = 0;
        for (Parameter p : parameters) {
            String name = p.name;
            String oclType = FancierConverter.getOCLType(p.fancierData);
            if (FancierConverter.isBasicType(p.fancierData)) {
                signature += "const " + oclType + " " + name;
            } else if (p.getReferenceInStage(stageName).parameterClass == ParameterClass.INPUT) {
                signature += "global const " + oclType + " " + name;
            } else {
                signature += "global " + oclType + " " + name;
            }
            if (idx < (parameters.size() - 1)) {
                signature += ", ";
            }
            idx += 1;
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
            Object data = entry.getValue();
            FancyJCLManager.addParameter(stageName, name, data, ParameterClass.INPUT, idx);
            idx += 1;
        }
    }

    public void setOutputs(Map<String, Object> outputElements) throws Exception {
        ArrayList<Parameter> parameters = FancyJCLManager.getParametersForStage(stageName);
        int idx = parameters.size();
        // Make the outputs fancier
        for (Map.Entry<String, Object> entry : outputElements.entrySet()) {
            String name = entry.getKey();
            Object data = entry.getValue();
            FancyJCLManager.addParameter(stageName, name, data, ParameterClass.OUTPUT, idx);
            idx += 1;
        }
    }


    /**
     * Prepare.
     * Compiles the kernel and sets its parameters
     *
     * @throws Exception the exception
     */
    private void prepare() throws Exception {
        // Compile and set arguments kernel
        prepare(generateKernel(), stageName,
                FancyJCLManager.getOrderedParamNamesForStage(stageName),
                FancyJCLManager.getOrderedParamDataForStage(stageName),
                FancyJCLManager.getOrderedParamTypes(stageName));
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
        ArrayList<Parameter> params = FancyJCLManager.getParametersForStage(stageName);
        for (Parameter param : params) {
            if (param.getReferenceInStage(stageName).parameterClass == ParameterClass.INPUT ||
                    param.getReferenceInStage(stageName).parameterClass ==
                            ParameterClass.INPUTOUTPUT) {
                FancierConverter.syncToOCL(param.fancierData);
            }
        }
    }

    public void syncOutputsToCPU() throws Exception {
        ArrayList<Parameter> parameters = FancyJCLManager.getParametersForStage(stageName);
        for (Parameter param : parameters) {
            if (param.getReferenceInStage(stageName).parameterClass == ParameterClass.OUTPUT ||
                    param.getReferenceInStage(stageName).parameterClass ==
                            ParameterClass.INPUTOUTPUT) {
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
        Timber.i("\t - STAGE NAME: %s", stageName);
        // Print inputs
        Timber.i("\t - PARAMETERS:");
        ArrayList<Parameter> parameters = FancyJCLManager.getParametersForStage(stageName);
        for (Parameter param : parameters) {
            long size = FancierConverter.getSize(param.fancierData);
            ParameterClass parameterClass = param.getReferenceInStage(stageName).parameterClass;
            int parameterIndex = param.getReferenceInStage(stageName).parameterIndex;
            if (size != 0) {
                Timber.i("\t\t%d: [%s] \"%s\" (%s)[%d]",
                        param.getReferenceInStage(stageName).parameterIndex,
                        parameterClass.toString(),
                        param.name,
                        param.type, size);
            } else {
                Timber.i("\t\t%d: [%s] \"%s\" (%s)", parameterIndex, parameterClass.toString(),
                        param.name,
                        param.type);
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
        if (runConfiguration != null) {
            Timber.i("\t\t%s", runConfiguration.getDimensionsAsString());
            Timber.i("\t\t%s", runConfiguration.getParallelizationAsString());
        } else {
            Timber.i("\t\t no run configuration defined");
        }
        Timber.i("****************************************" +
                "****************************************");
    }

}
