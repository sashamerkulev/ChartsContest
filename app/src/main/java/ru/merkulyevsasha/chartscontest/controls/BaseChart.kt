package ru.merkulyevsasha.chartscontest.controls

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum
import ru.merkulyevsasha.chartscontest.models.YValue
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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

    internal val animationInProgress = AtomicBoolean(false)
    internal var animatorSet: AnimatorSet? = null
    internal var noChartLines = false
    private var newChartLines: List<ChartLineExt>? = null

    private var onMeasureCalling = true

    open fun setData(chartData: ChartData) {
        this.chartData = chartData

        minX = chartData.getMinX()
        maxX = chartData.getMaxX()

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
        stopIndex = chartData.xValuesIn().size

        yShouldVisible.clear()
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

        if (isInitialized()) {
            if (onMeasureCalling) {
                onMeasureCalling = false
                onMeasureEnd()
            }
        }
    }

    open fun onMeasureEnd() {

    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            if (!noChartLines) {

                if (chartData.stacked) {
                    when (chartData.ys.first().type) {
                        ChartTypeEnum.BAR -> {
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
                        ChartTypeEnum.AREA -> {
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
                        ChartTypeEnum.AREA,
                        ChartTypeEnum.LINE -> {
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
                        ChartTypeEnum.BAR -> {
                            drawRect(
                                chartLine.x,
                                chartLine.y,
                                chartLine.x2,
                                chartLine.y2,
                                chartLine.paint
                            )
                        }
                    }
                }
            }

            if (animationInProgress.compareAndSet(true, true) && newChartLines != null) {
                for (index in 0 until newChartLines!!.size) {
                    val chartLine = newChartLines!![index]
                    when (chartLine.type) {
                        ChartTypeEnum.LINE -> {
                            if (chartLine.xIndex > 0) {
                                val prev = newChartLines!!.subList(0, index)
                                    .filter { it.xIndex == chartLine.xIndex - 1 && it.yIndex == chartLine.yIndex }
                                if (prev.isNotEmpty()) {
                                    val x1 = prev.last().x
                                    val y1 = prev.last().y
                                    drawLine(x1, y1, chartLine.x, chartLine.y, chartLine.paint)
                                }
                            }
                        }
                        ChartTypeEnum.BAR -> {
                            drawRect(
                                chartLine.x,
                                chartLine.y,
                                chartLine.x2,
                                chartLine.y2,
                                chartLine.paint
                            )
                        }
                    }
                }
            }


        }
    }

    internal fun setNewChartLines(newChartLines: List<ChartLineExt>?) {
        this.newChartLines = newChartLines
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
        chartData: ChartData,
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        yMinMaxValues: Map<Int, MinMaxValues>
    ): List<ChartLineExt> {
        val xScale = baseWidth / (maxX - minX).toFloat()
        val result = mutableListOf<ChartLineExt>()

        if (chartData.stacked) {
            val stackedType = chartData.ys.first().type

            var sortedArea: Map<Int, Double> = mutableMapOf()
            var prc: Map<Int, Double> = mutableMapOf()
            val areaYScale = mutableMapOf<Int, Float>()
            val mapYMin = mutableMapOf<Int, Long>()
            if (stackedType == ChartTypeEnum.AREA) {
                val avg = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).average() }
                val max = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).max()!! }
                val min = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).min()!! }
                val mapYAvg = mutableMapOf<Int, Double>()
                val mapYMax = mutableMapOf<Int, Long>()
                for (yIndex in 0 until avg.size) {
                    if (!yShouldVisible[yIndex]!!) continue
                    mapYAvg.put(yIndex, avg[yIndex])
                    mapYMax.put(yIndex, max[yIndex])
                    mapYMin.put(yIndex, min[yIndex])
                    areaYScale.put(yIndex, baseHeight / (max[yIndex]))
                }
                val avgSumma = mapYAvg.map { it.value }.sum()
                val maxSumma = max.max()!!
                val minSumma = min.sum()
                prc = mapYAvg.mapValues { it.value * 100 / avgSumma }
