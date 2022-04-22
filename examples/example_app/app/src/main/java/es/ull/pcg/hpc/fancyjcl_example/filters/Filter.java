package es.ull.pcg.hpc.fancyjcl_example.filters;

import java.nio.ByteBuffer;

public abstract class Filter {
    abstract public void benchmarkJava();
    abstract public void benchmarkFancyJCL();
    abstract public void runJavaOnce(ByteBuffer input, ByteBuffer output, int w, int h);
    abstract public void runFancyJCLOnce(ByteBuffer input, ByteBuffer output, int w, int h) throws Exception;
}
