package ru.merkulyevsasha.chartscontest.controls

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData

open class BaseChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    internal lateinit var chartData: ChartData
    internal var xScale: Float = 1f
    internal var yScale: Float = 1f
    internal var maxY: Long = 0
    internal var minY: Long = 0
    internal var maxX: Long = 0
    internal var minX: Long = 0

    internal val textPaint: Paint

    internal val paintTopBottomBorder: Paint
    internal val paintLeftRightBorder: Paint
    internal val paintBgr: Paint
    internal val paintWhiteBgr: Paint

    internal var baseWidth: Float = 0f
    internal var baseHeight: Float = 0f

    internal var startIndex: Int = 0
    internal var stopIndex: Int = 0

    internal val paints = mutableMapOf<String, Paint>()
    internal val draw = mutableMapOf<String, Boolean>()

    internal val chartLines = mutableListOf<ChartLine>()

    init {
        val metrics = resources.displayMetrics

        paintTopBottomBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTopBottomBorder.style = Paint.Style.STROKE
        paintTopBottomBorder.color = ContextCompat.getColor(context, R.color.border_transparent)
        paintTopBottomBorder.strokeWidth = TOP_BOTTOM_BORDER_WIDTH

        paintLeftRightBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLeftRightBorder.style = Paint.Style.STROKE
        paintLeftRightBorder.color = ContextCompat.getColor(context, R.color.border_transparent)
        paintLeftRightBorder.strokeWidth = LEFT_RIGHT_BORDER_WIDTH

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.strokeWidth = TEXT_STROKE_WIDTH
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.color = ContextCompat.getColor(getContext(), R.color.legend)
        textPaint.textSize = TEXT_SIZE_DP * metrics.density

        paintBgr = Paint(Paint.ANTI_ALIAS_FLAG)
        paintBgr.style = Paint.Style.FILL
        paintBgr.color = ContextCompat.getColor(context, R.color.bgrnd)

        paintWhiteBgr = Paint(Paint.ANTI_ALIAS_FLAG)
        paintWhiteBgr.style = Paint.Style.FILL
        paintWhiteBgr.color = ContextCompat.getColor(context, R.color.white_transparency)
    }

    open fun setData(chartData: ChartData) {
        this.chartData = chartData
        maxY = chartData.getMaxYs()
        minY = chartData.getMinYs()
        maxX = chartData.getMaxInDays()
        minX = chartData.getMinInDays()

        startIndex = 0
        stopIndex = chartData.xValuesInDays.size

        paints.clear()
        for (ys in chartData.ys) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = ys.color
            paint.strokeWidth = CHART_STOKE_WIDTH
            paints.put(ys.name, paint)
            draw.put(ys.name, true)
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

        onMeasureEnd()
    }

    open fun onMeasureEnd() {

    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            var startX: Long = 0
            val startY = mutableListOf<Long>()
            for (index in startIndex until stopIndex) {
                val xVal = chartData.xValuesInDays[index]
                if (index == startIndex) {
                    startX = xVal
                    startY.clear()
                    for (yVal in chartData.ys) {
                        startY.add(yVal.yValues[index])
                    }
                } else {
                    val x1 = (startX - minX) * xScale
                    val x2 = (xVal - minX) * xScale

                    val startY2 = mutableListOf<Long>()
                    startY.forEachIndexed { index1, yVal1 ->
                        val y1 = baseHeight - (yVal1 - minY) * yScale
                        val y2 = baseHeight - (chartData.ys[index1].yValues[index] - minY) * yScale
                        this.drawLine(x1, y1, x2, y2, paints[chartData.ys[index1].name]!!)
                        startY2.add(chartData.ys[index1].yValues[index])
                    }
                    startX = xVal
                    startY.clear()
                    startY.addAll(startY2)
                }
            }
        }
    }

    internal fun getChartLines2(): List<ChartLine> {
        val result = mutableListOf<ChartLine>()
        return result
    }

    data class ChartLine(val x1: Float, val y1: List<Float>, val x2: Float, val y2: List<Float>)

    companion object {
        const val ROWS = 6
        const val COLUMNS = 5
        const val TEXT_SIZE_DP = 12
        const val LEFT_RIGHT_BORDER_WIDTH = 20f
        const val TOP_BOTTOM_BORDER_WIDTH = 3f
        const val CHART_STOKE_WIDTH = 3f
        const val TEXT_STROKE_WIDTH = 1f
    }

}