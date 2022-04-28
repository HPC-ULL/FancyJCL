package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import es.ull.pcg.hpc.fancyjcl.Benchmark;
import es.ull.pcg.hpc.fancyjcl.FancyJCLManager;
import es.ull.pcg.hpc.fancyjcl.Stage;

public abstract class Filter {
    protected final ArrayList<Stage> jclStages = new ArrayList<>();

    public float benchmarkJava(byte [] input, byte [] output, int w, int h, int nExecutions)
            throws Exception {
        return Benchmark.perform(() -> runJavaOnce(input, output, w, h), () -> {
        }, nExecutions);
    }

    public float benchmarkFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h,
                                   int nExecutions) throws Exception {
        initFancyJCL(input, output, w, h);
        float time = Benchmark.perform(() -> {
            for (int i = 0; i < jclStages.size(); i++) {
                jclStages.get(i).run();
            }
        }, () -> jclStages.get(jclStages.size() - 1).syncOutputsToCPU(), nExecutions);
        jclStages.clear();
        FancyJCLManager.clear();
        return time;
    }

    public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h)
            throws Exception {
        initFancyJCL(input, output, w, h);
        for (int i = 0; i < jclStages.size() - 1; i++) {
            jclStages.get(i).run();
        }
        jclStages.get(jclStages.size() - 1).runSync();
        jclStages.clear();
        FancyJCLManager.clear();
    }

    abstract public void runJavaOnce(byte [] input, byte [] output, int w, int h);

    abstract void initFancyJCL(ByteBuffer input, ByteBuffer output, int w, int h) throws Exception;
}
