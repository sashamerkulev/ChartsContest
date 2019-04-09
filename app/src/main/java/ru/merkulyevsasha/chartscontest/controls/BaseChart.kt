package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.YValue
import java.util.*

open class BaseChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    internal var baseWidth: Float = 0f
    internal var baseHeight: Float = 0f
    internal var xScale: Float = 1f
    internal var yScale: Float = 1f
    internal var maxY: Long = 0
    internal var minY: Long = 0
    internal var maxX: Long = 0
    internal var minX: Long = 0
    internal var startIndex: Int = 0
    internal var stopIndex: Int = 0

    internal lateinit var chartData: ChartData
    internal val paints = mutableMapOf<String, Paint>()
    internal val yShouldVisible = mutableMapOf<Int, Boolean>()
    internal val chartLines = mutableListOf<ChartLineExt>()

    open fun setData(chartData: ChartData) {
        this.chartData = chartData
        minX = chartData.getMinInDays()
        maxX = chartData.getMaxInDays()
        minY = chartData.getMinYs()
        maxY = chartData.getMaxYs()

        startIndex = 0
        stopIndex = chartData.xValuesInDays.size

        paints.clear()
        chartData.ys.forEachIndexed { index, ys ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.color = ys.color
            paint.strokeWidth = CHART_STOKE_WIDTH
            paints.put(ys.name, paint)
            yShouldVisible.put(index, true)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 100
        val desiredHeight = 100

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        //Measure Width
        if (widthMode == View.MeasureSpec.EXACTLY) {
            //Must be this size
            baseWidth = widthSize.toFloat()
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            baseWidth = Math.min(desiredWidth, widthSize).toFloat()
        } else {
            //Be whatever you want
            baseWidth = desiredWidth.toFloat()
        }

        //Measure Height
        if (heightMode == View.MeasureSpec.EXACTLY) {
            //Must be this size
            baseHeight = heightSize.toFloat()
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            baseHeight = Math.min(desiredHeight, heightSize).toFloat()
        } else {
            //Be whatever you want
            baseHeight = desiredHeight.toFloat()
        }

        //MUST CALL THIS
        setMeasuredDimension(baseWidth.toInt(), baseHeight.toInt())

        if (::chartData.isInitialized) onMeasureEnd()
    }

    open fun onMeasureEnd() {

    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            for (index in 0 until chartLines.size) {
                val chartLine = chartLines[index]
                when (chartLine.type) {
                    "line" -> {
                        if (chartLine.xIndex > 0 && yShouldVisible[chartLine.yIndex] == true) {
                            val prev = chartLines.subList(0, index)
                                .filter { it.xIndex == chartLine.xIndex - 1 && it.yIndex == chartLine.yIndex }
                            if (prev.isNotEmpty()) {
                                val x1 = prev.last().x
                                val y1 = prev.last().y
                                drawLine(x1, y1, chartLine.x, chartLine.y, chartLine.paint)
                            }
                        }
                    }
                    "bar" -> {
                        if (yShouldVisible[chartLine.yIndex] == true) {
                            drawRect(
                                chartLine.x - BAR_SIZE / 2,
                                chartLine.y,
                                chartLine.x + BAR_SIZE / 2,
                                baseHeight,
                                chartLine.paint
                            )
                        }
                    }
                    "area" -> {

                    }
                }
            }
        }
    }

    internal fun isInitialized(): Boolean {
        return ::chartData.isInitialized
    }

    internal fun getChartLinesExt(
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        minY: Long,
        maxY: Long
    ): List<ChartLineExt> {
        val yScale = baseHeight / (maxY - minY).toFloat()
        val xScale = baseWidth / (maxX - minX).toFloat()
        val result = mutableListOf<ChartLineExt>()

        for (xIndex in startIndex until stopIndex) {
            val xDays = chartData.xValuesInDays[xIndex]
            val xDate = chartData.xValues[xIndex]
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]

                val x1 = (xDays - minX) * xScale
                val y1 = baseHeight - (yValue.yValues[xIndex] - minY) * yScale

                val chartPaint = paints[chartData.ys[yIndex].name]!!
                val chartType = yValue.type

                result.add(
                    ChartLineExt(
                        xIndex,
                        yIndex,
                        xDate,
                        xDays,
                        yValue.yValues[xIndex],
                        x1,
                        y1,
                        chartPaint,
                        chartType,
                        chartData.ys
                    )
                )
            }
        }

        return result
    }

    internal fun getMinYAccordingToVisibility(startIndex: Int, stopIndex: Int): Long {
        var result: Long = Long.MAX_VALUE
        for (index in 0 until chartData.ys.size) {
            if (!yShouldVisible[index]!!) continue
            result = Math.min(result, chartData.ys[index].yValues.subList(startIndex, stopIndex).min()!!)
        }
        return result
    }

    internal fun getMaxYAccordingToVisibility(startIndex: Int, stopIndex: Int): Long {
        var result: Long = Long.MIN_VALUE
        for (index in 0 until chartData.ys.size) {
            if (!yShouldVisible[index]!!) continue
            result = Math.max(result, chartData.ys[index].yValues.subList(startIndex, stopIndex).max()!!)
        }
        return result
    }

    data class ChartLineExt(
        val xIndex: Int,
        val yIndex: Int,
        val xDate: Date,
        val xDays: Long,
        val yValue: Long,
        val x: Float,
        val y: Float,
        val paint: Paint,
        val type: String,
        val ys: List<YValue>
    )

    companion object {
        const val ROWS = 6
        const val TEXT_SIZE_DP = 12
        const val LEFT_RIGHT_BORDER_WIDTH = 30f
        const val TOP_BOTTOM_BORDER_WIDTH = 3f
        const val CHART_STOKE_WIDTH = 2f
        const val CIRCLE_CHART_STOKE_WIDTH = 5f
        const val CIRCLE_CHART_RADIUS = 15f
        const val LEGEND_RECT_STOKE_WIDTH = 5f
        const val TEXT_STROKE_WIDTH = 1f
        const val ANIMATION_DURATION: Long = 300
        const val MINIMAL_DISTANCE = 50
        const val BAR_SIZE = 10
        const val MAGIC = 1.1f
    }

}