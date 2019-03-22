package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private var heightRow: Float = 0f
    private var widthColumn: Float = 0f

    private var startDate: Date = Date()
    private var stepInDays: Long = 0

    private val pattern = "dd MMM"
    private val dateFormat: DateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private val paintTextInfos = mutableListOf<PaintTextInfo>()
    private var edgeTextWidth: Float = 0f
    private var heightTextPadding: Int = 20

    fun onIndexesChanged(startIndex: Int, stopIndex: Int) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex

        startDate = chartData.xValues[startIndex]
        val startDay = chartData.xValuesInDays[startIndex]
        val stopDay = chartData.xValuesInDays[stopIndex - 1]

        stepInDays = (stopDay - startDay) / COLUMNS

        paintTextInfos.clear()
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        for (column in 0 until COLUMNS) {
            if (column > 0) calendar.add(Calendar.DAY_OF_YEAR, stepInDays.toInt())
            val text = dateFormat.format(calendar.time)
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            paintTextInfos.add(PaintTextInfo(text, bounds))
            if (column == 0 || column == COLUMNS - 1) {
                edgeTextWidth += bounds.width()
            }
        }
        invalidate()
    }

    fun onYDataSwitched(name: String, isChecked: Boolean) {
        draw[name] = isChecked
        invalidate()
    }

    override fun onMeasureEnd() {
        baseHeight -= 80

        yScale = baseHeight / (maxY - minY).toFloat()
        heightRow = baseHeight / ROWS.toFloat()
        widthColumn = baseWidth / COLUMNS.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        chartLines.clear()
        chartLines.addAll(getChartLines2())

        canvas?.apply {

            maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
            minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
            xScale = baseWidth / (maxX - minX).toFloat()

            drawYWithLegend(this)
            drawXWithLegend(this)

            super.onDraw(canvas)
        }
    }

    private fun drawXWithLegend(canvas: Canvas) {
        for (column in 0 until COLUMNS) {
            when (column) {
                COLUMNS - 1 -> canvas.drawText(
                    paintTextInfos[column].text,
                    baseWidth - paintTextInfos[column].bound.width(),
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
                0 -> canvas.drawText(
                    paintTextInfos[column].text,
                    widthColumn * column,
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
                else -> canvas.drawText(
                    paintTextInfos[column].text,
                    widthColumn * column + (widthColumn / 2 - paintTextInfos[column].bound.width() / 2),
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
            }
        }
    }

    private fun drawYWithLegend(canvas: Canvas) {
        val step = (maxY - minY) / ROWS
        var yText = step
        for (row in 0 until ROWS) {
            val yRow = baseHeight - heightRow * row
            canvas.drawLine(
                0f,
                yRow,
                width.toFloat(),
                yRow,
                paintTopBottomBorder
            )

            canvas.drawText(reduction(yText + minY), 0f, yRow - 20, textPaint)
            yText += step
        }
    }

    private fun reduction(value: Long): String {
        var reductionValue = value
        if (value > 10000) {
            reductionValue -= value % 1000
            return reductionValue.toString()
        }
        if (value > 1000) {
            reductionValue -= value % 100
            return reductionValue.toString()
        }
        if (value > 100) {
            reductionValue -= value % 10
            return reductionValue.toString()
        }
        return reductionValue.toString()
    }

    data class PaintTextInfo(val text: String, val bound: Rect)


}