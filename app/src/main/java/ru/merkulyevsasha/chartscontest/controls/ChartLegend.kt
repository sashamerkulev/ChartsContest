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
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.YValue
import java.text.SimpleDateFormat
import java.util.*


class ChartLegend @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

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
    private val pathCornerRect: Path
    private val bound: Rect

    private val pattern = "EEE, MMM dd"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private val gestureDetector = GestureDetectorCompat(getContext(), GestureListener())

    init {
        val metrics = resources.displayMetrics
        bound = Rect()
        pxLegendTextPaddingHorizontal =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, metrics)
        pxLegendTextPaddingVertical =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, metrics)

        paintTopBottomLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTopBottomLine.style = Paint.Style.STROKE
        paintTopBottomLine.color = ContextCompat.getColor(context, R.color.border_transparent)
        paintTopBottomLine.strokeWidth = CIRCLE_CHART_STOKE_WIDTH

        paintCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintCircle.strokeWidth = CIRCLE_CHART_STOKE_WIDTH
        paintCircle.style = Paint.Style.STROKE
        paintCircle.color = ContextCompat.getColor(getContext(), R.color.border)

        paintFillCircle = Paint(Paint.ANTI_ALIAS_FLAG)
        paintFillCircle.strokeWidth = CIRCLE_CHART_STOKE_WIDTH
        paintFillCircle.style = Paint.Style.FILL_AND_STROKE
        paintFillCircle.color = ContextCompat.getColor(getContext(), R.color.white)

        textBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textBlackPaint.strokeWidth = CHART_STOKE_WIDTH
        textBlackPaint.style = Paint.Style.FILL_AND_STROKE
        textBlackPaint.color = ContextCompat.getColor(getContext(), R.color.black)
        textBlackPaint.textSize = TEXT_SIZE_DP * metrics.density

        textLegendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textLegendPaint.strokeWidth = CHART_STOKE_WIDTH
        textLegendPaint.style = Paint.Style.FILL_AND_STROKE
        textLegendPaint.color = ContextCompat.getColor(getContext(), R.color.black)
        textLegendPaint.textSize = TEXT_SIZE_DP * metrics.density

        val cornerPathEffect10 = CornerPathEffect(10f)
        legendRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendRectPaint.strokeWidth = LEGEND_RECT_STOKE_WIDTH
        legendRectPaint.style = Paint.Style.STROKE
        legendRectPaint.color = ContextCompat.getColor(getContext(), R.color.border)
        legendRectPaint.pathEffect = cornerPathEffect10

        legendFillRectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        legendFillRectPaint.strokeWidth = CHART_STOKE_WIDTH
        legendFillRectPaint.style = Paint.Style.FILL_AND_STROKE
        legendFillRectPaint.color = ContextCompat.getColor(getContext(), R.color.white)
        legendFillRectPaint.pathEffect = cornerPathEffect10

        pathCornerRect = Path()
    }

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
        invalidate()
    }

    fun onStartIndexChanged(startIndex: Int) {
        this.startIndex = startIndex
        maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        isShow = false
        invalidate()

        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
    }

    fun onIndexesChanged(startIndex: Int, stopIndex: Int) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        isShow = false
        invalidate()

        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
    }

    fun onYDataSwitched(index: Int, isChecked: Boolean) {
        yShouldVisible[index] = isChecked

        minY = getMinYAccordingToVisibility()
        maxY = getMaxYAccordingToVisibility()
        yScale = baseHeight / (maxY - minY).toFloat()

        isShow = false
        invalidate()

        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
    }

    override fun onMeasureEnd() {
        baseHeight -= 80
        yScale = baseHeight / (maxY - minY).toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
            minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
            xScale = baseWidth / (maxX - minX).toFloat()
            chartLines.clear()
            chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))

            if (!isShow) return

            nearestPoint?.let { point ->
                // vertical line
                this.drawLine(point.x, 0f, point.x, baseHeight, paintTopBottomLine)

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
                // draw circles
                for (index in 0 until point.ys.size) {
                    if (!yShouldVisible[index]!!) continue
                    val yValue = point.ys[index]
                    val pointY = baseHeight - (yValue.yValues[point.xIndex] - minY) * yScale
                    paintCircle.color = yValue.color
                    this.drawCircle(point.x, pointY, 20f, paintFillCircle)
                    this.drawCircle(point.x, pointY, 20f, paintCircle)
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
        if (nearestPoint == null && isShow) {
            isShow = false
            invalidate()
            return
        }
        isShow = true
        invalidate()
    }

    private fun findMinimalDistancePoint(x: Float, y: Float): Distances? {
        val distances = mutableListOf<Distances>()
        for (chartLine in chartLines.filter { yShouldVisible[it.index]!! }) {
            distances.add(
                Distances(
                    Math.abs(chartLine.x1 - x),
                    Math.abs(chartLine.y1 - y),
                    chartLine.xValue,
                    chartLine.x1,
                    chartLine.xIndex,
                    chartLine.ys
                )
            )
            distances.add(
                Distances(
                    Math.abs(chartLine.x2 - x),
                    Math.abs(chartLine.y2 - y),
                    chartLine.xValue,
                    chartLine.x2,
                    chartLine.xIndex,
                    chartLine.ys
                )
            )
        }
        val nearestX = distances.sortedBy { it.distanceX }.filter { it.distanceX < MINIMAL_DISTANCE }
        return nearestX.sortedBy { it.distanceY }.firstOrNull { it.distanceY < MINIMAL_DISTANCE }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
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
        val ys: List<YValue>
    )
}