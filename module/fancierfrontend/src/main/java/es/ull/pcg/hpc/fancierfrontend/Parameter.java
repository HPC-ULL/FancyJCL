package es.ull.pcg.hpc.fancierfrontend;

import java.util.Objects;

enum ParameterClass {
    INPUT,
    INPUTOUTPUT,
    OUTPUT
}

public class Parameter {
    ParameterClass parameterClass;
    Object data;
    String type;
    int index;

    public Parameter(ParameterClass parameterClass, Object data, String type, int index) {
        this.parameterClass = parameterClass;
        this.data = data;
        this.type = type;
        this.index = index;
    }

}
