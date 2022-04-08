package es.ull.pcg.hpc.fancyjcl;

import java.util.ArrayList;
import java.util.Map;

import timber.log.Timber;

/**
 * Class representing an algorithm that has a set of parameters (inputs and outputs) and a run
 * configuration for its accelerated execution in GPU via OpenCL.
 */
public class Stage {
    private final String stageName;
    /**
     * This attribute will be set from JNI when kernel is created in `prepare`. It must be public
     * for this reason.
     */
    public long cl_kernel_ptr;
    private String kernelSource = null;
    private RunConfiguration runConfiguration = null;

    /**
     * Instantiates a new Stage, setting the stageName, and registering the stage in the Manager.
     */
    public Stage() {
        this.stageName = "kernel_" + FancyJCLManager.kernelCount;
        FancyJCLManager.kernelCount += 1;
    }

    private native long prepare(String kernel_source, String kernel_name,
                                Object[] parameter_names, Object[] parameters,
                                Object[] parameter_types);

    private native long run(long cl_kernel_ptr, long[] dimensions, long[] parallelization);

    private native long waitForQueueToFinish();

    /**
     * Sets kernel source. The variables used inside the kernel must match those of the input and
     * output parameters. The signature of the kernel will be automatically generated. Also there
     * is a shorthand for variables such that {@code dn} will be converted {@code get_global_id(n)}.
     * For more information on writing kernels check the TUTORIAL.
     *
     * @param kernelSource code that will be converted to OpenCL.
     */
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

    /**
     * Sets inputs for the Stage. Each input is an Object with a name. For example, in order to
     * set two input parameters called input and constant, we call the method like so:
     * <pre>{@code
     * stage.setInputs(Map.of("input", input, "constant", constant));
     * }</pre>
     * <p>
     * These kind of types are supported and will be zero-copy if initialized as DirectBuffer.
     * <ul>
     * <li> ByteBuffer  </li>
     * <li> CharBuffer  </li>
     * <li> ShortBuffer </li>
     * <li> IntBuffer   </li>
     * <li> FloatBuffer </li>
     * <li> DoubleBuffer</li>
     * </ul>
     * Note that the endianness of the architecture must be matched. So, initialize them as follow:
     * <pre>{@code
     * ByteBuffer input = ByteBuffer.allocateDirect(size);
     * input.order(ByteOrder.LITTLE_ENDIAN);
     * }</pre>
     *
     * <p>
     * These kind of types are supported but will not be zero-copy:
     * <ul>
     * <li> byte   []  </li>
     * <li> char   []  </li>
     * <li> short  []  </li>
     * <li> int    []  </li>
     * <li> float  []  </li>
     * <li> double []  </li>
     * </ul>
     *
     * @param inputElements A map where the key is the element name and the value is the input
     *                      object.
     * @throws Exception the exception
     */
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

    /**
     * Sets outputs for the Stage. Each output is an Object with a name. For example, in order to
     * a single output parameter called output, we call the method like so:
     * <pre>{@code
     * stage.setOutputs(Map.of("output", output));
     * }</pre>
     * <p>
     * These kind of types are supported and will be zero-copy if initialized as DirectBuffer.
     * <ul>
     * <li> ByteBuffer  </li>
     * <li> CharBuffer  </li>
     * <li> ShortBuffer </li>
     * <li> IntBuffer   </li>
     * <li> FloatBuffer </li>
     * <li> DoubleBuffer</li>
     * </ul>
     * Note that the endianness of the architecture must be matched. So, initialize them as follow:
     * <pre>{@code
     * ByteBuffer input = ByteBuffer.allocateDirect(size);
     * input.order(ByteOrder.LITTLE_ENDIAN);
     * }</pre>
     *
     * <p>
     * These kind of types are supported but will not be zero-copy:
     * <ul>
     * <li> byte   []  </li>
     * <li> char   []  </li>
     * <li> short  []  </li>
     * <li> int    []  </li>
     * <li> float  []  </li>
     * <li> double []  </li>
     * </ul>
     *
     * @param outputElements the output elements
     * @throws Exception the exception
     */
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
     * Generates the OpenCL kernel, compiles it and sets its parameters.
     */
    private void prepare() throws Exception {
        // Compile and set arguments kernel
        prepare(generateKernel(), stageName,
                FancyJCLManager.getOrderedParamNamesForStage(stageName),
                FancyJCLManager.getOrderedParamDataForStage(stageName),
                FancyJCLManager.getOrderedParamTypes(stageName));
    }

    /**
     * Enqueues the Stage to be run using the set {@link RunConfiguration}. This method is
     * non-blocking. <br>
     * It is used whenever several {@link Stage}s are going to be executed sequentially. Usually
     * all the stages of an algorithm are set to run with this method and then a call to
     * {@link Stage#waitUntilExecutionEnds()} is placed to ensure all the stages are executed in
     * GPU. For a blocking version of this method see {@link Stage#runSync()}.
     */
    public void run() {
        run(cl_kernel_ptr, runConfiguration.getDimensions(), runConfiguration.getParallelization());
    }

    /**
     * Synchronizes the inputs to GPU, enqueues this {@link Stage} to be run, waits until the
     * execution ends and synchronizes the outputs to CPU. If you are running several Stages
     * sequentially, avoid unneeded synchronizations by calling {@link Stage#run()} instead.
     */
    public void runSync() throws Exception {
        syncInputsToGPU();
        run(cl_kernel_ptr, runConfiguration.getDimensions(), runConfiguration.getParallelization());
        waitUntilExecutionEnds();
        syncOutputsToCPU();
    }

    /**
     * Synchronize inputs to GPU.
     */
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

    /**
     * Synchronize outputs to cpu.
     */
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

    /**
     * Wait until execution ends. This is a blocking call that ensures that all enqueued Stages
     * are finished.
     */
    public void waitUntilExecutionEnds() {
        waitForQueueToFinish();
    }

    /**
     * Sets run configuration. See {@link RunConfiguration}.
     *
     * @param runConfiguration the run configuration
     * @throws Exception the exception
     */
    public void setRunConfiguration(RunConfiguration runConfiguration) throws Exception {
        this.runConfiguration = runConfiguration;
        prepare();
    }

    /**
     * Print a summary showing information about the parameters (inputs and outputs), generated
     * OpenCL kernel and {@link RunConfiguration} of the {@link Stage}.
     */
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
