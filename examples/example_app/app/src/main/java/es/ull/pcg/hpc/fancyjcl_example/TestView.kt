package es.ull.pcg.hpc.fancyjcl_example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import es.ull.pcg.hpc.fancyjcl_example.filters.*
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

    init {
        MainActivity.layoutInflater.inflate(R.layout.tests_layout, this, true)
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
                if (position == 0) {
                    selectedFilter = Bilateral()
                } else if (position == 1) {
                    selectedFilter = Convolution5x5()
                } else if (position == 1) {
                    selectedFilter = GrayScale()
                } else if (position == 1) {
                    selectedFilter = Convolution3x3()
                } else if (position == 2) {
                    selectedFilter = GaussianBlur()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        runButton.setOnClickListener {
            spinner.isEnabled = false
            jclProgressBar.visibility = View.VISIBLE
            javaProgressBar.visibility = View.VISIBLE
            MainActivity.scopeBackground.launch {
                val w = 810
                val h = 456
                val input = TestImage.get(w, h)
                // JCL
                val outputJcl = ByteBuffer.allocateDirect(w * h * 4)
                try {
                    selectedFilter!!.runFancyJCLOnce(input, outputJcl, w, h)
                } catch (e: Exception) {
                    Timber.e(e.message)
                }
                MainActivity.scopeUI.launch {
                    jclImageView.setImageBitmap(TestImage.bufferToBitmap(outputJcl, w, h))
                    jclProgressBar.visibility = GONE
                }
                // Java
                val outputJava = ByteBuffer.allocateDirect(w * h * 4)
                selectedFilter!!.runJavaOnce(input, outputJava, w, h)
                MainActivity.scopeUI.launch {
                    javaImageView.setImageBitmap(TestImage.bufferToBitmap(outputJava, w, h))
                    javaProgressBar.visibility = GONE
                }
                // Check outputs difference
                var difference = 0
                for (i in 0 until outputJcl.capacity()) {
                    difference += abs(outputJava[i] - outputJcl[i])
                }
                MainActivity.scopeUI.launch {
                    errorTextView.text = "Accumulated error = $difference"
                    spinner.isEnabled = true
                    try {
                        val inputBmp = TestImage.bufferToBitmap(input, w, h)
                        val javaBmp = TestImage.bufferToBitmap(outputJava, w, h)
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

            }
        }
    }

}