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

    private var startMoving: Boolean = false
    private var startXAction: Float = 0f
    private var startYAction: Float = 0f
    private var parts: Int = 30

    private lateinit var onActionIndicesChange: OnActionIndicesChange

    private val gestureDetector = GestureDetectorCompat(getContext(), GestureListener())

    fun setData(chartData: ChartData, onActionIndicesChange: OnActionIndicesChange) {
        super.setData(chartData)
        this.onActionIndicesChange = onActionIndicesChange
        parts = chartData.xValuesInDays.size / 4

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

            x1 = (xDays[startIndex] - minX) * xScale + LEFT_RIGHT_BORDER_WIDTH / 2
            x2 = (xDays[stopIndex - 1] - minX) * xScale - LEFT_RIGHT_BORDER_WIDTH / 2
            delta = (x2 - x1) / (stopIndex - startIndex)

            this.drawRect(x1, y1, x2, y2, paintWhiteBgr)
            this.drawRect(0f, 0f, x1, baseHeight, paintBgr)
            this.drawRect(x2, 0f, baseWidth, baseHeight, paintBgr)

            this.drawLine(x1, y1, x2, y1, paintTopBottomBorder)
            this.drawLine(x1, y2, x2, y2, paintTopBottomBorder)

            this.drawLine(x1, y1, x1, y2, paintLeftRightBorder)
            this.drawLine(x2, y1, x2, y2, paintLeftRightBorder)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        gestureDetector.onTouchEvent(event)
//        return true
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                startXAction = event.x
                startYAction = event.y

                if (startXAction in isFullSquare() && startYAction in y1..y2) {
                    startMoving = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val moveXAction = event.x
                val moveYAction = event.y
                if (startMoving) {
                    onMoving(moveXAction, moveYAction, true)
                }
                invalidate()
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                val stopXAction = event.x
                val stopYAction = event.y
                if (startMoving) {
                    onMoving(stopXAction, stopYAction, false)
                }
                invalidate()
            }
        }
        return true
    }

    private fun isFullSquare() = x1 - LEFT_RIGHT_BORDER_WIDTH / 2..x2 + LEFT_RIGHT_BORDER_WIDTH / 2
    private fun isSquare() = x1 + LEFT_RIGHT_BORDER_WIDTH / 2..x2 - LEFT_RIGHT_BORDER_WIDTH / 2
    private fun isLeftBorderRange() = x1 - LEFT_RIGHT_BORDER_WIDTH / 2..x1 + LEFT_RIGHT_BORDER_WIDTH / 2
    private fun isRightBorderRange() = x2 - LEFT_RIGHT_BORDER_WIDTH / 2..x2 + LEFT_RIGHT_BORDER_WIDTH / 2

    private fun onMoving(xAction: Float, yAction: Float, startMoving: Boolean) {
        if (isSquareMoving(xAction, yAction)) {
            if (xAction > startXAction && delta(xAction, startXAction)) {
                startIndex += 1
                stopIndex += 1
            } else if (xAction < startXAction && delta(xAction, startXAction)) {
                startIndex -= 1
                stopIndex -= 1
            }
            if (startIndex < 0) initBeginIndices()
            if (stopIndex > chartData.xValuesInDays.size) initEndIndices()

            startXAction = xAction
            startYAction = yAction

            onActionIndicesChange.onActionIndicesChanged(startIndex, stopIndex)

        } else if (isLeftBorderMoving(xAction, yAction)) {
            System.out.println(
                String.format(
                    "TOUCH x1=%s   x2=%s  xAction=%s    startXAction=%s",
                    x1,
                    x2,
                    xAction,
                    startXAction
                )
            )
            if (xAction > startXAction && delta(xAction, startXAction)) {
                startIndex += 1
            } else if (xAction < startXAction && delta(xAction, startXAction)) {
                startIndex -= 1
            }
            if (startIndex < 0) initBeginIndices()
            if (stopIndex > chartData.xValuesInDays.size) initEndIndices()
            if (stopIndex - startIndex < parts) startIndex = stopIndex - parts

            startXAction = xAction
            startYAction = yAction

            onActionIndicesChange.onActionStartIndexChanged(startIndex)
        }
        this.startMoving = startMoving
    }

    private fun isLeftBorderMoving(xAction: Float, yAction: Float): Boolean {
        return xAction in isLeftBorderRange() && yAction in y1..y2
    }

    private fun isRightBorderMoving(xAction: Float, yAction: Float): Boolean {
        return xAction in isRightBorderRange() && yAction in y1..y2
    }

    private fun isSquareMoving(xAction: Float, yAction: Float): Boolean {
        return xAction in isSquare() && yAction in y1..y2
    }

    private fun delta(x1: Float, x2: Float): Boolean {
        return true//(Math.abs(x1 - x2)) > delta / 2
    }

    private fun initEndIndices() {
        startIndex = chartData.xValues.size - parts
        stopIndex = chartData.xValues.size
    }

    private fun initBeginIndices() {
        startIndex = 0
        stopIndex = parts
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            //System.out.println(String.format("e1.x= %s - e2.x= %s - velocityX= %s", e1?.x, e2?.x, velocityX))
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            System.out.println(
                String.format(
                    "e1.x= %s - e2.x= %s - distanceX= %s - delta=%s",
                    e1?.x,
                    e2?.x,
                    distanceX,
                    delta
                )
            )

            e1?.let {
                if (it.x in x1..x2) {
                    if (distanceX > 0 /*&& Math.abs(distanceX) >= delta*/) {
                        startIndex--
                        stopIndex--
                    }

                    if (distanceX < 0 /*&& Math.abs(distanceX) >= delta*/) {
                        startIndex++
                        stopIndex++
                    }

                    if (startIndex < 0) initBeginIndices()
                    if (stopIndex > chartData.xValuesInDays.size) initEndIndices()

                    invalidate()
                }
            }
            return true
        }
    }

}