package es.ull.pcg.hpc.fancyjcl_example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jraska.console.Console
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.String
import java.nio.ByteBuffer

class BenchmarkResultsActivity : AppCompatActivity() {
    private val JAVA_N_EXECUTIONS = 2
    private val JCL_N_EXECUTIONS = 15
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark_results)
        var filterIndices = intent.getIntegerArrayListExtra("filterIndexArray")
        var resolutionIndices = intent.getIntegerArrayListExtra("resolutionIndexArray")
        MainActivity.scopeBackground.launch {
            Console.clear()
            for (filterIdx in filterIndices!!) {
                val name = resources.getStringArray(R.array.filter_names)[filterIdx]
                Console.writeLine(" ------- Executing benchmarks for $name")
                Timber.d(" ------- Executing benchmarks for $name")
                for (resolutionIdx in resolutionIndices!!) {
                    val description =
                        resources.getStringArray(R.array.resolution_descriptions)[resolutionIdx]
                    Console.writeLine("   - Resolution: $description")
                    Timber.d("   - Resolution: $description")
                    val w = resources.getIntArray(R.array.resolutions_width)[resolutionIdx]
                    val h = resources.getIntArray(R.array.resolutions_height)[resolutionIdx]
                    runBenchmark(filterIdx, w, h)
                }
            }
        }
    }

    private fun runBenchmark(filterIdx: Int, w: Int, h: Int) {
        // Create buffers
        val input = ByteBuffer.allocateDirect(w * h * 4)
        val output = ByteBuffer.allocateDirect(w * h * 4)
        // Run java benchmark
        val filter = MainActivity.getFilterByIndex(filterIdx)
        val javaTime = filter.benchmarkJava(input, output, w, h, JAVA_N_EXECUTIONS)
        val javaTimeStr = String.format("%.2f", javaTime)
        Console.writeLine("         Java: $javaTimeStr milliseconds")
        Timber.d("         Java: $javaTimeStr milliseconds")
        val jclTime = filter.benchmarkFancyJCL(input, output, w, h, JCL_N_EXECUTIONS)
        val jclTimeStr = String.format("%.2f", jclTime)
        Console.writeLine("         FancyJCL: $jclTimeStr milliseconds")
        Timber.d("         FancyJCL: $jclTimeStr milliseconds")
    }
}