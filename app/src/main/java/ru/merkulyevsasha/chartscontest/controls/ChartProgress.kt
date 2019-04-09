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
        yScale = baseHeight / (maxY - minY).toFloat()
        xScale = baseWidth / (maxX - minX).toFloat()
        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
    }

    fun onYDataSwitched(index: Int, checked: Boolean) {
        yShouldVisible[index] = checked

        minY = getMinYAccordingToVisibility(0, chartData.xValues.size - 1)
        maxY = getMaxYAccordingToVisibility(0, chartData.xValues.size - 1)
        yScale = baseHeight / (maxY - minY).toFloat()

        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))

        for (indexLine in 0 until chartLines.size) {
            val chartLine = chartLines[indexLine]
            if (yShouldVisible[chartLine.index]!!) {
                chartLine.paint.alpha = 100
            } else {
                chartLine.paint.alpha = 0
            }
        }

        invalidate()
    }
}