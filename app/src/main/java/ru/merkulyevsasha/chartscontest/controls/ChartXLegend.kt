package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.models.ChartData
import java.text.SimpleDateFormat
import java.util.*


class ChartXLegend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private val pattern = "dd MMM"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private val coordDates = mutableListOf<CoordDate>()
    private var yText: Float = 0f
    private val calendar = Calendar.getInstance()

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawRect(0f, 0f, baseWidth, baseHeight, paintLeftRightBorder)

            if (yText == 0f) {
                yText = baseHeight / 2 + coordDates[0].bound.height() / 2
            }

            for (index in 0 until coordDates.size) {
                drawText(
                    coordDates[index].date,
                    coordDates[index].x,
                    yText,
                    textPaint
                )
            }
        }
    }

    fun onDataChanged(
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
        this.minX = minX
        this.minY = minY
        this.maxX = maxX
        this.maxY = maxY
        this.xScale = xScale
        this.yScale = yScale
        this.chartLines.clear()
        this.chartLines.addAll(chartLines)
        this.yShouldVisible.clear()
        this.yShouldVisible.putAll(yShouldVisible)

        coordDates.clear()
        coordDates.addAll(getCoordDates())
        invalidate()
    }

    private fun getCoordDates(): List<CoordDate> {
        val result = mutableListOf<CoordDate>()
        val startDate = chartData.xValues[startIndex]
        val stopDate = chartData.xValues[stopIndex - 1]
        val startDay = chartData.xValuesInDays[startIndex]
        val stopDay = chartData.xValuesInDays[stopIndex - 1]
        val stepInDays = (stopDay - startDay) / COLUMNS
        for (column in 0 until COLUMNS) {
            if (column == 0) calendar.time = startDate
            else if (column == COLUMNS - 1) calendar.time = stopDate
            else calendar.time = chartData.xValues[startIndex + column * stepInDays.toInt()]
            val text = dateFormat.format(calendar.time)

            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)

            val x = if (column == 0) 0f
            else if (column == COLUMNS - 1) baseWidth - bounds.width()
            else (chartData.xValuesInDays[startIndex + column * stepInDays.toInt()] - minX) * xScale

            result.add(CoordDate(x, text, bounds))
        }
        return result
    }


    data class CoordDate(val x: Float, val date: String, val bound: Rect)

}