package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.chart_layout.view.*
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData


class ChartLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

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
                chartLegend.onDataChanged(
                    startIndex,
                    stopIndex,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    xScale,
                    yScale,
                    chartLines,
                    yShouldVisible
                )
                chartXLegend.onDataChanged(
                    startIndex,
                    stopIndex,
                    minX,
                    minY,
                    maxX,
                    maxY,
                    xScale,
                    yScale,
                    chartLines,
                    yShouldVisible
                )
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
            val view = CheckboxView(context, ys.name, true, ys.color)
            view.setOnClickListener { _ ->
                if (!view.checked) {
                    view.setCheck(true)
                    chart.onYDataSwitched(index, view.checked)
                } else {
                    if (isThereAtLeastOneChecked()) {
                        view.setCheck(false)
                        chart.onYDataSwitched(index, view.checked)
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
            val view = container.getChildAt(index) as CheckboxView
            result = result || view.checked
        }
        return result
    }


}