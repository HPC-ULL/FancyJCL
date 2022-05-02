package es.ull.pcg.hpc.fancyjcl_example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jraska.console.Console
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.String
import java.nio.ByteBuffer

class BenchmarkResultsActivity : AppCompatActivity() {
    private var file: File
    private val JAVA_N_EXECUTIONS = 3
    private val JCL_N_EXECUTIONS = 15

    init {
        val path = MainActivity.ctx.getExternalFilesDir(null)
        file = File(path, "benchmarks.csv")
        file.delete()
        file.createNewFile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark_results)
        var filterIndices = intent.getIntegerArrayListExtra("filterIndexArray")
        var resolutionIndices = intent.getIntegerArrayListExtra("resolutionIndexArray")
        MainActivity.scopeBackground.launch {
            Console.clear()
            writeCSVHeader()
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
                    val resolutionName =
                        resources.getStringArray(R.array.resolution_names)[resolutionIdx]
                    file.appendText("$name,$resolutionName,")
                    runBenchmark(filterIdx, w, h)
                    file.appendText("\n")
                }
            }
        }
    }

    private fun writeCSVHeader() {
        val header = "Filter,Resolution,Java,FancyJCL"
        file.appendText(header)
        file.appendText("\n")
    }

    private fun runBenchmark(filterIdx: Int, w: Int, h: Int) {
        // Create buffers and arrays
        val inputJCL = ByteBuffer.allocateDirect(w * h * 4)
        val outputJCL = ByteBuffer.allocateDirect(w * h * 4)
        val inputJava = ByteArray(w * h * 4)
        val outputJava = ByteArray(w * h * 4)
        // Run java benchmark
        val filter = MainActivity.getFilterByIndex(filterIdx)
        val javaTime = filter.benchmarkJava(inputJava, outputJava, w, h, JAVA_N_EXECUTIONS)
        val javaTimeStr = String.format("%.2f", javaTime)
        file.appendText("$javaTimeStr,")
        Console.writeLine("         Java: $javaTimeStr milliseconds")
        Timber.d("         Java: $javaTimeStr milliseconds")
        val jclTime = filter.benchmarkFancyJCL(inputJCL, outputJCL, w, h, JCL_N_EXECUTIONS)
        val jclTimeStr = String.format("%.2f", jclTime)
        file.appendText(jclTimeStr)
        Console.writeLine("         FancyJCL: $jclTimeStr milliseconds")
        Timber.d("         FancyJCL: $jclTimeStr milliseconds")
    }
}