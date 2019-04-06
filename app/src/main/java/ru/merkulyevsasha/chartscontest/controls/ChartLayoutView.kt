package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.content.res.ColorStateList
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import kotlinx.android.synthetic.main.chart_layout.view.*
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData


class ChartLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private var parentScrollHandle: Boolean = true

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.chart_layout, this, true)
    }

    fun setData(chartData: ChartData) {
        chart.setData(chartData, object : OnDataChange {
            override fun onDataChanged(
                startIndex: Int,
                stopIndex: Int,
                minX: Long,
                minY: Long,
                maxX: Long,
                maxY: Long,
                xScale: Float,
                yScale: Float,
                chartLines: List<BaseChart.ChartLine>,
                yShouldVisible: Map<Int, Boolean>
            ) {
                chartLegend.onDataChanged(startIndex, stopIndex, minX, minY, maxX, maxY, xScale, yScale, chartLines, yShouldVisible)
                chartXLegend.onDataChanged(startIndex, stopIndex, minX, minY, maxX, maxY, xScale, yScale, chartLines, yShouldVisible)
            }
        })
        chartXLegend.setData(chartData)
        chartProgress.setData(chartData)
        slider.setData(chartData, object : OnActionIndicesChange {
            override fun onActionStartIndexChanged(startIndex: Int) {
                chart.onStartIndexChanged(startIndex)
            }

            override fun onActionStopIndexChanged(stopIndex: Int) {
                chart.onStopIndexChanged(stopIndex)
            }

            override fun onActionIndicesChanged(startIndex: Int, stopIndex: Int) {
                chart.onIndexesChanged(startIndex, stopIndex)
            }
        })
        container.removeAllViews()
        for (index in 0 until chartData.ys.size) {
            val ys = chartData.ys[index]
            val view = CheckBox(context)
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
                } else {
                    if (isThereAtLeastOneChecked()) {
                        chart.onYDataSwitched(index, isChecked)
                    } else {
                        view.isChecked = true
                        var startIndex = chart.startIndex - 5
                        if (startIndex < 0) startIndex = chartData.xValuesInDays.size / 3
                        chart.onStartIndexChanged(startIndex)
                    }
                }
            }
            container.addView(view)
            invalidate()
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


}