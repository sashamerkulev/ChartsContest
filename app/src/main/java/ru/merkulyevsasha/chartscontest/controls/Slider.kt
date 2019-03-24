package ru.merkulyevsasha.chartscontest.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.support.v4.view.GestureDetectorCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import ru.merkulyevsasha.chartscontest.models.ChartData


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
    private var distanceLeftScrollX: Float = 0f
    private var distanceRightScrollX: Float = 0f
    private var distanceBorderLeftScrollX: Float = 0f
    private var distanceBorderRightScrollX: Float = 0f

    private var parts: Int = 30

    private lateinit var onActionIndicesChange: OnActionIndicesChange

    private val gestureDetector = GestureDetectorCompat(getContext(), GestureListener())

    fun setData(chartData: ChartData, onActionIndicesChange: OnActionIndicesChange) {
        super.setData(chartData)
        this.onActionIndicesChange = onActionIndicesChange
        parts = chartData.xValuesInDays.size / 3

        initEndIndices()
        onActionIndicesChange.onActionIndicesChanged(startIndex, stopIndex)
    }

    override fun onMeasureEnd() {
        yScale = baseHeight / (maxY - minY).toFloat()
        xScale = baseWidth / (maxX - minX).toFloat()

        y2 = baseHeight
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            val xDays = chartData.xValuesInDays

            x1 = (xDays[startIndex] - minX) * xScale
            x2 = (xDays[stopIndex - 1] - minX) * xScale
            delta = (x2 - x1) / (stopIndex - startIndex)

            this.drawRect(x1, y1, x2, y2, paintWhiteBgr)
            this.drawRect(0f, 0f, x1, baseHeight, paintBgr)
            this.drawRect(x2, 0f, baseWidth, baseHeight, paintBgr)

            this.drawLine(x1, y1, x2, y1, paintTopBottomBorder)
            this.drawLine(x1, y2, x2, y2, paintTopBottomBorder)

            this.drawRect(x1, y1, x1 + LEFT_RIGHT_BORDER_WIDTH, y2, paintLeftRightBorder)
            this.drawRect(x2 - LEFT_RIGHT_BORDER_WIDTH, y1, x2, y2, paintLeftRightBorder)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun isSquare() = x1 + LEFT_RIGHT_BORDER_WIDTH..x2 - LEFT_RIGHT_BORDER_WIDTH
    private fun isLeftBorderRange() = x1..x1 + LEFT_RIGHT_BORDER_WIDTH
    private fun isRightBorderRange() = x2 - LEFT_RIGHT_BORDER_WIDTH..x2

    private fun initEndIndices() {
        startIndex = chartData.xValues.size - parts
        stopIndex = chartData.xValues.size
        distanceLeftScrollX = 0f
        distanceRightScrollX = 0f
        distanceBorderLeftScrollX = 0f
        distanceBorderRightScrollX = 0f
    }

    private fun initBeginIndices() {
        startIndex = 0
        stopIndex = parts
        distanceLeftScrollX = 0f
        distanceRightScrollX = 0f
        distanceBorderLeftScrollX = 0f
        distanceBorderRightScrollX = 0f
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            e1?.let { me1 ->
                e2?.let { me2 ->
                    if ((e1.x in isSquare() || e2.x in isSquare()) && e1.y in y1..y2) {
                        distanceLeftScrollX += distanceX
                        distanceRightScrollX += distanceX
                        if (distanceX > 0 && distanceLeftScrollX > delta / 2) {
                            distanceLeftScrollX = 0f
                            startIndex--
                            stopIndex--
                        } else if (distanceX < 0 && Math.abs(distanceRightScrollX) > delta / 2) {
                            distanceRightScrollX = 0f
                            startIndex++
                            stopIndex++
                        }
                        if (startIndex < 0) initBeginIndices()
                        if (stopIndex > chartData.xValuesInDays.size) initEndIndices()
                        onActionIndicesChange.onActionIndicesChanged(startIndex, stopIndex)
                        invalidate()
                    }
                    if ((e1.x in isLeftBorderRange() || e2.x in isLeftBorderRange()) && e1.y in y1..y2) {
                        distanceBorderLeftScrollX += distanceX
                        distanceBorderRightScrollX += distanceX
                        if (distanceX > 0 && distanceBorderLeftScrollX > delta / 2) {
                            distanceBorderLeftScrollX = 0f
                            startIndex -= 1
                        } else if (distanceX < 0 && Math.abs(distanceBorderRightScrollX) > delta / 2) {
                            distanceBorderRightScrollX = 0f
                            startIndex += 1
                        }
                        if (startIndex < 0) initBeginIndices()
                        if (stopIndex > chartData.xValuesInDays.size) initEndIndices()
                        if (stopIndex - startIndex < parts) startIndex = stopIndex - parts
                        onActionIndicesChange.onActionStartIndexChanged(startIndex)
                        invalidate()
                    }
                }
            }
            return true
        }
    }

}