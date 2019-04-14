package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum

open class Chart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private var heightRow: Float = 0f

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

        val newChartLines = getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues)

        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            chartData,
            minX,
            maxX,
            xScale,
            yMinMaxValues,
            yScales,
            newChartLines,
            yShouldVisible
        )

        if (newChartLines.first().type == ChartTypeEnum.LINE) {
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
        } else {
            animationEnd(startIndex, stopIndex, newChartLines)
            invalidate()
        }
    }

    fun onStartIndexChanged(newStartIndex: Int) {
        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        chartLines.clear()
        chartLines.addAll(getChartLinesExt(chartData, newStartIndex, stopIndex, minX, maxX, yMinMaxValues))

        recalculateYScales()

        maxX = chartData.xValuesIn().subList(newStartIndex, stopIndex).max()!!
        minX = chartData.xValuesIn().subList(newStartIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(chartData, newStartIndex, stopIndex, minX, maxX, yMinMaxValues)
        onDataChange?.onDataChanged(
            newStartIndex,
            stopIndex,
            chartData,
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
        chartLines.addAll(getChartLinesExt(chartData, startIndex, newStopIndex, minX, maxX, yMinMaxValues))

        recalculateYScales()

        maxX = chartData.xValuesIn().subList(startIndex, newStopIndex).max()!!
        minX = chartData.xValuesIn().subList(startIndex, newStopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()

        val newChartLines = getChartLinesExt(chartData, startIndex, newStopIndex, minX, maxX, yMinMaxValues)
        onDataChange?.onDataChanged(
            startIndex,
            newStopIndex,
            chartData,
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
        calculateYScales()
    }

    private fun updateIndexes() {
        maxX = chartData.xValuesIn().subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesIn().subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        recalculateYScales()
        chartLines.clear()
        chartLines.addAll(getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues))
        onDataChange?.onDataChanged(
            startIndex,
            stopIndex,
            chartData,
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
        if (chartData.yScaled) {
            yMinMaxValues.clear()
            yScales.clear()
            for (yIndex in 0 until chartData.ys.size) {
                val yValue = chartData.ys[yIndex]
                val min = yValue.yValues.subList(startIndex, stopIndex).min()!!
                val max = yValue.yValues.subList(startIndex, stopIndex).max()!!
                yMinMaxValues.put(yIndex, MinMaxValues(min, max))
                val yScale = baseHeight / (max - min).toFloat()
                yScales.put(yIndex, yScale)
            }
        } else {
            val minY = getMinYAccordingToVisibility(startIndex, stopIndex)
            val maxY = getMaxYAccordingToVisibility(startIndex, stopIndex)
            val yScale = baseHeight / (maxY - minY).toFloat()
            yMinMaxValues.clear()
            yMinMaxValues.put(0, MinMaxValues(minY, maxY))
            yScales.clear()
            yScales.put(0, yScale)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            super.onDraw(this)
            drawYWithLegend(this)
        }
    }

    private fun animationEnd(startIndex: Int, stopIndex: Int, newChartLines: List<BaseChart.ChartLineExt>) {
        this.startIndex = startIndex
        this.stopIndex = stopIndex
        animationInProgress.set(false)
        chartLines.clear()
        chartLines.addAll(newChartLines)
        //invalidate()
    }

    // TODO to separate controls
    private fun drawYWithLegend(canvas: Canvas) {
        val boundRect = Rect()
        if (chartData.ys.first().type == ChartTypeEnum.AREA) {
            val rowHeight = (baseHeight / 4).toLong()

            canvas.drawText("0", 0f, baseHeight, textPaint)
            var prcText = 25
            for (index in 1 until 4) {
                canvas.drawLine(
                    0f,
                    baseHeight - rowHeight * index,
                    baseWidth,
                    baseHeight - rowHeight * index,
                    paintVerticalChartLine
                )
                canvas.drawText(prcText.toString(), 0f, baseHeight - rowHeight * index - 5, textPaint)
                prcText += 25
            }

        } else {
            for (index in 0 until yMinMaxValues.size step 2) {
                val minMax = yMinMaxValues[index]
                val step = (minMax!!.max - minMax.min) / ROWS

                var minMax1: MinMaxValues? = null
                var step1: Long? = null
                if (chartData.yScaled && (index + 1) < yMinMaxValues.size) {
                    minMax1 = yMinMaxValues[index + 1]
                    step1 = (minMax1!!.max - minMax1.min) / ROWS
                }

                textPaint.color = if (chartData.yScaled) chartData.ys[index].color else ContextCompat.getColor(
                    getContext(),
                    R.color.legend_xy
                )
                canvas.drawText("0", 0f, baseHeight, textPaint)

                if (chartData.yScaled && (index + 1) < yMinMaxValues.size) {
                    textPaint.getTextBounds("0", 0, 1, boundRect)
                    textPaint.color = chartData.ys[index + 1].color
                    canvas.drawText("0", baseWidth - boundRect.width() - 5, baseHeight, textPaint)
                }

                for (row in 1 until ROWS) {
                    val yRow = baseHeight - heightRow * row
                    canvas.drawLine(
                        0f,
                        yRow,
                        baseWidth,
                        yRow,
                        paintVerticalChartLine
                    )
                    textPaint.color = if (chartData.yScaled) chartData.ys[index].color else ContextCompat.getColor(
                        getContext(),
                        R.color.legend_xy
                    )
                    canvas.drawText(reduction(step * row + minMax.min), 0f, yRow - 10, textPaint)

                    if (chartData.yScaled && (index + 1) < yMinMaxValues.size) {
                        val text = reduction(step1!! * row + minMax1!!.min)
                        textPaint.getTextBounds(text, 0, text.length, boundRect)
                        textPaint.color = chartData.ys[index + 1].color
                        canvas.drawText(text, baseWidth - boundRect.width() - 5, yRow - 10, textPaint)
                    }
                }
            }
        }
    }

    fun updateData(chartData: ChartData, startIndex: Int, stopIndex: Int) {
        val oldChartType = this.chartData.ys.first().type
        val oldChartDataStacked = this.chartData.stacked
        val oldStartIndex = this.startIndex
        val oldStopIndex = this.stopIndex

        super.setData(chartData)
        this.startIndex = startIndex
        this.stopIndex = stopIndex

        maxX = chartData.xValuesIn().subList(startIndex, stopIndex).max()!!
        minX = chartData.xValuesIn().subList(startIndex, stopIndex).min()!!
        xScale = baseWidth / (maxX - minX).toFloat()
        recalculateYScales()
        //calculateYScales()
        val newChartLines = getChartLinesExt(chartData, startIndex, stopIndex, minX, maxX, yMinMaxValues)

        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }
        val newChartType = chartData.ys.first().type
        val newChartDataStacked = chartData.stacked
        if (oldChartType == ChartTypeEnum.LINE && newChartType == ChartTypeEnum.LINE) {
            if (animationInProgress.compareAndSet(false, true)) {
                animatorSet = AnimatorSet()
                val animators =
                    getBarToBarAnimation(chartLines, oldStartIndex, oldStopIndex, newChartLines, startIndex, stopIndex)
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
        } else if (!oldChartDataStacked && oldChartType == ChartTypeEnum.BAR && newChartType == ChartTypeEnum.LINE) {
            if (animationInProgress.compareAndSet(false, true)) {
                setNewChartLines(newChartLines)
                animatorSet = AnimatorSet()
                val animators = mutableListOf<Animator>()
                val halfHeight = baseHeight / 2
                val halfWidth = baseWidth / 2
                val halfSize = chartLines.size / 2
                for (indexLine in 0 until chartLines.size) {
                    val chartLine = chartLines[indexLine]
                    val x1Animator =
                        if (indexLine <= halfSize) ValueAnimator.ofFloat(chartLine.x, chartLine.x - 1000)
                        else ValueAnimator.ofFloat(chartLine.x, chartLine.x + 1000)
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(x1Animator)
                    val x2Animator =
                        if (indexLine <= halfSize) ValueAnimator.ofFloat(chartLine.x2, chartLine.x2 - 1000)
                        else ValueAnimator.ofFloat(chartLine.x2, chartLine.x2 + 1000)
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    x2Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(x2Animator)
                    val paintAnimator = ValueAnimator.ofInt(255, 0)
                    paintAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.alpha = this as Int
                            invalidate()
                        }
                    }
                    paintAnimator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(paintAnimator)

                    val y1Animator = ValueAnimator.ofFloat(chartLine.y, halfHeight)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    y1Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(y1Animator)
                    val y2Animator = ValueAnimator.ofFloat(chartLine.y2, halfHeight)
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y2 = this as Float
                            invalidate()
                        }
                    }
                    y2Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(y2Animator)
                }
                for (indexLine in 0 until newChartLines.size) {
                    val chartLine = newChartLines[indexLine]

                    val paintAnimator = ValueAnimator.ofInt(0, 255)
                    paintAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.alpha = this as Int
                            invalidate()
                        }
                    }
                    paintAnimator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(paintAnimator)

                    val y1Animator = ValueAnimator.ofFloat(halfHeight, chartLine.y)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    y1Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(y1Animator)

                    val x1Animator = ValueAnimator.ofFloat(halfWidth, chartLine.x)
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    x1Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(x1Animator)
                }
                animatorSet?.apply {
                    this.playTogether(animators)
                    //this.duration = ANIMATION_REPLACING_DURATION
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
        } else if (!newChartDataStacked && newChartType == ChartTypeEnum.BAR && oldChartType == ChartTypeEnum.LINE) {
            if (animationInProgress.compareAndSet(false, true)) {
                animatorSet = AnimatorSet()
                val animators = mutableListOf<Animator>()
                val halfHeight = baseHeight / 2
                val halfWidth = baseWidth / 2

                for (indexLine in 0 until chartLines.size) {
                    val chartLine = chartLines[indexLine]

                    val paintAnimator = ValueAnimator.ofInt(255, 0)
                    paintAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.alpha = this as Int
                            invalidate()
                        }
                    }
                    paintAnimator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(paintAnimator)

                    val y1Animator = ValueAnimator.ofFloat(chartLine.y, halfHeight)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    y1Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(y1Animator)

                    val x1Animator = ValueAnimator.ofFloat(chartLine.x, halfWidth)
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    x1Animator.duration = ANIMATION_REPLACING_DURATION
                    animators.add(x1Animator)
                }

                setNewChartLines(newChartLines)
                val halfSize = newChartLines.size / 2
                for (indexLine in 0 until newChartLines.size) {
                    val chartLine = newChartLines[indexLine]
                    val x1Animator =
                        if (indexLine <= halfSize) ValueAnimator.ofFloat(chartLine.x - 1000, chartLine.x)
                        else ValueAnimator.ofFloat(chartLine.x + 1000, chartLine.x)
                    x1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x = this as Float
                            invalidate()
                        }
                    }
                    x1Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(x1Animator)

                    val x2Animator =
                        if (indexLine <= halfSize) ValueAnimator.ofFloat(chartLine.x2 - 1000, chartLine.x2)
                        else ValueAnimator.ofFloat(chartLine.x2 + 1000, chartLine.x2)
                    x2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.x2 = this as Float
                            invalidate()
                        }
                    }
                    x2Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(x2Animator)

                    val paintAnimator = ValueAnimator.ofInt(0, 255)
                    paintAnimator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.paint.alpha = this as Int
                            invalidate()
                        }
                    }
                    paintAnimator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(paintAnimator)

                    val y1Animator = ValueAnimator.ofFloat(halfHeight, chartLine.y)
                    y1Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y = this as Float
                            invalidate()
                        }
                    }
                    y1Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(y1Animator)

                    val y2Animator = ValueAnimator.ofFloat(halfHeight, chartLine.y2)
                    y2Animator.addUpdateListener { value ->
                        value.animatedValue?.apply {
                            chartLine.y2 = this as Float
                            invalidate()
                        }
                    }
                    y2Animator.duration = ANIMATION_REPLACING_DURATION_FASTER
                    animators.add(y2Animator)
                }
                animatorSet?.apply {
                    this.playTogether(animators)
                    //this.duration = ANIMATION_REPLACING_DURATION
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
                animatorSet = AnimatorSet()
                val animators =
                    getBarToBarAnimation(chartLines, oldStartIndex, oldStopIndex, newChartLines, startIndex, stopIndex)
                animatorSet?.apply {
                    this.playTogether(animators)
                    //this.duration = ANIMATION_REPLACING_DURATION
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