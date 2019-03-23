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
}