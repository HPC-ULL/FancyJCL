package es.ull.pcg.hpc.fancyjcl_example

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView


class TabAdapter(arrayList: ArrayList<Any>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val testsTypeId = 0
    private val benchmarksTypeId = 1
    private var items = ArrayList<Any>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == testsTypeId) {
            val view: View = TestView(MainActivity.ctx)
            view.layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            TestsViewHolder(view)
        } else {
            val view: View =
                MainActivity.layoutInflater.inflate(R.layout.benchmarks_layout, parent, false)
            BenchmarksViewHolder(view)
        }
    }

    class Tests
    class Benchmarks

    override fun getItemViewType(position: Int): Int {
        if (items[position] is Tests) {
            return testsTypeId
        } else if (items[position] is Benchmarks) {
            return benchmarksTypeId
        }
        return -1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int {
        return items.size
    }


    inner class BenchmarksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    inner class TestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    init {
        this.items = arrayList
    }
}