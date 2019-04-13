package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.XValuesEnum
import java.text.SimpleDateFormat
import java.util.*


class ChartXLegend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private val dayPattern = "dd MMM"
    private val dayFormat: SimpleDateFormat = SimpleDateFormat(dayPattern, Locale.getDefault())

    private val hourPattern = "HH:mm"
    private val hourFormat: SimpleDateFormat = SimpleDateFormat(hourPattern, Locale.getDefault())

    private var yText: Float = 0f
    private val calendar = Calendar.getInstance()

    private val coordDates = mutableListOf<CoordDate>()
    private val textPaint: Paint

    init {
        val metrics = resources.displayMetrics
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.strokeWidth = TEXT_STROKE_WIDTH
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.color = ContextCompat.getColor(getContext(), R.color.legend_xy)
        textPaint.textSize = TEXT_SIZE_DP * metrics.density
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            if (yText == 0f && coordDates.size > 0) {
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
        maxX: Long,
        xScale: Float,
        yMinMaxValues: Map<Int, BaseChart.MinMaxValues>,
        yScale: Map<Int, Float>,
        chartLines: List<BaseChart.ChartLineExt>,
        yShouldVisible: Map<Int, Boolean>
    ) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        this.minX = minX
        //this.minY = minY
        this.maxX = maxX
        //this.maxY = maxY
        this.xScale = xScale
        //this.yScale = yScale
        this.chartLines.clear()
        this.chartLines.addAll(chartLines)
        this.yShouldVisible.clear()
        this.yShouldVisible.putAll(yShouldVisible)

        val startDay = chartData.xValuesIn()[startIndex]
        val stopDay = chartData.xValuesIn()[stopIndex - 1]
        val parts = 5
        var stepInDays = ((stopDay - startDay) / parts).toInt()
        if (stepInDays < 1) stepInDays = 1

        coordDates.clear()
        coordDates.addAll(getCoordDates(stepInDays))
        invalidate()
    }

    private fun getCoordDates(step: Int): List<CoordDate> {
        val result = mutableListOf<CoordDate>()
        for (index in 0 until chartData.xValues.size step step) {
            calendar.time = chartData.xValues[index]
            val text = if (chartData.xValuesIn == XValuesEnum.X_DAYS) dayFormat.format(calendar.time) else hourFormat.format(calendar.time)

            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)

            val x = (chartData.xValuesIn()[index] - minX) * xScale - bounds.width() / 2

            result.add(CoordDate(x, text, bounds))
        }
        return result
    }

    fun updateData(chartData: ChartData, startIndex: Int, stopIndex: Int) {
        setData(chartData)
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        maxX = chartData.xValuesIn().subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesIn().subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        calculateYScales()

        var stepInDays: Int = 5
        if (chartData.xValuesIn == XValuesEnum.X_MINUTES) {
            stepInDays = 45
        } else {
            val startDay = chartData.xValuesIn()[startIndex]
            val stopDay = chartData.xValuesIn()[stopIndex - 1]
            val parts = 5
            stepInDays = ((stopDay - startDay) / parts).toInt()
            if (stepInDays < 1) stepInDays = 1
        }
        coordDates.clear()
        coordDates.addAll(getCoordDates(stepInDays))

        invalidate()
    }

    data class CoordDate(val x: Float, val date: String, val bound: Rect)
}