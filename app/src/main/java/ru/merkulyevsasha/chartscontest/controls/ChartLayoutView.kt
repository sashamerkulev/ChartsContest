package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.chart_layout.view.*
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum
import ru.merkulyevsasha.chartscontest.models.XValuesEnum
import java.text.SimpleDateFormat
import java.util.*


class ChartLayoutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private lateinit var oldChartData: ChartData
    private lateinit var oldOnLegendClicked: OnLegendClicked
    private lateinit var oldTitle: String

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.chart_layout, this, true)
    }

    fun getChartData(): ChartData {
        return oldChartData
    }

    fun setData(newChartData: ChartData, onLegendClicked: OnLegendClicked) {
        chartTitle.text = newChartData.title
        chartCurrentPeriod.text = ""

        this.oldChartData = newChartData
        this.oldOnLegendClicked = onLegendClicked

        chartXLegend.setData(newChartData)
        chartProgress.setData(newChartData)
        chart.setData(newChartData)
        slider.setData(newChartData)
        chartLegend.setData(newChartData)

        chart.setDataChangeCallback(object : OnDataChange {
            override fun onDataChanged(
                startIndex: Int,
                stopIndex: Int,
                chartData: ChartData,
                minX: Long,
                maxX: Long,
                xScale: Float,
                yMinMaxValues: Map<Int, BaseChart.MinMaxValues>,
                yScale: Map<Int, Float>,
                chartLines: List<BaseChart.ChartLineExt>,
                yShouldVisible: Map<Int, Boolean>
            ) {
                chartLegend.onDataChanged(
                    startIndex,
                    stopIndex,
                    minX,
                    maxX,
                    xScale,
                    yMinMaxValues,
                    yScale,
                    chartLines,
                    yShouldVisible
                )
                chartXLegend.onDataChanged(
                    startIndex,
                    stopIndex,
                    minX,
                    maxX,
                    xScale,
                    yMinMaxValues,
                    yScale,
                    chartLines,
                    yShouldVisible
                )
                if (chartLines.isNotEmpty()) {
                    if (chartData.xValuesIn == XValuesEnum.X_DAYS) {
                        val startDate = chartLines[0].xDate
                        val stopDate = chartLines[chartLines.size - 1].xDate
                        val pattern = "dd MMM yyyy"
                        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                        oldTitle = "${dateFormat.format(startDate)} - ${dateFormat.format(stopDate)}"
                        chartCurrentPeriod.text = oldTitle
                    } else {
                        val startDate = chartData.xValues.first()
                        val pattern = "EEE dd MMM yyyy"
                        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                        chartCurrentPeriod.text = dateFormat.format(startDate)
                    }
                }
            }
        })

        slider.setChangeIndexesCallback(object : OnActionIndicesChange {
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

        chartLegend.setCallback(onLegendClicked)

        showCheckBoxes(newChartData)
    }

    fun show(chartData: ChartData) {
        val oldChartType = this.oldChartData.ys.first().type
        val oldChartDataStacked = this.oldChartData.stacked
        val newChartType = chartData.ys.first().type

        zoomOut.visibility = View.VISIBLE
        chartTitle.text = context.getString(R.string.zoom_out_title)
        chartTitle.setTextColor(ContextCompat.getColor(context, R.color.zoom_out))

        val oldStartIndex = chart.startIndex
        val oldStopIndex = chart.stopIndex

        if (!oldChartData.stacked && oldChartType == ChartTypeEnum.BAR && newChartType == ChartTypeEnum.LINE) {
            chartProgress.visibility = View.GONE
            slider.visibility = View.GONE
        } else {
            chartProgress.visibility = View.VISIBLE
            slider.visibility = View.VISIBLE
            chartProgress.updateData(chartData)
            slider.updateData(chartData, 0, chartData.xValues.size)
        }

        chartLegend.setCallback(null)
        chartLegend.updateData(chartData, 0, chartData.xValues.size)

        if (chartData.xValues.isNotEmpty()) {
            val startDate = chartData.xValues.first()
            val pattern = "EEE dd MMM yyyy"
            val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
            chartCurrentPeriod.text = dateFormat.format(startDate)
        }

        chart.updateData(chartData, 0, chartData.xValues.size)
        chartXLegend.updateData(chartData, 0, chartData.xValues.size)

        showCheckBoxes(chartData)

        zoomContainer.setOnClickListener {
            zoomContainer.setOnClickListener { }

            chartCurrentPeriod.text = oldTitle

            zoomOut.visibility = View.GONE
            chartTitle.text = oldChartData.title
            chartTitle.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))

            chartLegend.updateData(oldChartData, oldStartIndex, oldStopIndex)
            chartLegend.setCallback(oldOnLegendClicked)

            chartProgress.visibility = View.VISIBLE
            slider.visibility = View.VISIBLE

            chartProgress.updateData(oldChartData)

            chart.updateData(oldChartData, oldStartIndex, oldStopIndex)
            slider.updateData(oldChartData, oldStartIndex, oldStopIndex)
            chartXLegend.updateData(oldChartData, oldStartIndex, oldStopIndex)

            showCheckBoxes(oldChartData)
        }
    }

    private fun showCheckBoxes(newChartData: ChartData) {
        container.removeAllViews()
        if (newChartData.ys.first().type == ChartTypeEnum.BAR && !newChartData.stacked) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            fillCheckBoxes(newChartData)
        }
    }

    private fun fillCheckBoxes(newChartData: ChartData) {
        for (index in 0 until newChartData.ys.size) {
            val ys = newChartData.ys[index]
            val view = CheckboxView(context, ys.name, true, ys.color)
            view.setOnClickListener { _ ->
                view.setCheck(!view.checked)
                if (view.checked) {
                    chart.onYDataSwitched(index, view.checked)
                    chartProgress.onYDataSwitched(index, view.checked)
                } else {
                    if (isThereAtLeastOneChecked()) {
                        chart.onYDataSwitched(index, view.checked)
                        chartProgress.onYDataSwitched(index, view.checked)
                    } else {
                        view.setCheck(true)
                    }
                }
            }
            container.addView(view)
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