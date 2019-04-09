package ru.merkulyevsasha.chartscontest.controls

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.R
import java.util.concurrent.atomic.AtomicBoolean

open class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private var heightRow: Float = 0f

    private val animationInProgress = AtomicBoolean(false)
    private var animatorSet: AnimatorSet? = null

    private var onDataChange: OnDataChange? = null

    private val textPaint: Paint
    private val paintVerticalChartLine: Paint

    init {
        val metrics = resources.displayMetrics
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.strokeWidth = TEXT_STROKE_WIDTH
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.color = ContextCompat.getColor(getContext(), R.color.legend_xy)
        textPaint.textSize = TEXT_SIZE_DP * metrics.density

        paintVerticalChartLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintVerticalChartLine.style = Paint.Style.STROKE
        paintVerticalChartLine.color = ContextCompat.getColor(context, R.color.legend_xy)
        paintVerticalChartLine.strokeWidth = TOP_BOTTOM_BORDER_WIDTH
    }

    fun onIndexesChanged(startIndex: Int, stopIndex: Int) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        updateIndexes()
        invalidate()
    }

    fun onYDataSwitched(index: Int, isChecked: Boolean) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        yShouldVisible[index] = isChecked

        minY = getMinYAccordingToVisibility(startIndex, stopIndex)
        maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
        yScale = baseHeight / (maxY - minY).toFloat()

        val newChartLines = getChartLinesExt(startIndex, stopIndex, minX, maxX, minY, maxY)

        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            minX,
            minY,
            maxX,
            maxY,
            xScale,
            yScale,
            newChartLines,
            yShouldVisible
        )

        if (animationInProgress.compareAndSet(false, true)) {
//            animatorSet = AnimatorSet()
//            val animators = mutableListOf<Animator>()
//            for (indexLine in 0 until chartLines.size) {
//                val chartLine = chartLines[indexLine]
//                val newChartLine = newChartLines[indexLine]
//                if (yShouldVisible[chartLine.index]!!) {
//                    if (chartLine.paint.alpha == 0) {
//                        val paintAnimator = ValueAnimator.ofInt(0, 255)
//                        paintAnimator.addUpdateListener { value ->
//                            value.animatedValue?.apply {
//                                chartLine.paint.alpha = this as Int
//                                invalidate()
//                            }
//                        }
//                        animators.add(paintAnimator)
//                    }
//                    val y1Animator = ValueAnimator.ofFloat(chartLine.y1, newChartLine.y1)
//                    y1Animator.addUpdateListener { value ->
//                        value.animatedValue?.apply {
//                            chartLine.y1 = this as Float
//                            invalidate()
//                        }
//                    }
//                    val y2Animator = ValueAnimator.ofFloat(chartLine.y2, newChartLine.y2)
//                    y2Animator.addUpdateListener { value ->
//                        value.animatedValue?.apply {
//                            chartLine.y2 = this as Float
//                            invalidate()
//                        }
//                    }
//                    animators.add(y1Animator)
//                    animators.add(y2Animator)
//                } else {
//                    val paintAnimator = ValueAnimator.ofInt(255, 0)
//                    paintAnimator.addUpdateListener { value ->
//                        value.animatedValue?.apply {
//                            chartLine.paint.alpha = this as Int
//                            invalidate()
//                        }
//                    }
//                    animators.add(paintAnimator)
//                }
//            }
//            animatorSet?.apply {
//                this.playTogether(animators)
//                this.duration = ANIMATION_DURATION
//                this.addListener(object : Animator.AnimatorListener {
//                    override fun onAnimationRepeat(animation: Animator?) {
//                    }
//
//                    override fun onAnimationEnd(animation: Animator?) {
//                        animationEnd(startIndex, stopIndex, newChartLines)
//                    }
//
//                    override fun onAnimationCancel(animation: Animator?) {
//                        animationEnd(startIndex, stopIndex, newChartLines)
//                    }
//
//                    override fun onAnimationStart(animation: Animator?) {
//                    }
//                })
//                this.start()
//            }
            animationEnd(startIndex, stopIndex, newChartLines)
        }
    }

    fun onStartIndexChanged(newStartIndex: Int) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        chartLines.clear()
        chartLines.addAll(getChartLinesExt(newStartIndex, stopIndex, minX, maxX, minY, maxY))

        minY = getMinYAccordingToVisibility(startIndex, stopIndex)
        maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
        yScale = baseHeight / (maxY - minY).toFloat()

        maxX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(newStartIndex, stopIndex, minX, maxX, minY, maxY)
        onDataChange?.onDataChanged(
            newStartIndex,
            stopIndex,
            minX,
            minY,
            maxX,
            maxY,
            xScale,
            yScale,
            newChartLines,
            yShouldVisible
        )
        animationEnd(newStartIndex, stopIndex, newChartLines)
        invalidate()
    }

    fun onStopIndexChanged(newStopIndex: Int) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        chartLines.clear()
        chartLines.addAll(getChartLinesExt(startIndex, newStopIndex, minX, maxX, minY, maxY))

        minY = getMinYAccordingToVisibility(startIndex, stopIndex)
        maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
        yScale = baseHeight / (maxY - minY).toFloat()

        maxX = chartData.xValuesInDays.subList(startIndex, newStopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, newStopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(startIndex, newStopIndex, minX, maxX, minY, maxY)
        onDataChange?.onDataChanged(
            startIndex,
            newStopIndex,
            minX,
            minY,
            maxX,
            maxY,
            xScale,
            yScale,
            newChartLines,
            yShouldVisible
        )
        animationEnd(startIndex, newStopIndex, newChartLines)
        invalidate()
    }

    fun setDataChangeCallback(onDataChange: OnDataChange) {
        this.onDataChange = onDataChange
    }

    override fun onMeasureEnd() {
        heightRow = baseHeight / ROWS.toFloat()

        updateIndexes()
    }

    private fun updateIndexes() {
        minY = getMinYAccordingToVisibility(startIndex, stopIndex)
        maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
        yScale = baseHeight / (maxY - minY).toFloat()
        maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        yScale = baseHeight / (maxY - minY).toFloat()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(startIndex, stopIndex, minX, maxX, minY, maxY))
        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            minX,
            minY,
            maxX,
            maxY,
            xScale,
            yScale,
            chartLines,
            yShouldVisible
        )
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            super.onDraw(this)
            drawYWithLegend(this)
        }
    }

    private fun animationEnd(startIndex: Int, stopIndex: Int, newChartLines: List<ChartLineExt>) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        animationInProgress.set(false)
        chartLines.clear()
        chartLines.addAll(newChartLines)
        //invalidate()
    }

    private fun drawYWithLegend(canvas: Canvas) {
        val step = (maxY - minY) / ROWS
        var yText = step
        canvas.drawText("0", 0f, baseHeight, textPaint)
        for (row in 1 until ROWS) {
            val yRow = baseHeight - heightRow * row
            canvas.drawLine(
                0f,
                yRow,
                width.toFloat(),
                yRow,
                paintVerticalChartLine
            )
            canvas.drawText(reduction(yText + minY), 0f, yRow - 20, textPaint)
            yText += step
        }
    }

    private fun reduction(value: Long): String {
        var reductionValue = value
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