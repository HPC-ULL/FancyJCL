package es.ull.pcg.hpc.fancyjcl_example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import es.ull.pcg.hpc.fancyjcl_example.filters.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newSingleThreadContext
import timber.log.Timber
import timber.log.Timber.Forest.plant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            plant(LinkingTree())
        }
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val alertDialog: android.app.AlertDialog? =
                    android.app.AlertDialog.Builder(this).create()
                alertDialog!!.setTitle("Camera Permission")
                alertDialog.setMessage("Camera Permission is not granted")
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.show()
            }
        } else {
            initialize()
        }
    }

    private fun initialize() {
        setContentView(R.layout.activity_main)
        ctx = applicationContext
        MainActivity.layoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        instance = this
        scopeBackground = CoroutineScope(newSingleThreadContext("background_jobs"))
        scopeUI = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val arrayList: ArrayList<Any> = ArrayList()
        arrayList.add(TabAdapter.Tests())
        arrayList.add(TabAdapter.Benchmarks())
        view_pager.adapter = TabAdapter(arrayList)

        TabLayoutMediator(
            tab_layout, view_pager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = if (position == 0) "Tests" else "Benchmarks"
        }.attach()
    }

    companion object {
        lateinit var ctx: Context
        lateinit var layoutInflater: LayoutInflater
        lateinit var instance: MainActivity
        lateinit var scopeBackground: CoroutineScope
        lateinit var scopeUI: CoroutineScope
        fun getFilterByIndex(index: Int): Filter {
            return when (index) {
                0 -> Bilateral()
                1 -> Median()
                2 -> Posterize()
                3 -> Levels()
                4 -> Fisheye()
                5 -> Contrast()
                6 -> Convolution5x5()
                7 -> GrayScale()
                8 -> Convolution3x3()
                9 -> GaussianBlur()
                else -> {
                    throw Exception("No filter for index $index")
                }
            }
        }
    }
}