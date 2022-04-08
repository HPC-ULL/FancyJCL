package es.ull.pcg.hpc.fancyjcl;

/**
 * Defines how a Stage is executed. It controls the size of each dimension of the execution, its
 * parallelization factor per dimension and vectorization factor per dimension (not implemented
 * yet).
 */
public class RunConfiguration {
    private final long[] dimensions;
    private final long[] parallelization;

    /**
     * Instantiates a new Run configuration. It defines how the {@code Stage} will be executed in
     * terms of execution size, parallelization size and vectorization size (not implemented yet).
     *
     * @param dimensions      The size of the dimensions of the execution. It doesn't need to
     *                        coincide with the size of the output parameters.
     * @param parallelization The parallelization factor per dimension.
     */
    public RunConfiguration(long[] dimensions, long[] parallelization) {
        this.dimensions = dimensions;
        this.parallelization = parallelization;
    }

    long[] getDimensions() {
        return dimensions;
    }

    long[] getParallelization() {
        return parallelization;
    }

    String getDimensionsAsString() {
        String out = "Dimensions (" + dimensions.length + "):";
        for (int i = 0; i < dimensions.length - 1; i++) {
            out += dimensions[i];
            out += " x ";
        }
        out += dimensions[dimensions.length - 1];
        return out;
    }

    String getParallelizationAsString() {
        String out = "Parallelization (" + parallelization.length + "): ";
        for (int i = 0; i < parallelization.length - 1; i++) {
            out += parallelization[i];
            out += " x ";
        }
        out += parallelization[parallelization.length - 1];
        return out;
    }

}
