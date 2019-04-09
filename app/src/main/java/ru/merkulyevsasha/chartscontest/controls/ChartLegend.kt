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

    private val pxLegendTextPaddingHorizontal: Float
    private val pxLegendTextPaddingVertical: Float

    private var nearestPoint: Distances? = null
    private val paintTopBottomLine: Paint
    private val paintCircle: Paint
    private val paintFillCircle: Paint
    private val legendRectPaint: Paint
    private val legendFillRectPaint: Paint
    private val textBlackPaint: Paint
    private val textLegendPaint: Paint
    private val shadowRectPaint: Paint
    private val pathCornerRect: Path
    private val bound: Rect

    private val pattern = "EEE, MMM dd"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private val gestureDetector = GestureDetectorCompat(getContext(), LegendGestureListener())

    init {
        val metrics = resources.displayMetrics
        bound = Rect()
        pxLegendTextPaddingHorizontal =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, metrics)
        pxLegendTextPaddingVertical =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, metrics)

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
        paintFillCircle.color = ContextCompat.getColor(getContext(), R.color.bgrnd_legend)

        textBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textBlackPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textBlackPaint.style = Paint.Style.FILL_AND_STROKE
        textBlackPaint.color = ContextCompat.getColor(getContext(), R.color.black)
        textBlackPaint.textSize = BaseChart.TEXT_SIZE_DP * metrics.density

        textLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        textLegendPaint.style = Paint.Style.FILL_AND_STROKE
        textLegendPaint.color = ContextCompat.getColor(getContext(), R.color.black)
        textLegendPaint.textSize = BaseChart.TEXT_SIZE_DP * metrics.density

        val cornerPathEffect10 = CornerPathEffect(20f)
        legendRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendRectPaint.strokeWidth = BaseChart.LEGEND_RECT_STOKE_WIDTH
        legendRectPaint.style = Paint.Style.STROKE
        legendRectPaint.color = ContextCompat.getColor(getContext(), R.color.border)
        legendRectPaint.pathEffect = cornerPathEffect10

        legendFillRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendFillRectPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        legendFillRectPaint.style = Paint.Style.FILL_AND_STROKE
        legendFillRectPaint.color = ContextCompat.getColor(getContext(), R.color.bgrnd_legend)
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
                // vertical line
                if (point.type == "line") {
                    this.drawLine(point.x, 0f, point.x, baseHeight, paintTopBottomLine)
                } else if (point.type == "bar") {

                }

                // title of legend (date)
                val textDate = dateFormat.format(point.xDate).capitalize()
                textBlackPaint.getTextBounds(textDate, 0, textDate.length, bound)

                // calculate of legend rect
                var heightLegendBox = bound.height().toFloat() + pxLegendTextPaddingVertical * 2
                var widthLegendBox = bound.width().toFloat() + pxLegendTextPaddingHorizontal * 2
                for (index in 0 until point.ys.size) {
                    if (!yShouldVisible[index]!!) continue
                    val yValue = point.ys[index]
                    val text = yValue.name + " " + yValue.yValues[point.xIndex].toString()
                    textLegendPaint.getTextBounds(text, 0, text.length, bound)
                    heightLegendBox += bound.height() + pxLegendTextPaddingVertical * 2
                    widthLegendBox = Math.max(widthLegendBox, bound.width() + pxLegendTextPaddingHorizontal * 2)
                }
                // calculate start position of legend rect
                var leftX = point.x - widthLegendBox + pxLegendTextPaddingHorizontal
                if (leftX + widthLegendBox > baseWidth) {
                    leftX = point.x - widthLegendBox - pxLegendTextPaddingHorizontal
                }
                if (leftX <= 0) {
                    leftX = pxLegendTextPaddingHorizontal
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

                var topX = 10f
                // draw legend rect
                this.drawRect(leftX, topX, leftX + widthLegendBox, topX + heightLegendBox, legendFillRectPaint)
                this.drawRect(leftX, topX, leftX + widthLegendBox, topX + heightLegendBox, legendRectPaint)
                topX += pxLegendTextPaddingVertical
                textBlackPaint.getTextBounds(textDate, 0, textDate.length, bound)
                this.drawText(textDate, leftX + pxLegendTextPaddingHorizontal, topX + bound.height(), textBlackPaint)
                topX += bound.height()
                // draw legend text according to yvalue
                for (index in 0 until point.ys.size) {
                    if (!yShouldVisible[index]!!) continue
                    val yValue = point.ys[index]
                    textLegendPaint.color = yValue.color
                    val text = yValue.name + " " + yValue.yValues[point.xIndex].toString()
                    textLegendPaint.getTextBounds(textDate, 0, textDate.length, bound)
                    topX += bound.height() + pxLegendTextPaddingVertical
                    this.drawText(text, leftX + pxLegendTextPaddingHorizontal, topX, textLegendPaint)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun showLegend(x: Float, y: Float) {
        nearestPoint = findMinimalDistancePoint(x, y)
        isShow = nearestPoint != null
        invalidate()
    }

    private fun findMinimalDistancePoint(x: Float, y: Float): Distances? {
        val distances = mutableListOf<Distances>()
        for (chartLine in chartLines.filter { yShouldVisible[it.yIndex]!! }) {

            if (chartLine.type == "line") {
                distances.add(
                    Distances(
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
                    Distances(
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
                    Distances(
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

    data class Distances(
        val distanceX: Float,
        val distanceY: Float,
        val xDate: Date,
        val x: Float,
        val xIndex: Int,
        val ys: List<YValue>,
        val type: String
    )
}