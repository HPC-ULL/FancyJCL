package es.ull.pcg.hpc.fancyjcl_example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import es.ull.pcg.hpc.fancyjcl_example.filters.Filter
import kotlinx.android.synthetic.main.tests_layout.view.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.abs


@SuppressLint("SetTextI18n")
class TestView(context: Context?) : LinearLayout(context) {
    private var selectedFilter: Filter? = null
    private val w = 810
    private val h = 456
    private val inputJcl: ByteBuffer?
    private val inputJava: ByteArray?
    private var outputJava: ByteArray?
    private var outputJcl: ByteBuffer?

    init {
        MainActivity.layoutInflater.inflate(R.layout.tests_layout, this, true)
        inputJcl = TestImage.get(w, h)
        inputJava = inputJcl.array()
        outputJcl = null
        outputJava = null
        inputImageView.setImageBitmap(TestImage.bitmap)
        javaImageView.setImageBitmap(TestImage.notComputedBitmap)
        jclImageView.setImageBitmap(TestImage.notComputedBitmap)
        jclProgressBar.visibility = View.GONE
        javaProgressBar.visibility = View.GONE
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                outputJcl = null
                outputJava = null
                errorTextView.text = ""
                selectedFilter = MainActivity.getFilterByIndex(position)
                javaImageView.setImageBitmap(TestImage.notComputedBitmap)
                jclImageView.setImageBitmap(TestImage.notComputedBitmap)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        runJavaButton.setOnClickListener {
            runJava()
        }
        runJCLButton.setOnClickListener {
            runJcl()
        }
        errorTextView.text = ""
    }

    private fun setInteraction(enable: Boolean) {
        MainActivity.scopeUI.launch {
            spinner.isEnabled = enable
            runJavaButton.isEnabled = enable
            runJCLButton.isEnabled = enable
        }
    }

    private fun showDifference() {
        var difference = 0
        if (outputJcl != null && outputJava != null) {
            outputJcl!!.rewind()
            val outputJclArray = outputJcl!!.array()
            for (i in 0 until outputJava!!.size) {
                difference += abs(outputJava!![i] - outputJclArray[i])
            }
            MainActivity.scopeUI.launch {
                errorTextView.text = "Accumulated error = $difference"
                spinner.isEnabled = true
            }
        }
    }

    fun saveImages() {
        try {
            val inputBmp = TestImage.bufferToBitmap(inputJcl, w, h)
            val javaBmp = TestImage.bufferToBitmap(ByteBuffer.wrap(outputJava), w, h)
            val jclBmp = TestImage.bufferToBitmap(outputJcl, w, h)
            FileOutputStream("/sdcard/dele/inputBmp.png").use { out ->
                inputBmp.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }
            FileOutputStream("/sdcard/dele/javaBmp.png").use { out ->
                javaBmp.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }
            FileOutputStream("/sdcard/dele/jclBmp.png").use { out ->
                jclBmp.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun runJava() {
        setInteraction(false)
        javaProgressBar.visibility = VISIBLE
        MainActivity.scopeBackground.launch {
            // Java
            outputJava = ByteArray(w * h * 4);
            val start = System.nanoTime()
            selectedFilter!!.runJavaOnce(inputJava, outputJava, w, h)
            val end = System.nanoTime()
            val elapsed: Float = (end - start).toFloat() / 1e6f
            val elapsedStr = String.format("%.2f", elapsed)
            Timber.d("Java elapsed time is %s milliseconds.", elapsedStr)
            MainActivity.scopeUI.launch {
                javaImageView.setImageBitmap(TestImage.bufferToBitmap(ByteBuffer.wrap(outputJava), w, h))
                javaProgressBar.visibility = GONE
            }
            setInteraction(true)
            showDifference()
        }
    }

    private fun runJcl() {
        setInteraction(false)
        jclProgressBar.visibility = VISIBLE
        MainActivity.scopeBackground.launch {
            // JCL
            outputJcl = ByteBuffer.allocateDirect(w * h * 4)
            try {
                val start = System.nanoTime()
                selectedFilter!!.runFancyJCLOnce(inputJcl, outputJcl, w, h)
                val end = System.nanoTime()
                val elapsed: Float = (end - start).toFloat() / 1e6f
                val elapsedStr = String.format("%.2f", elapsed)
                Timber.d("FancyJCL elapsed time is %s milliseconds.", elapsedStr)
            } catch (e: Exception) {
                Timber.e(e.message)
            }
            MainActivity.scopeUI.launch {
                jclImageView.setImageBitmap(TestImage.bufferToBitmap(outputJcl, w, h))
                jclProgressBar.visibility = GONE
            }
            setInteraction(true)
            showDifference()
        }
    }

}