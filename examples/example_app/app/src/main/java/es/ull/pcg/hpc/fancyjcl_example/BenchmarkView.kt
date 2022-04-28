package es.ull.pcg.hpc.fancyjcl_example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.benchmark_item_category.view.*
import kotlinx.android.synthetic.main.benchmark_switch_item.view.*
import kotlinx.android.synthetic.main.benchmarks_layout.view.*


class BenchmarkView(context: Context?) : LinearLayout(context) {
    private var filtersItemsList = mutableSetOf<FilterItemSwitchView>()
    private var resolutionItemsList = mutableSetOf<ResolutionItemSwitchView>()

    init {
        MainActivity.layoutInflater.inflate(R.layout.benchmarks_layout, this, true)
        val filtersCategoryView = CategoryView("Filters", context)
        itemsLayout.addView(filtersCategoryView)
        for (idx in resources.getStringArray(R.array.filter_names).indices) {
            val view = FilterItemSwitchView(idx, context)
            itemsLayout.addView(view)
            filtersItemsList.add(view)
        }
        val resolutionsCategoryView = CategoryView("Resolutions", context)
        itemsLayout.addView(resolutionsCategoryView)
        for (idx in resources.getStringArray(R.array.resolution_descriptions).indices) {
            val view = ResolutionItemSwitchView(idx, context)
            itemsLayout.addView(view)
            resolutionItemsList.add(view)
        }
        clearButton.setOnClickListener {
            for (item in filtersItemsList) {
                item.switchView.isChecked = false
            }
            for (item in resolutionItemsList) {
                item.switchView.isChecked = false
            }
        }
        selectAllButton.setOnClickListener {
            selectAll()
        }
        selectAll()
        runButton.setOnClickListener {
            val filterIndexArray: ArrayList<Int> = ArrayList()
            val resolutionIndexArray: ArrayList<Int> = ArrayList()

            for (filter in filtersItemsList) {
                if (filter.switchView.isChecked) {
                    filterIndexArray.add(filter.index)
                }
            }
            for (resolution in resolutionItemsList) {
                if (resolution.switchView.isChecked) {
                    resolutionIndexArray.add(resolution.index)
                }
            }
            val intent = Intent(MainActivity.ctx, BenchmarkResultsActivity::class.java)
            intent.putExtra("filterIndexArray", filterIndexArray)
            intent.putExtra("resolutionIndexArray", resolutionIndexArray)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            MainActivity.ctx.startActivity(intent)
        }
    }

    private fun selectAll() {
        for (item in filtersItemsList) {
            item.switchView.isChecked = true
        }
        for (item in resolutionItemsList) {
            item.switchView.isChecked = true
        }
    }

    inner class FilterItemSwitchView(index: Int, context: Context?) :
        LinearLayout(context) {
        var index: Int

        init {
            MainActivity.layoutInflater.inflate(R.layout.benchmark_switch_item, this, true)
            val name = resources.getStringArray(R.array.filter_names)[index]
            switchView.text = name
            this.index = index
        }
    }

    inner class ResolutionItemSwitchView(index: Int, context: Context?) :
        LinearLayout(context) {
        var index: Int

        init {
            MainActivity.layoutInflater.inflate(R.layout.benchmark_switch_item, this, true)
            val name = resources.getStringArray(R.array.resolution_descriptions)[index]
            switchView.text = name
            this.index = index
        }
    }

    inner class CategoryView(name: String, context: Context?) : LinearLayout(context) {
        init {
            MainActivity.layoutInflater.inflate(R.layout.benchmark_item_category, this, true)
            categoryTextView.text = name
        }
    }

}