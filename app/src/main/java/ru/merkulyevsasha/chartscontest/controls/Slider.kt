package ru.merkulyevsasha.chartscontest.controls

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import ru.merkulyevsasha.chartscontest.R
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.XValuesEnum


class Slider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseChart(context, attrs, defStyleAttr) {

    private var x1: Float = 0f
    private var y1: Float = 0f
    private var x2: Float = 0f
    private var y2: Float = 0f
    private var delta: Float = 0f
    private var deltaMagic: Float = 0f
    private var distanceScrollX: Float = 0f
    private var distanceLeftBorderScrollX: Float = 0f
    private var distanceRightBorderScrollX: Float = 0f

    private var parts: Int = 30

    private var onActionIndicesChange: OnActionIndicesChange? = null

    private val gestureDetector = GestureDetectorCompat(getContext(), GestureListener())

    private var isLeftBorderMoving: Boolean = false
    private var isRightBorderMoving: Boolean = false
    private var isSquareMoving: Boolean = false

    private val paintWhiteLine: Paint
    private val paintBgr: Paint
    private val paintLeftRightRoundedBorder: Paint
    private val paintLeftRightBorder: Paint
    private val paintTopBottomBorder: Paint

    private var maxXIndex = 0

    init {
        val cornerPathEffect20 = CornerPathEffect(20f)

        paintWhiteLine = Paint(Paint.ANTI_ALIAS_FLAG)
        paintWhiteLine.style = Paint.Style.FILL_AND_STROKE
        paintWhiteLine.color = ContextCompat.getColor(context, R.color.white)
        paintWhiteLine.strokeWidth = 3f

        paintBgr = Paint(Paint.ANTI_ALIAS_FLAG)
        paintBgr.style = Paint.Style.FILL_AND_STROKE
        paintBgr.color = ContextCompat.getColor(context, R.color.bgrnd_out_slider)
        paintBgr.pathEffect = cornerPathEffect20
        paintBgr.strokeWidth = 1f

        paintLeftRightRoundedBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLeftRightRoundedBorder.style = Paint.Style.FILL_AND_STROKE
        paintLeftRightRoundedBorder.color = ContextCompat.getColor(context, R.color.border_slider)
        paintLeftRightRoundedBorder.pathEffect = cornerPathEffect20
        paintLeftRightRoundedBorder.strokeWidth = 1f

        paintLeftRightBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLeftRightBorder.style = Paint.Style.FILL_AND_STROKE
        paintLeftRightBorder.color = ContextCompat.getColor(context, R.color.border_slider)
        paintLeftRightBorder.strokeWidth = 1f

        paintTopBottomBorder = Paint(Paint.ANTI_ALIAS_FLAG)
        paintTopBottomBorder.style = Paint.Style.STROKE
        paintTopBottomBorder.color = ContextCompat.getColor(context, R.color.border_slider)
        paintTopBottomBorder.strokeWidth = TOP_BOTTOM_BORDER_WIDTH
    }

    override fun setData(chartData: ChartData) {
        super.setData(chartData)
        maxXIndex = chartData.xValuesIn().size
        parts = maxXIndex / if (chartData.xValuesIn == XValuesEnum.X_DAYS) 5 else 3
        initEndIndices()
    }

    fun setChangeIndexesCallback(onActionIndicesChange: OnActionIndicesChange) {
        this.onActionIndicesChange = onActionIndicesChange
        this.onActionIndicesChange?.onActionIndicesChanged(startIndex, stopIndex)
    }

