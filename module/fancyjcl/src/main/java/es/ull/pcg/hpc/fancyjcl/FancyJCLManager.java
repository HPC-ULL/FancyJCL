package es.ull.pcg.hpc.fancyjcl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import es.ull.pcg.hpc.fancier.Fancier;
import es.ull.pcg.hpc.fancier.array.ByteArray;
import es.ull.pcg.hpc.fancier.array.DoubleArray;
import es.ull.pcg.hpc.fancier.array.FloatArray;
import es.ull.pcg.hpc.fancier.array.IntArray;
import es.ull.pcg.hpc.fancier.array.ShortArray;

/**
 * Manages initialization and releasing of FancyJCL and controls the set of stages and its
 * parameters.
 */
public class FancyJCLManager {
    // Set of all parameters of all stages
    private static final Map<String, Parameter> parameters = new HashMap<>();
    static int kernelCount = 0;
    private static boolean initialized = false;

    /**
     * Creates a context for FancyJCL. It must be called only once per execution of
     * the application.
     *
     * @param basePath Cache path needed for storing temporal data. It can usually be obtained by
     *                 calling {@code getApplicationContext().getCacheDir().getAbsolutePath()}
     *                 from your
     *                 MainActivity.
     */
    public static void initialize(String basePath) {
        if (!initialized) {
            System.loadLibrary("fancyjcl");
            Fancier.init(basePath);
            initialized = true;
        }
    }

    /**
     * Clears all the stages and all its parameters. It is called with {@code release()}
     * and it would be needed if you want to test different algorithms involving a set of stages
     * each one.
     */
    public static void clear() {
        for (Parameter parameter : parameters.values()) {
            if (parameter.fancierData != null) {
                if (parameter.type.equals("bytearray")) {
                    ((ByteArray) parameter.fancierData).release();
                }
                if (parameter.type.equals("shortarray")) {
                    ((ShortArray) parameter.fancierData).release();
                }
                if (parameter.type.equals("intarray")) {
                    ((IntArray) parameter.fancierData).release();
                }
                if (parameter.type.equals("floatarray")) {
                    ((FloatArray) parameter.fancierData).release();
                }
                if (parameter.type.equals("doublearray")) {
                    ((DoubleArray) parameter.fancierData).release();
                }
            }
            parameter.fancierData = null;
            parameter.javaData = null;
        }
        parameters.clear();
        kernelCount = 0;
    }

    /**
     * Clears all the stages and all its parameters and then releases the FancyJCL
     * context. It must be called only once per execution of the application, whenever FancyJCL
     * is not needed anymore.
     */
    public static void release() {
        FancyJCLManager.clear();
        Fancier.release();
    }

    // Adds a parameter, or a reference to the parameter.
    static void addParameter(String stageName, String parameterName, Object data,
                             ParameterClass parameterClass,
                             int idx) throws Exception {
        // If its already in parameters, just add a reference
        if (parameters.containsKey(parameterName)) {
            parameters.get(parameterName).addReference(stageName, parameterClass, idx);
        } else {
            // If it is not cached, create a new parameter
            if (FancierConverter.isFancierType(data)) {
                Object fancierData = data;
                String type = FancierConverter.getType(fancierData);
                parameters.put(parameterName, new Parameter(parameterName, null, fancierData,
                        type));
            } else {
                Object javaData = data;
                Object fancierData = FancierConverter.convert(javaData);
                String type = FancierConverter.getType(fancierData);
                parameters.put(parameterName, new Parameter(parameterName, javaData, fancierData,
                        type));
            }
            parameters.get(parameterName).addReference(stageName, parameterClass, idx);
        }
        parameters.get(stageName);
    }

    // Returns ordered parameters
    static ArrayList<Parameter> getParametersForStage(String stageName) {
        ArrayList<Parameter> output = new ArrayList<>();
        for (Parameter parameter : parameters.values()) {
            if (parameter.isPresentInStage(stageName)) {
                output.add(parameter);
            }
        }
        Collections.sort(output,
                (parameter, t1) -> Integer
                        .compare(parameter.references.get(stageName).parameterIndex,
                                t1.references.get(stageName).parameterIndex));
        return output;
    }

    static String[] getOrderedParamNamesForStage(String stage) {
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        ArrayList<String> parameterNames = new ArrayList<>();
        for (Parameter param : stageParams) {
            parameterNames.add(param.name);
        }
        String[] array = new String[parameterNames.size()];
        parameterNames.toArray(array);
        return array;
    }

    static Object[] getOrderedParamDataForStage(String stage) {
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        ArrayList<Object> parameterData = new ArrayList<>();
        for (Parameter param : stageParams) {
            parameterData.add(param.fancierData);
        }
        return parameterData.toArray();
    }

    static String[] getOrderedParamTypes(String stage) {
        ArrayList<String> parameterTypes = new ArrayList<>();
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        for (Parameter param : stageParams) {
            parameterTypes.add(param.type);
        }
        String[] array = new String[parameterTypes.size()];
        parameterTypes.toArray(array);
        return array;
    }

    static void showDebugInfo() {
        for (Parameter param : parameters.values()) {
            param.showDebugInfo();
        }

    }

}
