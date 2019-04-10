package ru.merkulyevsasha.chartscontest.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.controls.BaseChart.Companion.BAR_SIZE
import ru.merkulyevsasha.chartscontest.controls.BaseChart.Companion.CIRCLE_CHART_RADIUS
import ru.merkulyevsasha.chartscontest.models.YValue
import java.text.SimpleDateFormat
import java.util.*


class ChartLegend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var baseWidth: Float = 0f
    private var baseHeight: Float = 0f

    private var xScale: Float = 1f
    private var yScale: Float = 1f
    private var maxY: Long = 0
    private var minY: Long = 0
    private var maxX: Long = 0
    private var minX: Long = 0
    private var startIndex: Int = 0
    private var stopIndex: Int = 0
    private val yShouldVisible = mutableMapOf<Int, Boolean>()
    private val chartLines = mutableListOf<BaseChart.ChartLineExt>()

    private var isShow: Boolean = false

    private val pxLegendTextPadding: Float
    private val pxLegendTextLinePadding: Float

    private var nearestPoint: Distance? = null
    private val paintTopBottomLine: Paint
    private val paintCircle: Paint
    private val paintFillCircle: Paint
    private val legendRectPaint: Paint
    private val legendFillRectPaint: Paint
    private val shadowRectPaint: Paint
    private val pathCornerRect: Path

    private val boundLine = Rect()
    private val boundTitle = Rect()

    private val textLegendTitlePaint: Paint
    private val textLegendNamePaint: Paint
    private val textLegendNumberPaint: Paint


    private val weekPattern = "EEE, dd"
    private val weekDateFormat: SimpleDateFormat = SimpleDateFormat(weekPattern, Locale.getDefault())

    private val monthPattern = "MMM yyyy"
    private val monthDateFormat: SimpleDateFormat = SimpleDateFormat(monthPattern, Locale.getDefault())

    private val gestureDetector = GestureDetectorCompat(getContext(), LegendGestureListener())

    private val metrics = resources.displayMetrics

    private var lastLegendData: LegendData? = null
    private var newLegendData: LegendData? = null

    init {
        pxLegendTextPadding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, metrics)
        pxLegendTextLinePadding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, metrics)

        paintTopBottomLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTopBottomLine.style = Paint.Style.STROKE
        paintTopBottomLine.color = ContextCompat.getColor(context, R.color.chart_line)
        paintTopBottomLine.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH

        paintCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintCircle.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH
        paintCircle.style = Paint.Style.STROKE
        paintCircle.color = ContextCompat.getColor(getContext(), R.color.border)

        paintFillCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintFillCircle.strokeWidth = BaseChart.CIRCLE_CHART_STOKE_WIDTH
        paintFillCircle.style = Paint.Style.FILL_AND_STROKE
        paintFillCircle.color = ContextCompat.getColor(getContext(), R.color.legend_bgrnd)

        textLegendTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendTitlePaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendTitlePaint.style = Paint.Style.FILL_AND_STROKE
        textLegendTitlePaint.color = ContextCompat.getColor(getContext(), R.color.legend_title)
        textLegendTitlePaint.textSize = 14 * metrics.density

        textLegendNamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendNamePaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendNamePaint.style = Paint.Style.FILL_AND_STROKE
        textLegendNamePaint.color = ContextCompat.getColor(getContext(), R.color.legend_title)
        textLegendNamePaint.textSize = 12 * metrics.density

        textLegendNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendNumberPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendNumberPaint.style = Paint.Style.FILL_AND_STROKE
        textLegendNumberPaint.textSize = 12 * metrics.density

        val cornerPathEffect10 = CornerPathEffect(20f)
        legendRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendRectPaint.strokeWidth = BaseChart.LEGEND_RECT_STOKE_WIDTH
        legendRectPaint.style = Paint.Style.STROKE
        legendRectPaint.color = ContextCompat.getColor(getContext(), R.color.border)
        legendRectPaint.pathEffect = cornerPathEffect10

        legendFillRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendFillRectPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        legendFillRectPaint.style = Paint.Style.FILL_AND_STROKE
        legendFillRectPaint.color = ContextCompat.getColor(getContext(), R.color.legend_bgrnd)
        legendFillRectPaint.pathEffect = cornerPathEffect10

        pathCornerRect = Path()

        shadowRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowRectPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        shadowRectPaint.style = Paint.Style.FILL_AND_STROKE
        shadowRectPaint.color = ContextCompat.getColor(getContext(), R.color.white_shadow)
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
        chartLines: List<BaseChart.ChartLineExt>,
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
        isShow = false
        lastLegendData = null
        newLegendData = null
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
                if (point.type == "line") {
                    this.drawLine(point.x, 0f, point.x, baseHeight, paintTopBottomLine)
                } else if (point.type == "bar") {

                }

                if (point.type == "line") {
                    // draw circles
                    for (index in 0 until point.ys.size) {
                        if (!yShouldVisible[index]!!) continue
                        val yValue = point.ys[index]
                        val pointY = baseHeight - (yValue.yValues[point.xIndex] - minY) * yScale
                        paintCircle.color = yValue.color
                        this.drawCircle(point.x, pointY, CIRCLE_CHART_RADIUS, paintFillCircle)
                        this.drawCircle(point.x, pointY, CIRCLE_CHART_RADIUS, paintCircle)
                    }
                } else if (point.type == "bar") {
                    drawRect(0f, 0f, point.x - BAR_SIZE / 2, baseHeight, shadowRectPaint)
                    drawRect(point.x + BAR_SIZE / 2, 0f, baseWidth, baseHeight, shadowRectPaint)
                }

                // draw legend rect
                this.drawRect(
                    newLegendData!!.position.x,
                    newLegendData!!.position.y,
                    newLegendData!!.position.x + newLegendData!!.sizes.width,
                    newLegendData!!.position.y + newLegendData!!.sizes.height,
                    legendFillRectPaint
                )
                this.drawRect(
                    newLegendData!!.position.x,
                    newLegendData!!.position.y,
                    newLegendData!!.position.x + newLegendData!!.sizes.width,
                    newLegendData!!.position.y + newLegendData!!.sizes.height,
                    legendRectPaint
                )

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
        if (point.type == "line") {
            leftX = point.x - widthLegendBox / 2
            if (leftX + widthLegendBox > baseWidth) {
                leftX = baseWidth - widthLegendBox - 10f
            }
            if (leftX <= 0) {
                leftX = 10f
            }
        } else if (point.type == "bar") {
            leftX = point.x - widthLegendBox - BAR_SIZE - BAR_SIZE / 2
            if (leftX <= 0) {
                leftX = point.x + BAR_SIZE + BAR_SIZE / 2
            }
            if (leftX + widthLegendBox > baseWidth) {
                leftX = baseWidth - widthLegendBox - 10f
            }
        }
        return LegendPosition(leftX, 10f)
    }

    private fun showLegend(x: Float, y: Float) {
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
        val weekTextDate = weekDateFormat.format(point.xDate).capitalize()
        textLegendTitlePaint.getTextBounds(weekTextDate, 0, weekTextDate.length, boundTitle)
        val monthTextDate = monthDateFormat.format(point.xDate).capitalize()
        textLegendTitlePaint.getTextBounds(monthTextDate, 0, monthTextDate.length, boundTitle)

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
        for (index in 0 until point.ys.size) {
            if (!yShouldVisible[index]!!) continue
            val yValue = point.ys[index]
            textLegendNumberPaint.color = yValue.color
            topTextY += heightText //+ pxLegendTextLinePadding
            val numberText = yValue.yValues[point.xIndex].toString()
            textLegendNumberPaint.getTextBounds(numberText, 0, numberText.length, boundLine)
            numbers.add(
                LegendText(
                    numberText,
                    position.x + sizes.width - pxLegendTextPadding - boundLine.width(),
                    topTextY,
                    yValue.color
                )
            )
            names.add(LegendText(yValue.name, position.x + pxLegendTextPadding, topTextY))
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
        val distances = mutableListOf<Distance>()
        for (chartLine in chartLines.filter { yShouldVisible[it.yIndex]!! }) {

            if (chartLine.type == "line") {
                distances.add(
                    Distance(
                        Math.abs(chartLine.x - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.ys,
                        chartLine.type
                    )
                )
            } else if (chartLine.type == "bar") {
                distances.add(
                    Distance(
                        Math.abs(chartLine.x - BAR_SIZE / 2 - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.ys,
                        chartLine.type
                    )
                )
                distances.add(
                    Distance(
                        Math.abs(chartLine.x + BAR_SIZE / 2 - x),
                        Math.abs(chartLine.y - y),
                        chartLine.xDate,
                        chartLine.x,
                        chartLine.xIndex,
                        chartLine.ys,
                        chartLine.type
                    )
                )
            }
        }
        val nearestX = distances.sortedBy { it.distanceX }.filter { it.distanceX < BaseChart.MINIMAL_DISTANCE }
        if (nearestX.isNotEmpty()) {
            val firstNearest = nearestX.first()
            if (firstNearest.type == "bar") return firstNearest
            if (firstNearest.type == "line") return nearestX.firstOrNull { it.distanceY < BaseChart.MINIMAL_DISTANCE }
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
        val ys: List<YValue>,
        val type: String
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