package ru.merkulyevsasha.chartscontest.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum
import ru.merkulyevsasha.chartscontest.models.XValuesEnum
import ru.merkulyevsasha.chartscontest.models.YValue
import java.text.SimpleDateFormat
import java.util.*


class ChartLegend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private var isShow: Boolean = false

    private val pxLegendTextPadding: Float
    private val pxLegendTextLinePadding: Float

    private var nearestPoint: Distance? = null
    private val paintTopBottomLine: Paint
    private val paintCircle: Paint
    private val paintFillCircle: Paint
    private val legendFillRectPaint: Paint
    private val shadowRectPaint: Paint

    private val boundLine = Rect()
    private val boundTitle = Rect()

    private val textLegendTitlePaint: Paint
    private val textLegendNamePaint: Paint
    private val textLegendNumberPaint: Paint

    private val hourPattern = "HH:mm"
    private val hourDateFormat: SimpleDateFormat = SimpleDateFormat(hourPattern, Locale.getDefault())

    private val weekPattern = "EEE, dd"
    private val weekDateFormat: SimpleDateFormat = SimpleDateFormat(weekPattern, Locale.getDefault())

    private val monthPattern = "MMM yyyy"
    private val monthDateFormat: SimpleDateFormat = SimpleDateFormat(monthPattern, Locale.getDefault())

    private val gestureDetector = GestureDetectorCompat(getContext(), LegendGestureListener())

    private val metrics = resources.displayMetrics

    private var lastLegendData: LegendData? = null
    private var newLegendData: LegendData? = null

    private var onLegendClicked: OnLegendClicked? = null

    init {
        pxLegendTextPadding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, metrics)
        pxLegendTextLinePadding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, metrics)

        paintTopBottomLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTopBottomLine.style = Paint.Style.STROKE
        paintTopBottomLine.color = ContextCompat.getColor(context, ru.merkulyevsasha.chartscontest.R.color.chart_line)
        paintTopBottomLine.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH

        paintCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintCircle.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH
        paintCircle.style = Paint.Style.STROKE
        paintCircle.color = ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.border)

        paintFillCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintFillCircle.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH
        paintFillCircle.style = Paint.Style.FILL_AND_STROKE
        paintFillCircle.color =
            ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.legend_bgrnd)

        textLegendTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendTitlePaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendTitlePaint.style = Paint.Style.FILL_AND_STROKE
        textLegendTitlePaint.color =
            ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.legend_title)
        textLegendTitlePaint.textSize = 14 * metrics.density

        textLegendNamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendNamePaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendNamePaint.style = Paint.Style.FILL_AND_STROKE
        textLegendNamePaint.color =
            ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.legend_title)
        textLegendNamePaint.textSize = 12 * metrics.density

        textLegendNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendNumberPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendNumberPaint.style = Paint.Style.FILL_AND_STROKE
        textLegendNumberPaint.textSize = 12 * metrics.density

        val cornerPathEffect10 = CornerPathEffect(20f)
        legendFillRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendFillRectPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        legendFillRectPaint.style = Paint.Style.FILL_AND_STROKE
        legendFillRectPaint.color =
            ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.legend_bgrnd)
        legendFillRectPaint.pathEffect = cornerPathEffect10
        legendFillRectPaint.setShadowLayer(
            10f,
            0f,
            0f,
            ContextCompat.getColor(context, ru.merkulyevsasha.chartscontest.R.color.legend_shadow)
        )

        shadowRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowRectPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        shadowRectPaint.style = Paint.Style.FILL_AND_STROKE
        shadowRectPaint.color =
            ContextCompat.getColor(getContext(), ru.merkulyevsasha.chartscontest.R.color.white_shadow)

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setCallback(onLegendClicked: OnLegendClicked?) {
        this.onLegendClicked = onLegendClicked
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
        this.maxX = maxX
        this.xScale = xScale

        this.yMinMaxValues.clear()
        this.yMinMaxValues.putAll(yMinMaxValues)
        this.yScales.clear()
        this.yScales.putAll(yScale)

        this.chartLines.clear()
        this.chartLines.addAll(chartLines)
        this.yShouldVisible.clear()
        this.yShouldVisible.putAll(yShouldVisible)
        isShow = false
        lastLegendData = null
        newLegendData = null
        nearestPoint = null
        invalidate()
    }

    fun updateData(chartData: ChartData, startIndex: Int, stopIndex: Int) {
        super.setData(chartData)
        this.startIndex = startIndex
        this.stopIndex = stopIndex

        maxX = chartData.xValuesIn().subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesIn().subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        recalculateYScales()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues))

        isShow = false
        lastLegendData = null
        newLegendData = null
        nearestPoint = null
        invalidate()
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
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            if (!isShow) return

            nearestPoint?.let { point ->

                if (newLegendData == null) return@apply

                // vertical line
                if (point.type == ChartTypeEnum.LINE) {
                    this.drawLine(point.x, 0f, point.x, baseHeight, paintTopBottomLine)
                    // draw circles
                    for (yIndex in 0 until point.ys.size) {
                        if (!yShouldVisible[yIndex]!!) continue
                        val yValue = point.ys[yIndex]

                        var min: Long
                        var pointY: Float
                        if (point.yScaled) {
                            min = yMinMaxValues[yIndex]!!.min
                            pointY = baseHeight - (yValue.yValues[point.xIndex] - min) * yScales[yIndex]!!
                        } else {
                            min = yMinMaxValues[0]!!.min
                            pointY = baseHeight - (yValue.yValues[point.xIndex] - min) * yScales[0]!!
                        }

                        paintCircle.color = yValue.color
                        this.drawCircle(point.x, pointY, CIRCLE_CHART_RADIUS, paintFillCircle)
                        this.drawCircle(point.x, pointY, CIRCLE_CHART_RADIUS, paintCircle)
                    }
                } else if (point.type == ChartTypeEnum.BAR) {
                    drawRect(0f, 0f, point.x, baseHeight, shadowRectPaint)
                    drawRect(point.x + point.barSize, 0f, baseWidth, baseHeight, shadowRectPaint)
                }

                // draw legend rect
                this.drawRect(
                    newLegendData!!.position.x,
                    newLegendData!!.position.y,
                    newLegendData!!.position.x + newLegendData!!.sizes.width,
                    newLegendData!!.position.y + newLegendData!!.sizes.height,
                    legendFillRectPaint
                )

                // draw chevron
                if (chartData.xValuesIn == XValuesEnum.X_DAYS) {
                    val xChevron = newLegendData!!.position.x + newLegendData!!.sizes.width - pxLegendTextPadding
                    val yChevron = newLegendData!!.position.y + pxLegendTextPadding
                    this.drawLine(
                        xChevron,
                        yChevron + 15,
                        xChevron - 15,
                        yChevron,
                        paintTopBottomLine
                    )
                    this.drawLine(
                        xChevron,
                        yChevron + 15,
                        xChevron - 15,
                        yChevron + 30,
                        paintTopBottomLine
                    )
                }

                // draw legend title (week/day month/year and shevron icon)
                this.drawText(
                    newLegendData!!.weekTextDate.text,
                    newLegendData!!.weekTextDate.x,
                    newLegendData!!.weekTextDate.y,
                    textLegendTitlePaint
                )
                this.drawText(
                    newLegendData!!.monthTextDate.text,
                    newLegendData!!.monthTextDate.x,
                    newLegendData!!.monthTextDate.y,
                    textLegendTitlePaint
                )

                // draw legend text according to yvalue
                for (index in 0 until newLegendData!!.names.size) {
                    val name = newLegendData!!.names[index]
                    val number = newLegendData!!.numbers[index]
                    textLegendNumberPaint.color = number.color
                    this.drawText(
                        number.text,
                        number.x,
                        number.y,
                        textLegendNumberPaint
                    )
                    this.drawText(name.text, name.x, name.y, textLegendNamePaint)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun recalculateYScales() {
        if (chartData.yScaled) {
            yMinMaxValues.clear()
            yScales.clear()
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]
                val min = yValue.yValues.subList(startIndex, stopIndex).min()!!
                val max = yValue.yValues.subList(startIndex, stopIndex).max()!!
                yMinMaxValues.put(yIndex, MinMaxValues(min, max))
                val yScale = baseHeight / (max - min).toFloat()
                yScales.put(yIndex, yScale)
            }
        } else {
            val minY = getMinYAccordingToVisibility(startIndex, stopIndex)
            val maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
            val yScale = baseHeight / (maxY - minY).toFloat()
            yMinMaxValues.clear()
            yMinMaxValues.put(0, MinMaxValues(minY, maxY))
            yScales.clear()
            yScales.put(0, yScale)
        }
    }

    private fun calculateLegendRect(point: Distance, heightText: Int, bound: Rect): LegendSizes {
        var heightLegendBox = bound.height().toFloat() + pxLegendTextPadding * 2 + pxLegendTextLinePadding
        val widthLegendBox = 200 * metrics.density //bound.width().toFloat() + pxLegendTextPaddingHorizontal * 2
        // TODO point.ys.filter{}.count() ??
        for (index in 0 until point.ys.size) {
            if (!yShouldVisible[index]!!) continue
            heightLegendBox += heightText + if (index == point.ys.size - 1) 0f else pxLegendTextLinePadding
        }
        return LegendSizes(widthLegendBox, heightLegendBox)
    }

    private fun calculateLegendRectPosition(point: Distance, widthLegendBox: Float): LegendPosition {
        var leftX = 10f
        if (point.type == ChartTypeEnum.LINE) {
            leftX = point.x - widthLegendBox / 2
            if (leftX + widthLegendBox > baseWidth) {
                leftX = baseWidth - widthLegendBox - 10f
            }
            if (leftX <= 0) {
                leftX = 10f
            }
        } else if (point.type == ChartTypeEnum.BAR) {
            leftX = point.x - widthLegendBox - point.barSize * 1.5f
            if (leftX <= 0) {
                leftX = point.x + point.barSize * 2f
            }
            if (leftX + widthLegendBox > baseWidth) {
                leftX = baseWidth - widthLegendBox - 10f
            }
        }
        return LegendPosition(leftX, 10f)
    }

    private fun showLegend(x: Float, y: Float) {

        if (isShow && newLegendData != null) {
            if (x in newLegendData!!.position.x..newLegendData!!.position.x + newLegendData!!.sizes.width
                && y in newLegendData!!.position.y..newLegendData!!.position.y + newLegendData!!.sizes.height
            ) {
                isShow = false
                invalidate()
                onLegendClicked?.onLegendClicked(nearestPoint!!)
                return
            }
        }

        nearestPoint = findMinimalDistancePoint(x, y)
        isShow = nearestPoint != null

        val legendData = calculateLegendData(nearestPoint)
        if (lastLegendData != null) {
            newLegendData = legendData
        } else {
            lastLegendData = legendData
            newLegendData = legendData
        }
        invalidate()
    }

    private fun calculateLegendData(point: Distance?): LegendData? {
        if (point == null) return null

        // title of legend (week/day and month/year separatly)
        var weekTextDate = ""
        var monthTextDate = ""
        if (chartData.xValuesIn == XValuesEnum.X_DAYS) {
            weekTextDate = weekDateFormat.format(point.xDate).capitalize()
            textLegendTitlePaint.getTextBounds(weekTextDate, 0, weekTextDate.length, boundTitle)
            monthTextDate = monthDateFormat.format(point.xDate).capitalize()
            textLegendTitlePaint.getTextBounds(monthTextDate, 0, monthTextDate.length, boundTitle)
        } else {
            weekTextDate = hourDateFormat.format(point.xDate).capitalize()
            textLegendTitlePaint.getTextBounds(weekTextDate, 0, weekTextDate.length, boundTitle)
        }

        // height of legend's line
        val textLine0 = point.ys[0].yValues[point.xIndex].toString()
        textLegendTitlePaint.getTextBounds(textLine0, 0, textLine0.length, boundLine)
        val heightText = boundLine.height()

        // calculate of legend rect
        val sizes = calculateLegendRect(point, heightText, boundTitle)
        // calculate start position of legend rect
        val position = calculateLegendRectPosition(point, sizes.width)

        // calculate legend title (week/day month/year and shevron icon)
        var topTextY = position.y + pxLegendTextPadding + heightText
        textLegendTitlePaint.getTextBounds(weekTextDate, 0, weekTextDate.length, boundTitle)
        val xWeekTextDate = position.x + pxLegendTextPadding
        val yWeekTextDate = topTextY
        val xMonthTextDate = position.x + pxLegendTextPadding + boundTitle.width() + 20
        val yMonthTextDate = topTextY

        // calculate names and numbers positions
        val names = mutableListOf<LegendText>()
        val numbers = mutableListOf<LegendText>()

        topTextY += pxLegendTextLinePadding
        // draw legend text according to yvalue
        for (yIndex in 0 until point.ys.size) {
            if (!yShouldVisible[yIndex]!!) continue
            val yValue = point.ys[yIndex]
            textLegendNumberPaint.color = yValue.color
            topTextY += heightText //+ pxLegendTextLinePadding
            val numberText = formatNumber(yValue.yValues[point.xIndex])
            textLegendNumberPaint.getTextBounds(numberText, 0, numberText.length, boundLine)
            numbers.add(
                LegendText(
                    numberText,
                    position.x + sizes.width - pxLegendTextPadding - boundLine.width(),
                    topTextY,
                    yValue.color
                )
            )
            if (chartData.firstChartDataType() == ChartTypeEnum.AREA) {
                val prc = point.percents[yIndex]!!.toInt()

                names.add(LegendText("${prc}% ${yValue.name}", position.x + pxLegendTextPadding, topTextY))
            } else {
                names.add(LegendText(yValue.name, position.x + pxLegendTextPadding, topTextY))
            }
            topTextY += pxLegendTextLinePadding
        }

        return LegendData(
            sizes,
            position,
            boundTitle,
            LegendText(weekTextDate, xWeekTextDate, yWeekTextDate),
            LegendText(monthTextDate, xMonthTextDate, yMonthTextDate),
            names,
            numbers
        )
    }

    private fun findMinimalDistancePoint(x: Float, y: Float): Distance? {

        if (chartData.firstChartDataType() == ChartTypeEnum.AREA) {
            val point = chartLines.firstOrNull() { yShouldVisible[it.yIndex]!! }
            return if (point == null) null else Distance(
                0f,
                0f,
                point.xDate,
                point.x,
                point.xIndex,
                point.yScaled,
                point.ys,
                point.type,
                point.barSize,
                point.percents
            )
        }

        val distances = mutableListOf<Distance>()
        for (chartLine in chartLines.filter { yShouldVisible[it.yIndex]!! }) {

            if (chartLine.type == ChartTypeEnum.LINE) {
                distances.add(
                    Distance(
                        Math.abs(chartLine.x - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.yScaled,
                        chartLine.ys,
                        chartLine.type,
                        chartLine.barSize
                    )
                )
            } else if (chartLine.type == ChartTypeEnum.BAR) {
                distances.add(
                    Distance(
                        Math.abs(chartLine.x - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.yScaled,
                        chartLine.ys,
                        chartLine.type,
                        chartLine.barSize
                    )
                )
                distances.add(
                    Distance(
                        Math.abs(chartLine.x + chartLine.barSize - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.yScaled,
                        chartLine.ys,
                        chartLine.type,
                        chartLine.barSize
                    )
                )
            } else if (chartLine.type == ChartTypeEnum.AREA) {

            }
        }
        val nearestX = distances.sortedBy { it.distanceX }.filter { it.distanceX < BaseChart.MINIMAL_DISTANCE }
        if (nearestX.isNotEmpty()) {
            val firstNearest = nearestX.first()
            if (firstNearest.type == ChartTypeEnum.BAR) return firstNearest
            if (firstNearest.type == ChartTypeEnum.LINE) return nearestX.firstOrNull { it.distanceY < BaseChart.MINIMAL_DISTANCE }
        }
        return null
    }

    inner class LegendGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            e?.apply {
                showLegend(e.x, e.y)
            }
            return true
        }
    }

    data class Distance(
        val distanceX: Float,
        val distanceY: Float,
        val xDate: Date,
        val x: Float,
        val xIndex: Int,
        val yScaled: Boolean,
        val ys: List<YValue>,
        val type: ChartTypeEnum,
        val barSize: Float,
        val percents: Map<Int, Double> = emptyMap()
    )

    data class LegendSizes(
        val width: Float,
        val height: Float
    )

    data class LegendPosition(
        val x: Float,
        val y: Float
    )

    data class LegendText(
        val text: String,
        val x: Float,
        val y: Float,
        val color: Int = 0
    )

    data class LegendData(
        val sizes: LegendSizes,
        val position: LegendPosition,
        val boundTitle: Rect,
        val weekTextDate: LegendText,
        val monthTextDate: LegendText,
        val names: List<LegendText>,
        val numbers: List<LegendText>
    )

}