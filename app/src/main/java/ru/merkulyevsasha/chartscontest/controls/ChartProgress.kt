package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum


class ChartProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
        invalidate()
    }

    override fun onMeasureEnd() {
        calculateYScales()
        xScale = baseWidth / (maxX - minX).toFloat()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues))
    }

    fun onYDataSwitched(index: Int, checked: Boolean) {
        yShouldVisible[index] = checked

        if (chartData.yScaled) {
            chartLines.clear()
            chartLines.addAll(getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues))
        } else {
            val minY = getMinYAccordingToVisibility(0, chartData.xValues.size - 1)
            val maxY = getMaxYAccordingToVisibility(0, chartData.xValues.size - 1)
            val yScale = baseHeight / (maxY - minY).toFloat()
            yMinMaxValues.clear()
            yMinMaxValues.put(0, MinMaxValues(minY, maxY))
            yScales.clear()
            yScales.put(0, yScale)
            chartLines.clear()
            chartLines.addAll(getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues))
        }

        for (indexLine in 0 until chartLines.size) {
            val chartLine = chartLines[indexLine]
            if (yShouldVisible[chartLine.yIndex]!!) {
                chartLine.paint.alpha = 100
            } else {
                chartLine.paint.alpha = 0
            }
        }

        invalidate()
    }

    fun updateData(chartData: ChartData) {
        val oldChartType = this.chartData.ys.first().type
        val oldChartDataStacked = this.chartData.stacked
        super.setData(chartData)

        minX = chartData.getMinX()
        maxX = chartData.getMaxX()
        xScale = baseWidth / (maxX - minX).toFloat()
        calculateYScales()
        val newChartLines = getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues)

        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        val newChartType = chartData.ys.first().type
        if (oldChartType == ChartTypeEnum.LINE && newChartType == ChartTypeEnum.LINE) {
            if (animationInProgress.compareAndSet(false, true)) {
                setNewChartLines(newChartLines)
                animatorSet = AnimatorSet()
                val animators = mutableListOf<Animator>()
                for (indexLine in 0 until chartLines.size) {
                    val chartLine = chartLines[indexLine]
                    val x1Animator =
                        if (newChartLines.size < chartLines.size) ValueAnimator.ofFloat(chartLine.x, chartLine.x - 1000)
                        else ValueAnimator.ofFloat(chartLine.x, chartLine.x + 1000)
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)
                }
                for (indexLine in 0 until newChartLines.size) {
                    val chartLine = newChartLines[indexLine]
                    val x1Animator =
                        if (newChartLines.size < chartLines.size) ValueAnimator.ofFloat(chartLine.x + 1000, chartLine.x)
                        else ValueAnimator.ofFloat(chartLine.x - 1000, chartLine.x)

                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    animators.add(x1Animator)
                }
                animatorSet?.apply {
                    this.playTogether(animators)
                    this.duration = ANIMATION_REPLACING_DURATION
                    this.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            onAnimationEnd(newChartLines)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            onAnimationEnd(newChartLines)
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                    this.start()
                }
            }
        } else if (oldChartType == ChartTypeEnum.BAR && newChartType == ChartTypeEnum.BAR) {
            if (animationInProgress.compareAndSet(false, true)) {
                //setNewChartLines(newChartLines)
                animatorSet = AnimatorSet()
                val animators = getBarToBarAnimation(chartLines, newChartLines)
                animatorSet?.apply {
                    this.playTogether(animators)
                    this.duration = ANIMATION_REPLACING_DURATION
                    this.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            onAnimationEnd(newChartLines)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            onAnimationEnd(newChartLines)
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                    this.start()
                }
            }
        } else {
            onAnimationEnd(newChartLines)
            invalidate()
        }
    }

    private fun onAnimationEnd(newChartLines: List<BaseChart.ChartLineExt>) {
        animationInProgress.set(false)
        chartLines.clear()
        chartLines.addAll(newChartLines)
        noChartLines = false
        setNewChartLines(null)
    }

}