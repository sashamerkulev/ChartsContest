package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
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
    private var widthColumn: Float = 0f

    private var startDate: Date = Date()
    private var stepInDays: Long = 0

    private val pattern = "dd MMM"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    private var edgeTextWidth: Float = 0f
    private var heightTextPadding: Int = 20

    private val animationInProgress = AtomicBoolean(false)
    private var animatorSet: AnimatorSet? = null

    fun onIndexesChanged(startIndex: Int, stopIndex: Int) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex

        startDate = chartData.xValues[startIndex]
        val startDay = chartData.xValuesInDays[startIndex]
        val stopDay = chartData.xValuesInDays[stopIndex - 1]

        stepInDays = (stopDay - startDay) / COLUMNS

        paintTextInfos.clear()
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        for (column in 0 until COLUMNS) {
            if (column > 0) calendar.add(Calendar.DAY_OF_YEAR, stepInDays.toInt())
            val text = dateFormat.format(calendar.time)
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            paintTextInfos.add(PaintTextInfo(text, bounds))
            if (column == 0 || column == COLUMNS - 1) {
                edgeTextWidth += bounds.width()
            }
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

    override fun onMeasureEnd() {
        baseHeight -= 80

        yScale = baseHeight / (maxY - minY).toFloat()
        heightRow = baseHeight / ROWS.toFloat()
        widthColumn = baseWidth / COLUMNS.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            if (animationInProgress.compareAndSet(false, false)) {
                maxX = chartData.xValuesInDays.subList(startIndex, stopIndex).max()!!
                minX = chartData.xValuesInDays.subList(startIndex, stopIndex).min()!!
                xScale = baseWidth / (maxX - minX).toFloat()
                chartLines.clear()
                chartLines.addAll(getChartLines2(startIndex, stopIndex, minX, maxX, minY, maxY))
            }
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
                    widthColumn * column,
                    height.toFloat() - heightTextPadding,
                    textPaint
                )
                else -> canvas.drawText(
                    paintTextInfos[column].text,
                    widthColumn * column + (widthColumn / 2 - paintTextInfos[column].bound.width() / 2),
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