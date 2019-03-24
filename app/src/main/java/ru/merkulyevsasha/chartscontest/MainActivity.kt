package ru.merkulyevsasha.chartscontest

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.app.AppCompatActivity
import android.widget.CheckBox
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import ru.merkulyevsasha.chartscontest.controls.OnActionIndicesChange
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.sources.Example
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity(), IMainView {

    private val pres = MainPresenter(SourceDataConverter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            val source = readSource()
            val root = convertToObject(source)
            pres.onBind(this)
            pres.dealWithIt(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        pres.onBind(this)
    }

    override fun onPause() {
        pres.onUnbind()
        super.onPause()
    }

    override fun showCharts(chartData: ChartData) {
        chart.setData(chartData)
        chartLegend.setData(chartData)
        chartProgress.setData(chartData)
        slider.setData(chartData, object : OnActionIndicesChange {
            override fun onActionStartIndexChanged(startIndex: Int) {
                runOnUiThread {
                    chart.onStartIndexChanged(startIndex)
                    chartLegend.onStartIndexChanged(startIndex)
                }
            }

            override fun onActionStopIndexChanged(stopIndex: Int) {
            }

            override fun onActionIndicesChanged(startIndex: Int, stopIndex: Int) {
                runOnUiThread {
                    chart.onIndexesChanged(startIndex, stopIndex)
                    chartLegend.onIndexesChanged(startIndex, stopIndex)
                }
            }
        })
        container.removeAllViews()
        for (index in 0 until chartData.ys.size) {
            val ys = chartData.ys[index]
            val view = CheckBox(this)
            view.text = ys.name
            view.isChecked = true
            val colors = intArrayOf(
                ys.color,
                ys.color,
                ys.color,
                ys.color
            )
            val states = arrayOf(
                intArrayOf(android.R.attr.state_enabled), // enabled
                intArrayOf(-android.R.attr.state_enabled), // disabled
                intArrayOf(-android.R.attr.state_checked), // unchecked
                intArrayOf(android.R.attr.state_pressed)  // pressed
            )
            CompoundButtonCompat.setButtonTintList(view, ColorStateList(states, colors))
            view.setTextColor(ys.color)
            view.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chart.onYDataSwitched(index, isChecked)
                    chartLegend.onYDataSwitched(index, isChecked)
                } else {
                    if (isThereAtLeastOneChecked()) {
                        chart.onYDataSwitched(index, isChecked)
                        chartLegend.onYDataSwitched(index, isChecked)
                    } else {
                        view.isChecked = true
                    }
                }
            }
            container.addView(view)
        }
    }

    private fun isThereAtLeastOneChecked(): Boolean {
        var result = false
        for (index in 0 until container.childCount) {
            val view = container.getChildAt(index) as CheckBox
            result = result || view.isChecked
        }
        return result
    }

    private fun readSource(): String {
        val stream = assets.open("chart_data.json")
        val text = StringBuilder()
        val inputReader = InputStreamReader(stream)
        val buffReader = BufferedReader(inputReader)
        val line = buffReader.readLine()
        if (line != null) {
            text.append(line)
        }
        return text.toString()
    }

    private fun convertToObject(json: String): List<Example> {
        val gson = Gson()
        val listType = object : TypeToken<List<Example>>() {}.type
        val data: List<Example> = gson.fromJson(json, listType)
        return data
    }

}
