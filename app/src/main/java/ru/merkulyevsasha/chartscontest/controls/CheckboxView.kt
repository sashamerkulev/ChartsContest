package ru.merkulyevsasha.chartscontest.controls

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.view.View
import ru.merkulyevsasha.chartscontest.R

@SuppressLint("ViewConstructor")
class CheckboxView(
    context: Context,
    val title: String,
    var checked: Boolean,
    val color: Int
) : View(context) {

    private val titleBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleRect = Paint(Paint.ANTI_ALIAS_FLAG)

    private val titleBound = Rect()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleColor = Paint(Paint.ANTI_ALIAS_FLAG)
    private val padding: Float

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val checkSize: Float

    private var baseWidth: Float = 0f
    private var baseHeight: Float = 0f

    init {
        val metrics = resources.displayMetrics
        padding = 10f * metrics.density
        checkSize = 20 * metrics.density

        titlePaint.style = Paint.Style.FILL_AND_STROKE
        titlePaint.color = ContextCompat.getColor(getContext(), R.color.white)
        titlePaint.textSize = 10 * metrics.density
        titlePaint.strokeWidth = 1f

        titleColor.style = Paint.Style.FILL_AND_STROKE
        titleColor.color = color
        titleColor.textSize = 10 * metrics.density
        titleColor.strokeWidth = 1f

        titlePaint.getTextBounds(title, 0, title.length, titleBound)

        checkPaint.strokeWidth = BaseChart.CHART_STOKE_WIDTH
        checkPaint.style = Paint.Style.STROKE
        checkPaint.color = ContextCompat.getColor(getContext(), R.color.white)
        checkPaint.strokeWidth = 3f

        val cornerPathEffect20 = CornerPathEffect(60f)
        titleBackground.style = Paint.Style.FILL_AND_STROKE
        titleBackground.color = color
        titleBackground.pathEffect = cornerPathEffect20
        titleBackground.strokeWidth = 1f

        titleRect.style = Paint.Style.STROKE
        titleRect.color = color
        titleRect.pathEffect = cornerPathEffect20
        titleRect.strokeWidth = 5f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        baseWidth = checkSize + titleBound.width() + padding * 2
        baseHeight = 30 + padding * 2
        setMeasuredDimension(baseWidth.toInt() + 20, baseHeight.toInt() + 20)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            if (checked) {
                drawRect(
                    0f + EDGE_PADDING,
                    0f + EDGE_PADDING,
                    baseWidth - EDGE_PADDING,
                    baseHeight - EDGE_PADDING,
                    titleBackground
                )
                drawLine(
                    checkSize,
                    baseHeight / 2 + titleBound.height() / 2,
                    checkSize + padding / 2,
                    baseHeight / 2 - titleBound.height() / 2.toFloat(),
                    checkPaint
                )
                drawLine(
                    checkSize,
                    baseHeight / 2 + titleBound.height() / 2,
                    checkSize - padding / 4,
                    baseHeight / 2 - titleBound.height() / 5.toFloat(),
                    checkPaint
                )
                drawText(title, checkSize + padding, baseHeight / 2 + titleBound.height() / 2, titlePaint)
            } else {
                drawRect(
                    0f + EDGE_PADDING,
                    0f + EDGE_PADDING,
                    baseWidth - EDGE_PADDING,
                    baseHeight - EDGE_PADDING,
                    titleRect
                )
                drawText(
                    title,
                    baseWidth / 2 - titleBound.width() / 2,
                    baseHeight / 2 + titleBound.height() / 2,
                    titleColor
                )
            }
        }
    }

    fun setCheck(checked: Boolean) {
        this.checked = checked
        invalidate()
    }

    companion object {
        const val EDGE_PADDING = 5
    }

}