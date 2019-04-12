package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
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
    internal var startIndex: Int = 0
    internal var stopIndex: Int = 0

    internal lateinit var chartData: ChartData
    internal val paints = mutableMapOf<String, Paint>()
    internal val yShouldVisible = mutableMapOf<Int, Boolean>()
    internal val chartLines = mutableListOf<ChartLineExt>()

    internal val yMinMaxValues = mutableMapOf<Int, MinMaxValues>()
    internal val yScales = mutableMapOf<Int, Float>()

    open fun setData(chartData: ChartData) {
        this.chartData = chartData

        minX = chartData.getMinInDays()
        maxX = chartData.getMaxInDays()

        if (chartData.yScaled) {
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]
                val min = yValue.yValues.min()!!
                val max = yValue.yValues.max()!!
                yMinMaxValues.put(yIndex, MinMaxValues(min, max))
            }
        } else {
            val min = chartData.getMinYs()
            val max = chartData.getMaxYs()
            yMinMaxValues.put(0, MinMaxValues(min, max))
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
                when (chartData.ys.first().type) {
                    "bar" -> {
                        for (stackedRect in chartLines) {
                            drawRect(
                                stackedRect.x,
                                stackedRect.y,
                                stackedRect.x2,
                                stackedRect.y2,
                                stackedRect.paint
                            )
                        }
                        return@apply
                    }
                    "area" -> {
                        Log.d("area", "asa")

//                        paths.keys.forEachIndexed { index, yIndex ->
//                            val path = paths[yIndex]!!
//                            if (index == 0) {
////                                path.lineTo(baseWidth, baseHeight)
////                                path.lineTo(0f, baseHeight)
////                                path.close()
//                            } else if (index == paths.keys.size - 1) {
////                                path.lineTo(baseWidth, 0f)
////                                path.lineTo(0f, 0f)
////                                path.close()
//                            } else {
//
//                            }
//                            val paint = paints[chartData.ys[index].name]!!
//                            drawPath(path, paint)
//                        }

                    }
                }
            }

            for (index in 0 until chartLines.size) {
                val chartLine = chartLines[index]
                if (yShouldVisible[chartLine.yIndex] == false) continue
                when (chartLine.type) {
                    "area", "line" -> {
                        if (chartLine.xIndex > 0) {
                            val prev = chartLines.subList(0, index)
                                .filter { it.xIndex == chartLine.xIndex - 1 && it.yIndex == chartLine.yIndex }
                            if (prev.isNotEmpty()) {
                                val x1 = prev.last().x
                                val y1 = prev.last().y

                                //chartLine.paint.color = ContextCompat.getColor(context, R.color.legend_xy)

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
                }
            }
        }
    }

    internal fun isInitialized(): Boolean {
        return ::chartData.isInitialized
    }

    internal fun calculateYScales() {
        for (index in 0 until yMinMaxValues.size) {
            val yScale = baseHeight / (yMinMaxValues[index]!!.max - yMinMaxValues[index]!!.min).toFloat()
            yScales.put(index, yScale)
        }
    }

    internal fun getChartLinesExt(
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        yMinMaxValues: Map<Int, MinMaxValues>
    ): List<ChartLineExt> {
        val xScale = baseWidth / (maxX - minX).toFloat()
        val result = LinkedList<ChartLineExt>()

        if (chartData.stacked) {
            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesInDays[xIndex]
                val xDate = chartData.xValues[xIndex]
                val yValues = chartData.ys.map { it.yValues[xIndex] }
                val mapYValue = mutableMapOf<Int, Long>()
                for (yIndex in 0 until yValues.size) {
                    mapYValue.put(yIndex, yValues[yIndex])
                }
                val stackedType = chartData.ys.first().type
                when (stackedType) {
                    "bar" -> {
                        val sorted = mapYValue.toList().sortedByDescending { it.second }.toMap()
                        val scale = baseHeight / (yMinMaxValues[0]!!.max ).toFloat()
                        var maxValue = true
                        var maxY = 0f
                        for (yIndex in sorted.keys) {
                            if (!yShouldVisible[yIndex]!!) continue

                            val value = sorted[yIndex]!!
                            val paint = paints[chartData.ys[yIndex].name]!!
                            val x = (xDays - minX) * xScale
                            val y = baseHeight - (value ) * scale

                            if (maxValue) {
                                maxValue = false
                                maxY = y
                                result.add(
                                    ChartLineExt(
                                        xIndex,
                                        yIndex,
                                        xDate,
                                        xDays,
                                        chartData.yScaled,
                                        value,
                                        x - BAR_SIZE / 2,
                                        y,
                                        paint,
                                        stackedType,
                                        chartData.ys,
                                        x + BAR_SIZE / 2,
                                        baseHeight
                                    )
                                )
                            } else {
                                val diff = baseHeight - y
                                result.add(
                                    ChartLineExt(
                                        xIndex,
                                        yIndex,
                                        xDate,
                                        xDays,
                                        chartData.yScaled,
                                        value,
                                        x - BAR_SIZE / 2,
                                        maxY,
                                        paint,
                                        stackedType,
                                        chartData.ys,
                                        x + BAR_SIZE / 2,
                                        maxY + diff
                                    )
                                )
                            }
                        }
                    }
                    "area" -> {
//                        val groupedByY = result.groupBy { it.yIndex }
//                        val sumByYIndex = groupedByY.mapValues {
//                            it.value.filter { yShouldVisible[it.yIndex]!! }.map { it.yValue }.sum()
//                        }
//                        val minByYIndex = groupedByY.mapValues {
//                            it.value.filter { yShouldVisible[it.yIndex]!! }.map { it.yValue }.min()
//                        }
//                        val maxByYIndex = groupedByY.mapValues {
//                            it.value.filter { yShouldVisible[it.yIndex]!! }.map { it.yValue }.max()
//                        }
//                        val countByYIndex = groupedByY.mapValues {
//                            it.value.filter { yShouldVisible[it.yIndex]!! }.map { it.yValue }.count()
//                        }
//                        val avgByYIndex = sumByYIndex.mapValues { sumByYIndex[it.key]!! / countByYIndex[it.key]!! }
//                        val sum = avgByYIndex.map { it.value }.sum()
//                        val prc =
//                            avgByYIndex.mapValues { it.value * 100 / sum }
//                        //val sortedPrc = prc.toList().sortedByDescending { it.second }.toMap()
//
//                        for (drawChartLine in xFilteredByVisibility) {
//                            val yIndex = drawChartLine.yIndex
//
//                            val scale = baseHeight / (maxByYIndex[yIndex]!! - minByYIndex[yIndex]!!).toFloat()
//                            val y = baseHeight - (drawChartLine.yValue - minByYIndex[yIndex]!!) * scale
//
//                            if (!paths.containsKey(drawChartLine.yIndex)) {
//                                val path = Path()
//                                path.moveTo(
//                                    drawChartLine.x,
//                                    y
//                                )
//                                paths.put(drawChartLine.yIndex, path)
//                                continue
//                            }
//                            val path = paths[drawChartLine.yIndex]!!
//                            path.lineTo(drawChartLine.x, y)
//                        }
                    }

                }
            }
        } else {
            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesInDays[xIndex]
                val xDate = chartData.xValues[xIndex]
                for (yIndex in 0 until chartData.ys.size) {
                    val yValue = chartData.ys[yIndex]

                    val x1 = (xDays - minX) * xScale
                    var y1: Float
                    if (chartData.yScaled) {
                        val scale = baseHeight / (yMinMaxValues[yIndex]!!.max - yMinMaxValues[yIndex]!!.min).toFloat()
                        y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[yIndex]!!.min) * scale
                    } else {
                        val scale = baseHeight / (yMinMaxValues[0]!!.max - yMinMaxValues[0]!!.min).toFloat()
                        y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[0]!!.min) * scale
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
        val ys: List<YValue>,
        var x2: Float = 0f,
        var y2: Float = 0f
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