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
    internal var maxX: Long = 0
    internal var minX: Long = 0
    internal var xScale: Float = 1f
    //    internal var yScale: Float = 1f
    internal var startIndex: Int = 0
    internal var stopIndex: Int = 0

    internal lateinit var chartData: ChartData
    internal val paints = mutableMapOf<String, Paint>()
    internal val yShouldVisible = mutableMapOf<Int, Boolean>()
    internal val chartLines = mutableListOf<ChartLineExt>()

    internal val yMinMaxValues = mutableListOf<MinMaxValues>()
    internal val yScales = mutableListOf<Float>()

    open fun setData(chartData: ChartData) {
        this.chartData = chartData

        minX = chartData.getMinInDays()
        maxX = chartData.getMaxInDays()

        if (chartData.yScaled) {
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]
                val min = yValue.yValues.min()!!
                val max = yValue.yValues.max()!!
                yMinMaxValues.add(MinMaxValues(min, max))
            }
        } else {
            val min = chartData.getMinYs()
            val max = chartData.getMaxYs()
            yMinMaxValues.add(MinMaxValues(min, max))
        }

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

            if (chartData.stacked) {
                val groupedByX = chartLines.groupBy { it.xIndex }
                for (xIndex in groupedByX.keys) {
                    val xIndexes = groupedByX[xIndex]!!
                    val xSortedByYValue = xIndexes.sortedByDescending { it.yValue }
                    val chartLine = xSortedByYValue.first()
                    if (yShouldVisible[chartLine.yIndex] == true) {
                        val type = chartLine.type
                        when (type) {
                            "bar" -> {
                                for (yIndex in 0 until xSortedByYValue.size) {
                                    val drawChartLine = xSortedByYValue[yIndex]
                                    if (yIndex == 0) {
                                        drawRect(
                                            drawChartLine.x - BAR_SIZE / 2,
                                            drawChartLine.y,
                                            drawChartLine.x + BAR_SIZE / 2,
                                            baseHeight,
                                            drawChartLine.paint
                                        )
                                    } else {
                                        val diff = baseHeight - drawChartLine.y
                                        drawRect(
                                            drawChartLine.x - BAR_SIZE / 2,
                                            chartLine.y,
                                            drawChartLine.x + BAR_SIZE / 2,
                                            chartLine.y + diff,
                                            drawChartLine.paint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                return@apply
            }

            for (index in 0 until chartLines.size) {
                val chartLine = chartLines[index]
                if (yShouldVisible[chartLine.yIndex] == false) continue
                when (chartLine.type) {
                    "line" -> {
                        if (chartLine.xIndex > 0) {
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
                        drawRect(
                            chartLine.x - BAR_SIZE / 2,
                            chartLine.y,
                            chartLine.x + BAR_SIZE / 2,
                            baseHeight,
                            chartLine.paint
                        )
                    }
                    "area" -> {
                        if (chartLine.xIndex > 0) {
                            val prev = chartLines.subList(0, index)
                                .filter { it.xIndex == chartLine.xIndex - 1 && it.yIndex == chartLine.yIndex }
                            if (prev.isNotEmpty()) {
                                val x1 = prev.last().x
                                val y1 = prev.last().y
                                drawLine(x1, y1, chartLine.x, chartLine.y, chartLine.paint)
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun isInitialized(): Boolean {
        return ::chartData.isInitialized
    }

    internal fun calculateYScales() {
        for (index in 0 until yMinMaxValues.size) {
            val yScale = baseHeight / (yMinMaxValues[index].max - yMinMaxValues[index].min).toFloat()
            yScales.add(yScale)
        }
    }

    internal fun getChartLinesExt(
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        yMinMaxValues: List<MinMaxValues>
    ): List<ChartLineExt> {
        val xScale = baseWidth / (maxX - minX).toFloat()
        val result = mutableListOf<ChartLineExt>()

        for (xIndex in startIndex until stopIndex) {
            val xDays = chartData.xValuesInDays[xIndex]
            val xDate = chartData.xValues[xIndex]
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]

                val x1 = (xDays - minX) * xScale
                var y1: Float
                if (chartData.yScaled) {
                    val scale = baseHeight / (yMinMaxValues[yIndex].max - yMinMaxValues[yIndex].min).toFloat()
                    y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[yIndex].min) * scale
                } else {
                    val scale = baseHeight / (yMinMaxValues[0].max - yMinMaxValues[0].min).toFloat()
                    y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[0].min) * scale
                }

                val chartPaint = paints[chartData.ys[yIndex].name]!!
                val chartType = yValue.type

                result.add(
                    ChartLineExt(
                        xIndex,
                        yIndex,
                        xDate,
                        xDays,
                        chartData.yScaled,
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
        val yScaled: Boolean,
        val yValue: Long,
        var x: Float,
        var y: Float,
        val paint: Paint,
        val type: String,
        val ys: List<YValue>
    )

    data class MinMaxValues(
        var min: Long,
        var max: Long
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

        fun reduction(value: Long): String {
            var reductionValue = value
            if (value > 10000000) {
                return (value / 1000000).toString() + "M"
            }
            if (value > 10000) {
                reductionValue -= value % 1000
                return (reductionValue + 1000).toString()
            }
            if (value > 1000) {
                reductionValue -= value % 100
                return (reductionValue + 100).toString()
            }
            if (value > 100) {
                reductionValue -= value % 10
                return (reductionValue + 10).toString()
            }
            if (value > 10) {
                reductionValue -= value % 10
                return (reductionValue + 10).toString()
            }
            return reductionValue.toString()
        }

    }

}