    fun updateData(chartData: ChartData, newStartIndex: Int, newStopIndex: Int) {
        val oldStartIndex = startIndex
        val oldStopIndex = stopIndex
        val oldMaxIndex = this.chartData.xValuesIn().size
        super.setData(chartData)
        maxXIndex = chartData.xValuesIn().size
        parts = maxXIndex / if (chartData.xValuesIn == XValuesEnum.X_DAYS) 5 else 3
//        setData(chartData)
        calculateYScales()
        xScale = baseWidth / (maxX - minX).toFloat()

        if (animationInProgress.compareAndSet(true, false)) {
            animatorSet?.cancel()
            animatorSet = null
        }

        if (animationInProgress.compareAndSet(false, true)) {
            animatorSet = AnimatorSet()
            val animators = mutableListOf<Animator>()

            val aaa = oldMaxIndex.toFloat() / oldStartIndex.toFloat()
            val bbb = maxXIndex / aaa

            val aaa1 = oldMaxIndex.toFloat() / oldStopIndex.toFloat()
            val bbb1 = maxXIndex / aaa1

            val startIndexAnimator = ValueAnimator.ofInt(bbb.toInt(), newStartIndex)
            startIndexAnimator.addUpdateListener { value ->
                value.animatedValue?.apply {
                    startIndex = this as Int
                    invalidate()
                }
            }
            animators.add(startIndexAnimator)

            val stopIndexAnimator = ValueAnimator.ofInt(bbb1.toInt(), newStopIndex)
            stopIndexAnimator.addUpdateListener { value ->
                value.animatedValue?.apply {
                    stopIndex = this as Int
                    invalidate()
                }
            }
            animators.add(stopIndexAnimator)

            animatorSet?.apply {
                this.playTogether(animators)
                this.duration = ANIMATION_REPLACING_DURATION
                this.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        onAnimationEnd(startIndex, stopIndex)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        onAnimationEnd(startIndex, stopIndex)
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })
                this.start()
            }
        }
    }

    private fun onAnimationEnd(startIndex: Int, stopIndex: Int) {
        animationInProgress.set(false)
        this.startIndex = startIndex
        this.stopIndex = stopIndex
    }


    override fun onMeasureEnd() {
        calculateYScales()
        xScale = baseWidth / (maxX - minX).toFloat()
        y2 = baseHeight
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            if (!isInitialized()) return@apply

            val xDays = chartData.xValuesIn()

            x1 = (xDays[startIndex] - minX) * xScale
            x2 = (xDays[stopIndex - 1] - minX) * xScale
            delta = (x2 - x1) / (stopIndex - startIndex)
            deltaMagic = delta / MAGIC

            //this.drawRect(x1, y1, x2, y2, paintWhiteBgr)
            this.drawRect(0f, 0f, x1 + LEFT_RIGHT_BORDER_WIDTH, baseHeight, paintBgr)
            this.drawRect(x2 - LEFT_RIGHT_BORDER_WIDTH, 0f, baseWidth, baseHeight, paintBgr)

            this.drawLine(
                x1 + LEFT_RIGHT_BORDER_WIDTH / 2,
                y1,
                x2 - LEFT_RIGHT_BORDER_WIDTH / 2,
                y1,
                paintTopBottomBorder
            )
            this.drawLine(
                x1 + LEFT_RIGHT_BORDER_WIDTH / 2,
                y2,
                x2 - LEFT_RIGHT_BORDER_WIDTH / 2,
                y2,
                paintTopBottomBorder
            )

            this.drawRect(x1, y1, x1 + LEFT_RIGHT_BORDER_WIDTH, y2, paintLeftRightRoundedBorder)
            this.drawRect(x1 + LEFT_RIGHT_BORDER_WIDTH / 2, y1, x1 + LEFT_RIGHT_BORDER_WIDTH, y2, paintLeftRightBorder)
            this.drawLine(
                x1 + LEFT_RIGHT_BORDER_WIDTH / 2,
                y1 + 30,
                x1 + LEFT_RIGHT_BORDER_WIDTH / 2,
                y2 - 30,
                paintWhiteLine
            )

            this.drawRect(x2 - LEFT_RIGHT_BORDER_WIDTH, y1, x2, y2, paintLeftRightRoundedBorder)
            this.drawRect(x2 - LEFT_RIGHT_BORDER_WIDTH, y1, x2 - LEFT_RIGHT_BORDER_WIDTH / 2, y2, paintLeftRightBorder)
            this.drawLine(
                x2 - LEFT_RIGHT_BORDER_WIDTH / 2,
                y1 + 30,
                x2 - LEFT_RIGHT_BORDER_WIDTH / 2,
                y2 - 30,
                paintWhiteLine
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                isLeftBorderMoving = false
                isRightBorderMoving = false
                isSquareMoving = false
            }
        }
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun isSquare() = x1 + LEFT_RIGHT_BORDER_WIDTH..x2 - LEFT_RIGHT_BORDER_WIDTH
    private fun isLeftBorderRange() = x1 - LEFT_RIGHT_BORDER_WIDTH..x1 + LEFT_RIGHT_BORDER_WIDTH
    private fun isRightBorderRange() = x2 - LEFT_RIGHT_BORDER_WIDTH..x2 + LEFT_RIGHT_BORDER_WIDTH

    private fun initEndIndices() {
        startIndex = maxXIndex - parts
        stopIndex = maxXIndex
        distanceScrollX = 0f
        distanceLeftBorderScrollX = 0f
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            e1?.let { me1 ->
                e2?.let { me2 ->
                    if (!isSquareMoving) leftBorderMoving(e1, e2, distanceX)
                    if (!isSquareMoving) rightBorderMoving(e1, e2, distanceX)
                    if (!isLeftBorderMoving && !isRightBorderMoving) squareMoving(e1, e2, distanceX)
                }
            }
            return true
        }

        private fun squareMoving(e1: MotionEvent, e2: MotionEvent, distanceX: Float) {
            if ((e1.x in isSquare() || e2.x in isSquare()) && (e1.y in y1..y2 || e2.y in y1..y2) || isSquareMoving) {
                isSquareMoving = true
                distanceScrollX += distanceX
                if (distanceX > 0 && distanceScrollX > deltaMagic) {
                    startIndex -= Math.abs((distanceScrollX / deltaMagic)).toInt()
                    stopIndex -= Math.abs((distanceScrollX / deltaMagic)).toInt()
                    distanceScrollX = 0f
                } else if (distanceX < 0 && Math.abs(distanceScrollX) > deltaMagic) {
                    startIndex += Math.abs((distanceScrollX / deltaMagic)).toInt()
                    stopIndex += Math.abs((distanceScrollX / deltaMagic)).toInt()
                    distanceScrollX = 0f
                }
                if (startIndex < 0) {
                    distanceScrollX = 0f
                    startIndex = 0
                }
                if (stopIndex > maxXIndex) {
                    distanceScrollX = 0f
                    stopIndex = maxXIndex
                }
                if (stopIndex - startIndex < parts) {
                    if (startIndex == 0) {
                        stopIndex = startIndex + parts
                    } else if (stopIndex == maxXIndex) {
                        startIndex = stopIndex - parts
                    }
                }
                onActionIndicesChange?.onActionIndicesChanged(startIndex, stopIndex)
                invalidate()
            }
        }

        private fun leftBorderMoving(e1: MotionEvent, e2: MotionEvent, distanceX: Float) {
            if ((e1.x in isLeftBorderRange() || e2.x in isLeftBorderRange()) && (e1.y in y1..y2 || e2.y in y1..y2) || isLeftBorderMoving) {
                isLeftBorderMoving = true
                distanceLeftBorderScrollX += distanceX
                if (distanceX > 0 && distanceLeftBorderScrollX > deltaMagic) {
                    startIndex -= Math.abs((distanceLeftBorderScrollX / deltaMagic)).toInt()
                    distanceLeftBorderScrollX = 0f
                } else if (distanceX < 0 && Math.abs(distanceLeftBorderScrollX) > deltaMagic) {
                    startIndex += Math.abs((distanceLeftBorderScrollX / deltaMagic)).toInt()
                    distanceLeftBorderScrollX = 0f
                }
                if (startIndex < 0) {
                    distanceLeftBorderScrollX = 0f
                    startIndex = 0
                }
                if (stopIndex - startIndex < parts) {
                    startIndex = stopIndex - parts
                }
                onActionIndicesChange?.onActionStartIndexChanged(startIndex)
                invalidate()
            }
        }

        private fun rightBorderMoving(e1: MotionEvent, e2: MotionEvent, distanceX: Float) {
            if ((e1.x in isRightBorderRange() || e2.x in isRightBorderRange()) && (e1.y in y1..y2 || e2.y in y1..y2) || isRightBorderMoving) {
                isRightBorderMoving = true
                distanceRightBorderScrollX += distanceX
                if (distanceX > 0 && distanceRightBorderScrollX > deltaMagic) {
                    stopIndex -= Math.abs((distanceRightBorderScrollX / deltaMagic)).toInt()
                    distanceRightBorderScrollX = 0f
                } else if (distanceX < 0 && Math.abs(distanceRightBorderScrollX) > deltaMagic) {
                    stopIndex += Math.abs((distanceRightBorderScrollX / deltaMagic)).toInt()
                    distanceRightBorderScrollX = 0f
                }
                if (stopIndex > maxXIndex) {
                    distanceRightBorderScrollX = 0f
                    stopIndex = maxXIndex
                }
                if (stopIndex - startIndex < parts) {
                    stopIndex = startIndex + parts
                }
                onActionIndicesChange?.onActionStopIndexChanged(stopIndex)
                invalidate()
            }
        }
    }

}