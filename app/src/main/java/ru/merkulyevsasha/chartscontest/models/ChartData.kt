package ru.merkulyevsasha.chartscontest.models

import java.util.*

data class ChartData(
    val title: String,
    val yScaled: Boolean,
    val percentage: Boolean,
    val stacked: Boolean,
    val xValues: List<Date>,
    private val xValuesInDays: List<Long>,
    private val xValuesInHours: List<Long>,
    private val xValuesInMinutes: List<Long>,
    val ys: List<YValue>,
    var xValuesIn: XValuesEnum = XValuesEnum.X_DAYS
) {
    private fun getMinInDays(): Long {
        return xValuesInDays.min()!!
    }

    private fun getMaxInDays(): Long {
        return xValuesInDays.max()!!
    }

    private fun getMinInHours(): Long {
        return xValuesInHours.min()!!
    }

    private fun getMaxInHours(): Long {
        return xValuesInHours.max()!!
    }

    private fun getMinInMinutes(): Long {
        return xValuesInMinutes.min()!!
    }

    private fun getMaxInMinutes(): Long {
        return xValuesInMinutes.max()!!
    }

    fun getMinX(): Long {
        return if (xValuesIn == XValuesEnum.X_DAYS) getMinInDays()
        else if (xValuesIn == XValuesEnum.X_HOURS) getMinInHours() else getMinInMinutes()
    }

    fun getMaxX(): Long {
        return if (xValuesIn == XValuesEnum.X_DAYS) getMaxInDays()
        else if (xValuesIn == XValuesEnum.X_HOURS) getMaxInHours() else getMaxInMinutes()
    }

    fun getMaxYs(): Long {
        return ys.map { it.yValues.max()!! }.max()!!
    }

    fun getMinYs(): Long {
        return ys.map { it.yValues.min()!! }.min()!!
    }

    fun xValuesIn(): List<Long> {
        return if (xValuesIn == XValuesEnum.X_DAYS) xValuesInDays
        else if (xValuesIn == XValuesEnum.X_HOURS) xValuesInHours else xValuesInMinutes
    }

    fun firstChartDataType(): ChartTypeEnum {
        return ys.first().type
    }
}

data class YValue(
    val yValues: List<Long>,
    val type: ChartTypeEnum,
    val name: String,
    val color: Int,
    val avg: Double = 0.0
)

enum class XValuesEnum {
    X_DAYS,
    X_HOURS,
    X_MINUTES
}

enum class ChartTypeEnum {
    LINE,
    BAR,
    AREA
}
