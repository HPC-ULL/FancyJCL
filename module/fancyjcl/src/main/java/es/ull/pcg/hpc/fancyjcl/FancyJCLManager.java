package es.ull.pcg.hpc.fancyjcl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import es.ull.pcg.hpc.fancier.Fancier;

public class FancyJCLManager {
    private static boolean initialized = false;
    public static int kernelCount = 0;
    // Set of all parameters of all stages
    public static final Map<String, Parameter> parameters = new HashMap<>();

    public static void initialize(String basePath) {
        if (!initialized) {
            System.loadLibrary("fancyjcl");
            Fancier.init(basePath);
            initialized = true;
        }
    }

    public static void clear() {
        parameters.clear();
    }

    public static void release() {
        Fancier.release();
    }
    // Adds a parameter, or a reference to the parameter.
    public static void addParameter(String stageName, String parameterName, Object data,
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
    public static ArrayList<Parameter> getParametersForStage(String stageName) {
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

    public static String[] getOrderedParamNamesForStage(String stage) {
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        ArrayList<String> parameterNames = new ArrayList<>();
        for (Parameter param : stageParams) {
            parameterNames.add(param.name);
        }
        String[] array = new String[parameterNames.size()];
        parameterNames.toArray(array);
        return array;
    }

    public static Object[] getOrderedParamDataForStage(String stage) {
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        ArrayList<Object> parameterData = new ArrayList<>();
        for (Parameter param : stageParams) {
            parameterData.add(param.fancierData);
        }
        return parameterData.toArray();
    }

    public static String[] getOrderedParamTypes(String stage) {
        ArrayList<String> parameterTypes = new ArrayList<>();
        ArrayList<Parameter> stageParams = FancyJCLManager.getParametersForStage(stage);
        for (Parameter param : stageParams) {
            parameterTypes.add(param.type);
        }
        String[] array = new String[parameterTypes.size()];
        parameterTypes.toArray(array);
        return array;
    }

    public static void showDebugInfo() {
        for (Parameter param : parameters.values()) {
            param.showDebugInfo();
        }

    }

}
