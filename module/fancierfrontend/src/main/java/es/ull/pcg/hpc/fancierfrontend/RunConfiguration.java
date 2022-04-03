package es.ull.pcg.hpc.fancierfrontend;

public class RunConfiguration {
    private final long[] dimensions;
    private final long[] parallelization;

    public RunConfiguration(long[] dimenisons, long[] parallelization) {
        this.dimensions = dimenisons;
        this.parallelization = parallelization;
    }

    public long[] getDimensions() {
        return dimensions;
    }

    public long[] getParallelization() {
        return parallelization;
    }

    public String getDimensionsAsString() {
        String out = "Dimensions (" + dimensions.length + "):";
        for (int i = 0; i < dimensions.length - 1; i++) {
            out += dimensions[i];
            out += " x ";
        }
        out += dimensions[dimensions.length - 1];
        return out;
    }

    public String getParallelizationAsString() {
        String out = "Parallelization (" + parallelization.length + "): ";
        for (int i = 0; i < parallelization.length - 1; i++) {
            out += parallelization[i];
            out += " x ";
        }
        out += parallelization[parallelization.length - 1];
        return out;
    }

}
