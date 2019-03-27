package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.models.ChartData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    internal val paintTextInfos = mutableListOf<PaintTextInfo>()

    private var heightRow: Float = 0f

    private val pattern = "dd MMM"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private var heightTextPadding: Int = 20

    private val animationInProgress = AtomicBoolean(false)
    private var animatorSet: AnimatorSet? = null

    private var onDataChange: OnDataChange? = null

    fun onIndexesChanged(startIndex: Int, stopIndex: Int) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex

        updateIndexes()

        val startDate = chartData.xValues[startIndex]
        val stopDate = chartData.xValues[stopIndex - 1]
        val startDay = chartData.xValuesInDays[startIndex]
        val stopDay = chartData.xValuesInDays[stopIndex - 1]

        val stepInDays = (stopDay - startDay) / COLUMNS

        paintTextInfos.clear()
        val calendar = Calendar.getInstance()
        for (column in 0 until COLUMNS) {
            if (column == 0) calendar.time = startDate
            else if (column == COLUMNS - 1) calendar.time = stopDate
            else if (column > 0) calendar.time = chartData.xValues[startIndex + column * stepInDays.toInt()]

            val text = dateFormat.format(calendar.time)

            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)

            paintTextInfos.add(PaintTextInfo(text, bounds))
        }
        invalidate()
    }

    fun onYDataSwitched(index: Int, isChecked: Boolean) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        yShouldVisible[index] = isChecked

        minY = getMinYAccordingToVisibility()
        maxY = getMaxYAccordingToVisibility()
        yScale = baseHeight / (maxY - minY).toFloat()

        val newChartLines = getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY)

        onDataChange?.onDataChanged(minX, minY, maxX, maxY, xScale, yScale, newChartLines, yShouldVisible)


        if (animationInProgress.compareAndSet(false, true)) {
            animatorSet = AnimatorSet()
            val animators = mutableListOf<Animator>()
            for (indexLine in 0 until chartLines.size) {
                val chartLine = chartLines[indexLine]
                val newChartLine = newChartLines[indexLine]
                if (yShouldVisible[chartLine.index]!!) {
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
                    val y1Animator = ValueAnimator.ofFloat(chartLine.y1, newChartLine.y1)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y1 = this as Float
                            invalidate()
                        }
                    }
                    val y2Animator = ValueAnimator.ofFloat(chartLine.y2, newChartLine.y2)
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y2 = this as Float
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
        chartLines.addAll(getChartLines2(newStartIndex, stopIndex, minX, maxX, minY, maxY))

        maxX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(newStartIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLines2(newStartIndex, stopIndex, minX, maxX, minY, maxY)
        onDataChange?.onDataChanged(minX, minY, maxX, maxY, xScale, yScale, newChartLines, yShouldVisible)

        if (animationInProgress.compareAndSet(false, true)) {
            animatorSet = AnimatorSet()
            val animators = mutableListOf<Animator>()
            for (indexLine in chartLines.size - 1 downTo 0) {
                val chartLine = chartLines[indexLine]
                val newChartLine = newChartLines[indexLine]
                val x1Animator = ValueAnimator.ofFloat(chartLine.x1, newChartLine.x1)
                x1Animator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        chartLine.x1 = this as Float
                        invalidate()
                    }
                }
                animators.add(x1Animator)
                val x2Animator = ValueAnimator.ofFloat(chartLine.x2, newChartLine.x2)
                x2Animator.addUpdateListener { value ->
                    value.animatedValue?.apply {
                        chartLine.x2 = this as Float
                        invalidate()
                    }
                }
                animators.add(x2Animator)
            }
            animatorSet?.apply {
                this.playTogether(animators)
                this.duration = ANIMATION_DURATION
                this.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        animationEnd(newStartIndex, stopIndex, newChartLines)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        animationEnd(newStartIndex, stopIndex, newChartLines)
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })
                this.start()
            }
        }
    }

    fun setData(chartData: ChartData, onDataChange: OnDataChange) {
        super.setData(chartData)
        this.onDataChange = onDataChange
    }

    override fun onMeasureEnd() {
        baseHeight -= 80
        heightRow = baseHeight / ROWS.toFloat()

        updateIndexes()
    }

    private fun updateIndexes() {
        maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        yScale = baseHeight / (maxY - minY).toFloat()
        chartLines.clear()
        chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
        onDataChange?.onDataChanged(minX, minY, maxX, maxY, xScale, yScale, chartLines, yShouldVisible)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawYWithLegend(this)
            drawXWithLegend(this)
            super.onDraw(this)
        }
    }

    private fun animationEnd(startIndex: Int, stopIndex: Int, newChartLines: List<ChartLine>) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        animationInProgress.set(false)
        chartLines.clear()
        chartLines.addAll(newChartLines)
        //invalidate()
    }

    private fun drawXWithLegend(canvas: Canvas) {

        val leftWidth = baseWidth - paintTextInfos[0].bound.width() - paintTextInfos[COLUMNS - 1].bound.width()
        val legendWidth = leftWidth / (COLUMNS - 1)

        for (column in 0 until COLUMNS) {
            when (column) {
                COLUMNS - 1 -> canvas.drawText(
                    paintTextInfos[column].text,
                    baseWidth - paintTextInfos[column].bound.width(),
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
                0 -> canvas.drawText(
                    paintTextInfos[column].text,
                    0f,
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
                else -> canvas.drawText(
                    paintTextInfos[column].text,
                    legendWidth * column + (legendWidth / 2 - paintTextInfos[column].bound.width() / 2),
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
            }
        }
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
                paintTopBottomBorder
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
        return reductionValue.toString()
    }

    data class PaintTextInfo(val text: String, val bound: Rect)
}