//                for (yIndex in mapYAvg.keys) {
//                    System.out.println("area -> ${chartData.ys[yIndex].name} - ${prc[yIndex]} - ${avg[yIndex]} - ${min[yIndex]} - ${max[yIndex]}")
//                }
                sortedArea = mapYAvg.toList().sortedByDescending { it.second }.toMap()
            }

            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesIn()[xIndex]
                val xDate = chartData.xValues[xIndex]
                val yValues = chartData.ys.map { it.yValues[xIndex] }
                val mapYValue = mutableMapOf<Int, Long>()
                for (yIndex in 0 until yValues.size) {
                    mapYValue.put(yIndex, yValues[yIndex])
                }
                when (stackedType) {
                    ChartTypeEnum.BAR -> {
                        val sortedBar = mapYValue.toList().sortedByDescending { it.second }.toMap()
                        val scale = baseHeight / (yMinMaxValues[0]!!.max).toFloat()
                        var maxValue = true
                        var maxY = 0f
                        for (yIndex in sortedBar.keys) {
                            if (!yShouldVisible[yIndex]!!) continue

                            val value = sortedBar[yIndex]!!
                            val paint = paints[chartData.ys[yIndex].name]!!
                            val x = (xDays - minX) * xScale
                            val y = baseHeight - (value) * scale

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
                    ChartTypeEnum.AREA -> {
//                        var yVals = 0f
                        //System.out.println("area")
                        for (yIndex in sortedArea.keys) {
                            val paint = paints[chartData.ys[yIndex].name]!!
                            val value = mapYValue[yIndex]!!
                            val x = (xDays - minX) * xScale
//                            val y =  ((baseHeight - value  * areaYScale[yIndex]!!) * prc[yIndex]!! / 100).toFloat()
//                            yVals += y
                            //System.out.println("area ->" + chartData.ys[yIndex].name +" procent " + prc[yIndex] + " height "+ baseHeight + " - " + yVals)

                            //val y = yVals

                            val scale = baseHeight / (yMinMaxValues[0]!!.max - yMinMaxValues[0]!!.min).toFloat()
                            val y = baseHeight - (value - yMinMaxValues[0]!!.min) * scale

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
                                    chartData.ys
                                )
                            )
                        }
                        //System.out.println("area")
                    }

                }
            }
        } else {
            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesIn()[xIndex]
                val xDate = chartData.xValues[xIndex]
                for (yIndex in 0 until chartData.ys.size) {
                    val yValue = chartData.ys[yIndex]

                    val x1 = (xDays - minX) * xScale
                    val chartPaint = paints[chartData.ys[yIndex].name]!!
                    val chartType = yValue.type

                    if (yValue.type == ChartTypeEnum.LINE) {
                        var y1: Float
                        if (chartData.yScaled) {
                            val scale =
                                baseHeight / (yMinMaxValues[yIndex]!!.max - yMinMaxValues[yIndex]!!.min).toFloat()
                            y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[yIndex]!!.min) * scale
                        } else {
                            val scale = baseHeight / (yMinMaxValues[0]!!.max - yMinMaxValues[0]!!.min).toFloat()
                            y1 = baseHeight - (yValue.yValues[xIndex] - yMinMaxValues[0]!!.min) * scale
                        }

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
                    } else if (yValue.type == ChartTypeEnum.BAR) {
                        val scale = baseHeight / (yMinMaxValues[0]!!.max).toFloat()
                        val y1 = baseHeight - (yValue.yValues[xIndex]) * scale

                        result.add(
                            ChartLineExt(
                                xIndex,
                                yIndex,
                                xDate,
                                xDays,
                                chartData.yScaled,
                                yValue.yValues[xIndex],
                                x1 - BAR_SIZE / 2,
                                y1,
                                chartPaint,
                                chartType,
                                chartData.ys,
                                x1 + BAR_SIZE / 2,
                                baseHeight
                            )
                        )

                    }
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
        var paint: Paint,
        val type: ChartTypeEnum,
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
        const val TEXT_STROKE_WIDTH = 1f
        const val ANIMATION_DURATION: Long = 300
        const val ANIMATION_REPLACING_DURATION: Long = 1000
        const val ANIMATION_REPLACING_DURATION_FASTER: Long = 500
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