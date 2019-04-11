package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.models.ChartData


class ChartProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
        invalidate()
    }

    override fun onMeasureEnd() {
        for (index in 0 until yMinMaxValues.size) {
            val yScale = baseHeight / (yMinMaxValues[index].max - yMinMaxValues[index].min).toFloat()
            yScales.add(yScale)
        }
        xScale = baseWidth / (maxX - minX).toFloat()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(startIndex, stopIndex, minX, maxX, yMinMaxValues))
    }

    fun onYDataSwitched(index: Int, checked: Boolean) {
        yShouldVisible[index] = checked

        if (chartData.yScaled) {
            chartLines.clear()
            chartLines.addAll(getChartLinesExt(startIndex, stopIndex, minX, maxX, yMinMaxValues))
        } else {
            val minY = getMinYAccordingToVisibility(0, chartData.xValues.size - 1)
            val maxY = getMaxYAccordingToVisibility(0, chartData.xValues.size - 1)
            val yScale = baseHeight / (maxY - minY).toFloat()
            yMinMaxValues.clear()
            yMinMaxValues.add(MinMaxValues(minY, maxY))
            yScales.clear()
            yScales.add(yScale)
            chartLines.clear()
            chartLines.addAll(getChartLinesExt(startIndex, stopIndex, minX, maxX, yMinMaxValues))
        }

        for (indexLine in 0 until chartLines.size) {
            val chartLine = chartLines[indexLine]
            if (yShouldVisible[chartLine.yIndex]!!) {
                chartLine.paint.alpha = 100
            } else {
                chartLine.paint.alpha = 0
            }
        }

        invalidate()
    }
}