package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum
import ru.merkulyevsasha.chartscontest.models.YValue
import java.text.DecimalFormat
import java.text.NumberFormat
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
    internal val paints = mutableMapOf<Int, Paint>()
    internal val piePaints = mutableMapOf<Int, Paint>()
    internal val yShouldVisible = mutableMapOf<Int, Boolean>()
    internal val chartLines = mutableListOf<ChartLineExt>()

    internal val yMinMaxValues = mutableMapOf<Int, MinMaxValues>()
    internal val yScales = mutableMapOf<Int, Float>()

    internal val animationInProgress = AtomicBoolean(false)
    internal var animatorSet: AnimatorSet? = null
    internal var noChartLines = false
    private var newChartLines: List<ChartLineExt>? = null

    private var onMeasureCalling = true

    private var yPaths = mutableMapOf<Int, PathPaint>()
    private var tempPath = Path()

    internal var showPie = false

    private val legendPiePaint = Paint()
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 0f

    init {
        val metrics = resources.displayMetrics
        legendPiePaint.strokeWidth = TEXT_STROKE_WIDTH
        legendPiePaint.style = Paint.Style.FILL_AND_STROKE
        legendPiePaint.color = ContextCompat.getColor(getContext(), R.color.white)
        legendPiePaint.textSize = TEXT_SIZE_DP * metrics.density
    }

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
        piePaints.clear()
        chartData.ys.forEachIndexed { index, ys ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.color = ys.color
            paint.strokeWidth = CHART_STOKE_WIDTH
            paints.put(index, paint)

            val piePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            piePaint.style = Paint.Style.FILL_AND_STROKE
            piePaint.color = ys.color
            piePaint.strokeWidth = CHART_STOKE_WIDTH
            piePaints.put(index, piePaint)

            yShouldVisible.put(index, true)

            yPaths[index] = PathPaint(Path(), paint, index)
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
            if (radius <= 0) {
                centerX = baseWidth / 2
                centerY = baseHeight / 2
                radius = baseWidth / 3
            }

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
                        }
                        ChartTypeEnum.AREA -> {

                            if (showPie) {

                                val coords = getArcCoords()
                                val percents = chartLines.first().percents
                                for (yIndex in percents.keys) {
                                    if (!yShouldVisible[yIndex]!!) continue

                                    val paint = piePaints[yIndex]
                                    val rect = RectF()
                                    rect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

                                    drawArc(rect, coords[yIndex]!!.startAngle, coords[yIndex]!!.angle, true, paint)
                                }

                                for (yIndex in percents.keys) {
                                    if (!yShouldVisible[yIndex]!!) continue
                                    val name = percents[yIndex]!!.toInt().toString() + "%"
                                    drawText(name, coords[yIndex]!!.x, coords[yIndex]!!.y, legendPiePaint)
                                }

                                if (animationInProgress.compareAndSet(false, false)) {
                                    return@apply
                                }
                            }

                            for (pp in yPaths.values) {
                                pp.path.reset()
                            }
                            //val sortedChartLines = chartLines.sortedBy { it.yIndex }

                            for (chartLine in chartLines) {
                                val pathPaint = yPaths[chartLine.yIndex]!!
                                if (pathPaint.path.isEmpty) {
                                    pathPaint.path.moveTo(chartLine.x, chartLine.y)
                                    continue
                                }
                                pathPaint.path.lineTo(chartLine.x, chartLine.y)
                            }

                            val notEmptyValues = yPaths.filter { !it.value.path.isEmpty }.map { it.value }
                            for (index in 0 until notEmptyValues.size) {
                                val yIndex = notEmptyValues[index].yIndex
                                val path = notEmptyValues[index].path
                                val paint = notEmptyValues[index].paint
                                if (index == 0) {
                                    path.lineTo(baseWidth, baseHeight)
                                    path.lineTo(0f, baseHeight)
                                    path.close()
                                    drawPath(path, paint)
                                } else if (index == notEmptyValues.size - 1) {
//                                    path.lineTo(baseWidth, 0f)
//                                    path.lineTo(0f, 0f)
                                    tempPath.reset()
                                    tempPath.moveTo(baseWidth, 0f)
                                    tempPath.lineTo(0f, 0f)
                                    val sorted =
                                        chartLines.filter { it.yIndex < yIndex }.sortedByDescending { it.yIndex }
                                    val first = sorted.firstOrNull()
                                    if (first != null) {
                                        val mmm = sorted.filter { it.yIndex == first.yIndex }
                                        for (chartLine in mmm) {
                                            tempPath.lineTo(chartLine.x, chartLine.y)
                                        }
                                    }
                                    tempPath.close()
                                    drawPath(tempPath, paint)
                                } else {
//                                    tempPath.reset()
//                                    tempPath.addPath(yPaths[index - 1]!!.path)
//                                    tempPath.addPath(path)
//                                    tempPath.close()
//                                    if (index == 2) {
                                    val sorted =
                                        chartLines.filter { it.yIndex < yIndex }.sortedByDescending { it.yIndex }
                                    val first = sorted.firstOrNull()
                                    if (first != null) {
                                        val mmm =
                                            sorted.filter { it.yIndex == first.yIndex }.sortedByDescending { it.xIndex }
                                        for (chartLine in mmm) {
                                            path.lineTo(chartLine.x, chartLine.y)
                                        }
                                    }
//                                    }
                                    path.close()
                                    drawPath(path, paint)
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

        val x1 = (chartData.xValuesIn()[0] - minX) * xScale
        val x2 = (chartData.xValuesIn()[1] - minX) * xScale
        val barSize = x2 - x1

        if (chartData.stacked) {
            val stackedChartType = chartData.ys.first().type
            val yNumber = chartData.ys.size

            var sortedArea: Map<Int, Double> = mutableMapOf()
            var prc: Map<Int, Double> = mutableMapOf()
            val areaYScale = mutableMapOf<Int, Float>()
            val mapYMin = mutableMapOf<Int, Long>()
            val mapYMax = mutableMapOf<Int, Long>()
            val mapYAvg = mutableMapOf<Int, Double>()
            if (stackedChartType == ChartTypeEnum.AREA) {
                val avg = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).average() }
                val max = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).max()!! }
                val min = chartData.ys.map { it.yValues.subList(startIndex, stopIndex).min()!! }
                for (yIndex in 0 until avg.size) {
                    if (!yShouldVisible[yIndex]!!) continue
                    mapYAvg.put(yIndex, avg[yIndex])
                    mapYMax.put(yIndex, max[yIndex])
                    mapYMin.put(yIndex, min[yIndex])
                    //areaYScale.put(yIndex, baseHeight / (mapYAvg[yIndex]!!.toFloat()))
                    //areaYScale.put(yIndex, baseHeight / (mapYMax[yIndex]!! - mapYMin[yIndex]!!))
                    areaYScale.put(yIndex, baseHeight / (max[yIndex]))
                }
                val avgSumma = mapYAvg.map { it.value }.sum()
//                val maxSumma = max.max()!!
//                val minSumma = min.sum()
                prc = mapYAvg.mapValues { it.value * 100 / avgSumma }
//                for (yIndex in mapYAvg.keys) {
//                    System.out.println("area -> ${chartData.ys[yIndex].name} - ${prc[yIndex]} - ${avg[yIndex]} - ${min[yIndex]} - ${max[yIndex]}")
//                }
                //sortedArea = mapYAvg.toList().sortedByDescending { it.second }.toMap()
            }

            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesIn()[xIndex]
                val xDate = chartData.xValues[xIndex]
                val x = (xDays - minX) * xScale
                val yValues = chartData.ys.map { it.yValues[xIndex] }
                val mapYValue = mutableMapOf<Int, Long>()
                for (yIndex in 0 until yValues.size) {
                    mapYValue.put(yIndex, yValues[yIndex])
                }
                if (stackedChartType == ChartTypeEnum.BAR) {
                    val sortedBar = mapYValue.toList().sortedByDescending { it.second }.toMap()
                    val scale = baseHeight / (yMinMaxValues[0]!!.max).toFloat()
                    var maxValue = true
                    var maxY = 0f
                    for ((index, yIndex) in sortedBar.keys.withIndex()) {
                        if (!yShouldVisible[yIndex]!!) continue

                        val value = sortedBar[yIndex]!!
                        val paint = paints[yIndex]!!
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
                                    x - barSize / 2,
                                    y,
                                    paint,
                                    stackedChartType,
                                    chartData.ys,
                                    x + barSize / 2,
                                    baseHeight,
                                    barSize = barSize,
                                    order = yNumber - index
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
                                    x - barSize / 2,
                                    maxY,
                                    paint,
                                    stackedChartType,
                                    chartData.ys,
                                    x + barSize / 2,
                                    maxY + diff,
                                    barSize = barSize,
                                    order = yNumber - index
                                )
                            )
                        }
                    }
                } else if (stackedChartType == ChartTypeEnum.AREA) {

                    var yvals = 0.0
                    for (yIndex in mapYAvg.keys) {
                        if (!yShouldVisible[yIndex]!!) continue
                        val paint = paints[yIndex]!!
                        var value = mapYValue[yIndex]!!
                        val avg = mapYAvg[yIndex]!!
                        val deviation = (avg * 10 / 100).toLong()
                        if (value + deviation < avg || value - deviation > avg) value = avg.toLong()

//                        val scale = baseHeight / (yMinMaxValues[0]!!.max - yMinMaxValues[0]!!.min).toFloat()
//                        val y = baseHeight - (value - yMinMaxValues[0]!!.min) * scale
                        //val y = y1 * 100 / baseHeight * prc[yIndex]!! / 100


//                        val y =
//                            baseHeight - (baseHeight - (value - mapYMin[yIndex]!!) * areaYScale[yIndex]!!) * prc[yIndex]!! / 100
//                        val y =  ((baseHeight - value  * areaYScale[yIndex]!!) * prc[yIndex]!! / 100).toFloat()

//                        val scale = baseHeight / (yMinMaxValues[0]!!.max - yMinMaxValues[0]!!.min).toFloat()
//                        val y = baseHeight - (value - yMinMaxValues[0]!!.min) * scale
                        yvals += (baseHeight - (baseHeight - (value) * areaYScale[yIndex]!!)) * prc[yIndex]!! / 100

                        if (baseHeight == 660f) {
                            System.out.println(
                                "area ->" + yIndex + " - " + chartData.ys[yIndex].name +
                                        " procent= " + prc[yIndex] +
                                        " height= " + baseHeight + " - yvals= " + yvals + " -  y= " + (baseHeight - yvals.toFloat())
                            )
                        }
                        result.add(
                            ChartLineExt(
                                xIndex,
                                yIndex,
                                xDate,
                                xDays,
                                chartData.yScaled,
                                value,
                                x - barSize / 2,
                                baseHeight - yvals.toFloat(),
                                paint,
                                stackedChartType,
                                chartData.ys,
                                barSize = barSize,
                                percents = prc
                            )
                        )
                    }
                    //System.out.println("area")
                }
            }
        } else {
            for (xIndex in startIndex until stopIndex) {
                val xDays = chartData.xValuesIn()[xIndex]
                val xDate = chartData.xValues[xIndex]
                for (yIndex in 0 until chartData.ys.size) {
                    val yValue = chartData.ys[yIndex]

                    val x1 = (xDays - minX) * xScale
                    val chartPaint = paints[yIndex]!!
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
                                chartData.ys,
                                barSize = barSize
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
                                x1 - barSize / 2,
                                y1,
                                chartPaint,
                                chartType,
                                chartData.ys,
                                x1 + barSize / 2,
                                baseHeight,
                                barSize = barSize
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

    internal fun getBarToBarAnimation(
        chartLines: List<ChartLineExt>, oldStartIndex: Int, oldStopIndex: Int,
        newChartLines: List<ChartLineExt>, newStartIndex: Int, newStopIndex: Int
    ): List<Animator> {
        val animators = mutableListOf<Animator>()

        val chartLinesSize = chartLines.size
        val newChartLinesSize = newChartLines.size

        if (chartLinesSize > newChartLinesSize) {

            val startAnimIndex = chartLinesSize / 2 - newChartLinesSize / 2
            val stopAnimIndex = startAnimIndex + newChartLinesSize
            for (xIndex in 0 until chartLines.size) {
                val chartLine = chartLines[xIndex]
                if (xIndex < startAnimIndex) {
                    val x1Animator = ValueAnimator.ofFloat(chartLine.x, chartLine.x - 1000)
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(chartLine.x2, chartLine.x2 - 1000)
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)
                } else if (xIndex >= stopAnimIndex) {
                    val x1Animator = ValueAnimator.ofFloat(chartLine.x, chartLine.x + 1000)
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(chartLine.x2, chartLine.x2 + 1000)
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)
                } else {
                    val newxIndex = xIndex - startAnimIndex
                    val newChartLine = newChartLines[newxIndex]

                    val colorAnimator =
                        ValueAnimator.ofArgb(chartLine.paint.color, newChartLine.paint.color)
                    colorAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.color = this as Int
                            invalidate()
                        }
                    }
                    animators.add(colorAnimator)

                    val x1Animator = ValueAnimator.ofFloat(chartLine.x, newChartLine.x)
                    x1Animator.duration =
                        ANIMATION_REPLACING_DURATION - newChartLine.order * ANIMATION_ORDER_ACCELERATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(chartLine.x2, newChartLine.x2)
                    x2Animator.duration =
                        ANIMATION_REPLACING_DURATION - newChartLine.order * ANIMATION_ORDER_ACCELERATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)

                    val y1Animator = ValueAnimator.ofFloat(chartLine.y, newChartLine.y)
                    y1Animator.duration =
                        ANIMATION_REPLACING_DURATION - newChartLine.order * ANIMATION_ORDER_ACCELERATION
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    animators.add(y1Animator)

                    val y2Animator = ValueAnimator.ofFloat(chartLine.y2, newChartLine.y2)
                    y2Animator.duration =
                        ANIMATION_REPLACING_DURATION - newChartLine.order * ANIMATION_ORDER_ACCELERATION
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(y2Animator)

                }

            }
        } else { // chartLinesSize <= newChartLinesSize

            val startAnimIndex =
                if (chartLinesSize == newChartLinesSize) 0 else newChartLinesSize / 2 - chartLinesSize / 2
            val stopAnimIndex =
                if (chartLinesSize == newChartLinesSize) chartLinesSize else chartLinesSize + startAnimIndex
            setNewChartLines(newChartLines)
            noChartLines = true

            for (indexLine in 0 until newChartLines.size) {
                val chartLine = newChartLines[indexLine]
                if (indexLine < startAnimIndex) {
                    val x1Animator = ValueAnimator.ofFloat(chartLine.x - 1000, chartLine.x)
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(chartLine.x2 - 1000, chartLine.x2)
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)

                } else if (indexLine >= stopAnimIndex) {
                    val x1Animator = ValueAnimator.ofFloat(chartLine.x + 1000, chartLine.x)
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(chartLine.x2 + 1000, chartLine.x2)
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)
                } else {
                    val newxIndex = indexLine - startAnimIndex
                    val oldChartLine = chartLines[newxIndex]

//                    val colorAnimator =
//                        ValueAnimator.ofArgb(oldChartLine.paint.color, chartLine.paint.color)
//                    colorAnimator.addUpdateListener { value ->
//                        value.animatedValue?.apply {
//                            chartLine.paint.color = this as Int
//                            invalidate()
//                        }
//                    }
//                    animators.add(colorAnimator)

                    val x1Animator = ValueAnimator.ofFloat(oldChartLine.x, chartLine.x)
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)

                    val x2Animator = ValueAnimator.ofFloat(oldChartLine.x2, chartLine.x2)
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x2Animator)

                    val y1Animator = ValueAnimator.ofFloat(oldChartLine.y, chartLine.y)
                    y1Animator.duration = ANIMATION_REPLACING_DURATION
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    animators.add(y1Animator)

                    val y2Animator = ValueAnimator.ofFloat(oldChartLine.y2, chartLine.y2)
                    y2Animator.duration = ANIMATION_REPLACING_DURATION
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y2 = this as Float
                            invalidate()
                        }
                    }
                    animators.add(y2Animator)
                }
            }
        }

        return animators
    }

    fun showPie() {

        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        if (animationInProgress.compareAndSet(false, true)) {
            showPie = true
            animatorSet = AnimatorSet()
            val animators = mutableListOf<Animator>()

            val radiusAnimator = ValueAnimator.ofFloat(baseWidth / 10, baseWidth / 3)
            radiusAnimator.duration = ANIMATION_REPLACING_DURATION
            radiusAnimator.addUpdateListener { value ->
                value.animatedValue?.apply {
                    radius = this as Float
                    invalidate()
                }
            }
            animators.add(radiusAnimator)

            for (paint in piePaints.values) {
                val paintAnimator = ValueAnimator.ofInt(0, 255)
                paintAnimator.duration = ANIMATION_REPLACING_DURATION_SLOWER
                paintAnimator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        paint.alpha = this as Int
                        invalidate()
                    }
                }
                animators.add(paintAnimator)
            }

            for (paint in paints.values) {
                val paintAnimator = ValueAnimator.ofInt(255, 0)
                paintAnimator.duration = ANIMATION_REPLACING_DURATION
                paintAnimator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        paint.alpha = this as Int
                        invalidate()
                    }
                }
                animators.add(paintAnimator)
            }

            val coords = getArcCoords()

            for (chartLine in chartLines) {

                val y1Animator = ValueAnimator.ofFloat(chartLine.y, coords[chartLine.yIndex]!!.y)
                y1Animator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        chartLine.y = this as Float
                        invalidate()
                    }
                }
                y1Animator.duration = ANIMATION_REPLACING_DURATION
                animators.add(y1Animator)

                val x1Animator = ValueAnimator.ofFloat(chartLine.x, coords[chartLine.yIndex]!!.x)
                x1Animator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        chartLine.x = this as Float
                        invalidate()
                    }
                }
                x1Animator.duration = ANIMATION_REPLACING_DURATION
                animators.add(x1Animator)
            }

            animatorSet?.apply {
                this.playTogether(animators)
                this.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        animationInProgress.set(false)
                        invalidate()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        animationInProgress.set(false)
                        invalidate()
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })
                this.start()
            }
        }

        //invalidate()
    }

    private fun getArcCoords(): Map<Int, ArcCoord> {
        var startAngle = 0f
        val percents = chartLines.first().percents
        val coords = mutableMapOf<Int, ArcCoord>()
        for (yIndex in percents.keys) {
            if (!yShouldVisible[yIndex]!!) continue
            val angle = (360 * percents[yIndex]!! / 100).toFloat()
            val angl = startAngle + angle + 90

            val cos = radius / 2 * Math.cos(angl * GRAD_TO_RAD)
            val sin = radius / 2 * Math.sin(angl * GRAD_TO_RAD)
            val xtext = (centerX - cos).toFloat()
            val ytext = (centerY - sin).toFloat()

            coords.put(yIndex, ArcCoord(startAngle, angle, xtext, ytext))
            startAngle += angle
        }
        return coords
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
        var y2: Float = 0f,
        val barSize: Float = 0f,
        val order: Int = 0,
        val percents: Map<Int, Double> = emptyMap()
    )

    data class MinMaxValues(
        var min: Long,
        var max: Long
    )

    data class PathPaint(
        val path: Path,
        val paint: Paint,
        val yIndex: Int
    )

    data class ArcCoord(
        val startAngle: Float,
        val angle: Float,
        val x: Float,
        val y: Float
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
        const val ANIMATION_REPLACING_DURATION_SLOWER: Long = 2000
        const val ANIMATION_REPLACING_DURATION: Long = 1000
        const val ANIMATION_REPLACING_DURATION_FASTER: Long = 500
        const val ANIMATION_ORDER_ACCELERATION: Long = 100
        const val MINIMAL_DISTANCE = 50
        const val MAGIC = 1.1f
        private val GRAD_TO_RAD = Math.PI / 180

        fun reduction(value: Long): String {
            var reductionValue = value
            if (value > 10000000) {
                return (value / 1000000).toString() + "M"
            }
            if (value > 10000) {
                reductionValue -= value % 1000
                return formatNumber(reductionValue + 1000).toString()
            }
            if (value > 1000) {
                reductionValue -= value % 100
                return formatNumber(reductionValue + 100).toString()
            }
            if (value > 100) {
                reductionValue -= value % 10
                return formatNumber(reductionValue + 10).toString()
            }
            if (value > 10) {
                reductionValue -= value % 10
                return formatNumber(reductionValue + 10).toString()
            }
            return reductionValue.toString()
        }

        fun formatNumber(number: Long): String {
            val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
            val symbols = formatter.decimalFormatSymbols
            symbols.groupingSeparator = ' '
            formatter.decimalFormatSymbols = symbols
            return formatter.format(number)
        }

    }
}