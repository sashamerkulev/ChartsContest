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

    internal lateinit var chartData: ChartData
    internal var xScale: Float = 1f
    internal var yScale: Float = 1f
    internal var maxY: Long = 0
    internal var minY: Long = 0
    internal var maxX: Long = 0
    internal var minX: Long = 0


    internal var baseWidth: Float = 0f
    internal var baseHeight: Float = 0f

    internal var startIndex: Int = 0
    internal var stopIndex: Int = 0

    internal val paints = mutableMapOf<String, Paint>()
    internal val yShouldVisible = mutableMapOf<Int, Boolean>()
    internal val chartLines = mutableListOf<ChartLine>()

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
            paint.style = Paint.Style.STROKE
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
            for (chartLine in chartLines) {
                drawLine(chartLine.x1, chartLine.y1, chartLine.x2, chartLine.y2, chartLine.paint)
            }
        }
    }

    internal fun isInitialized(): Boolean {
        return ::chartData.isInitialized
    }

    internal fun getChartLines2(
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        minY: Long,
        maxY: Long
    ): List<ChartLine> {
        val yScale = baseHeight / (maxY - minY).toFloat()
        val xScale = baseWidth / (maxX - minX).toFloat()

        val result = mutableListOf<ChartLine>()
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
                    val paint = paints[chartData.ys[index1].name]!!
                    //canvas.drawLine(x1, y1, x2, y2, paint)
                    result.add(ChartLine(index1, chartData.xValues[index], x1, y1, x2, y2, paint, index, chartData.ys))
                    startY2.add(chartData.ys[index1].yValues[index])
                }
                startX = xVal
                startY.clear()
                startY.addAll(startY2)
            }
        }
        return result
    }

    internal fun getMinYAccordingToVisibility(): Long {
        var result: Long = Long.MAX_VALUE
        for (index in 0 until chartData.ys.size) {
            if (!yShouldVisible[index]!!) continue
            result = Math.min(result, chartData.ys[index].yValues.min()!!)
        }
        return result
    }

    internal fun getMaxYAccordingToVisibility(): Long {
        var result: Long = Long.MIN_VALUE
        for (index in 0 until chartData.ys.size) {
            if (!yShouldVisible[index]!!) continue
            result = Math.max(result, chartData.ys[index].yValues.max()!!)
        }
        return result
    }

    data class ChartLine(
        val index: Int,
        val xValue: Date,
        var x1: Float,
        var y1: Float,
        var x2: Float,
        var y2: Float,
        val paint: Paint,
        val xIndex: Int,
        val ys: List<YValue>
    )

    companion object {
        const val ROWS = 6
        const val TEXT_SIZE_DP = 12
        const val LEFT_RIGHT_BORDER_WIDTH = 30f
        const val TOP_BOTTOM_BORDER_WIDTH = 3f
        const val CHART_STOKE_WIDTH = 3f
        const val CIRCLE_CHART_STOKE_WIDTH = 5f
        const val CIRCLE_CHART_RADIUS = 15f
        const val LEGEND_RECT_STOKE_WIDTH = 5f
        const val TEXT_STROKE_WIDTH = 1f
        const val ANIMATION_DURATION: Long = 300
        const val MINIMAL_DISTANCE = 50
        const val MAGIC = 1.1f
    }

}