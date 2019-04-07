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

    private var yText: Float = 0f
    private val calendar = Calendar.getInstance()

    private var stepDate: Int = 6
    private val coordDates = mutableListOf<CoordDate>()

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
        stepDate = if (chartData.xValues.size > 200) 20 else 6
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
//            drawRect(0f, 0f, baseWidth, baseHeight, paintLeftRightBorder)

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
        this.startIndex = startIndex
        this.stopIndex = stopIndex
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
        coordDates.addAll(getCoordDates(stepDate))
        invalidate()
    }

    private fun getCoordDates(step: Int): List<CoordDate> {
        val result = mutableListOf<CoordDate>()
        for (index in 0 until stopIndex step step) {
            calendar.time = chartData.xValues[index]
            val text = dateFormat.format(calendar.time)

            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)

            val x = (chartData.xValuesInDays[index] - minX) * xScale - bounds.width() / 2

            result.add(CoordDate(x, text, bounds))
        }
        return result
    }

    data class CoordDate(val x: Float, val date: String, val bound: Rect)
}