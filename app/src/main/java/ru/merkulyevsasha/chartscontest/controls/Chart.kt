package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
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

        recalculateYScales()

        val newChartLines = getChartLinesExt(startIndex, stopIndex, minX, maxX, yMinMaxValues)

        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            minX,
            maxX,
            xScale,
            yMinMaxValues,
            yScales,
            newChartLines,
            yShouldVisible
        )

        if (animationInProgress.compareAndSet(false, true)) {
            animatorSet = AnimatorSet()
            val animators = mutableListOf<Animator>()
            for (indexLine in 0 until chartLines.size) {
                val chartLine = chartLines[indexLine]
                val newChartLine = newChartLines[indexLine]
                if (yShouldVisible[chartLine.yIndex]!!) {
                    if (chartLine.paint.alpha == 0) {
                        val paintAnimator = ValueAnimator.ofInt(0, 255)
                        paintAnimator.addUpdateListener { value ->
                            value.animatedValue?.apply {
                                chartLine.paint.alpha = this as Int
                                invalidate()
                            }
                        }
                        animators.add(paintAnimator)
                    }
                    val y1Animator = ValueAnimator.ofFloat(chartLine.y, newChartLine.y)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    val y2Animator = ValueAnimator.ofFloat(chartLine.y, newChartLine.y)
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    animators.add(y1Animator)
                    animators.add(y2Animator)
                } else {
                    val paintAnimator = ValueAnimator.ofInt(255, 0)
                    paintAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.alpha = this as Int
                            invalidate()
                        }
                    }
                    animators.add(paintAnimator)
                }
            }
            animatorSet?.apply {
                this.playTogether(animators)
                this.duration = ANIMATION_DURATION
                this.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        animationEnd(startIndex, stopIndex, newChartLines)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        animationEnd(startIndex, stopIndex, newChartLines)
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })
                this.start()
            }
        }
    }

    fun onStartIndexChanged(newStartIndex: Int) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        chartLines.clear()
        chartLines.addAll(getChartLinesExt(newStartIndex, stopIndex, minX, maxX, yMinMaxValues))

        recalculateYScales()

        maxX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(newStartIndex, stopIndex, minX, maxX, yMinMaxValues)
        onDataChange?.onDataChanged(
            newStartIndex,
            stopIndex,
            minX,
            maxX,
            xScale,
            yMinMaxValues,
            yScales,
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
        chartLines.addAll(getChartLinesExt(startIndex, newStopIndex, minX, maxX, yMinMaxValues))

        recalculateYScales()

        maxX = chartData.xValuesInDays.subList(startIndex, newStopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, newStopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(startIndex, newStopIndex, minX, maxX, yMinMaxValues)
        onDataChange?.onDataChanged(
            startIndex,
            newStopIndex,
            minX,
            maxX,
            xScale,
            yMinMaxValues,
            yScales,
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
        maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        recalculateYScales()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(startIndex, stopIndex, minX, maxX, yMinMaxValues))
        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            minX,
            maxX,
            xScale,
            yMinMaxValues,
            yScales,
            chartLines,
            yShouldVisible
        )
    }

    private fun recalculateYScales() {
        if (!chartData.yScaled) {
            val minY = getMinYAccordingToVisibility(startIndex, stopIndex)
            val maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
            val yScale = baseHeight / (maxY - minY).toFloat()
            yMinMaxValues.clear()
            yMinMaxValues.add(MinMaxValues(minY, maxY))
            yScales.clear()
            yScales.add(yScale)
        }
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
        for (index in 0 until yMinMaxValues.size step 2) {
            val minMax = yMinMaxValues[index]
            val step = (minMax.max - minMax.min) / ROWS
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
                canvas.drawText(reduction(yText + minMax.min), 0f, yRow - 20, textPaint)
                yText += step
            }
        }
    }

}