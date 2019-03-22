package ru.merkulyevsasha.chartscontest.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
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

    private var startMoving: Boolean = false
    private var startXAction: Float = 0f
    private var startYAction: Float = 0f
    private var parts: Int = 30

    private lateinit var onActionIndicesChange: OnActionIndicesChange

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
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                startXAction = event.x
                startYAction = event.y

                if (startXAction in x1..x2 && startYAction in y1..y2) {
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

    private fun onMoving(xAction: Float, yAction: Float, startMoving: Boolean) {
        if (xAction in x1..x2 && yAction in y1..y2) {
            if (xAction > startXAction) {
                startIndex += 1
                stopIndex += 1
            } else if (xAction < startXAction) {
                startIndex -= 1
                stopIndex -= 1
            }
            if (startIndex < 0) initBeginIndices()
            if (stopIndex > chartData.xValuesInDays.size) initEndIndices()

            startXAction = xAction
            startYAction = yAction

            onActionIndicesChange.onActionIndicesChanged(startIndex, stopIndex)
        }
        this.startMoving = startMoving
    }

    private fun initEndIndices() {
        startIndex = chartData.xValues.size - parts
        stopIndex = chartData.xValues.size
    }

    private fun initBeginIndices() {
        startIndex = 0
        stopIndex = parts
    }
}