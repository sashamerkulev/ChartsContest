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

    fun getMinX(): Long {
        return if (xValuesIn == XValuesEnum.X_DAYS) getMinInDays() else getMinInHours()
    }

    fun getMaxX(): Long {
        return if (xValuesIn == XValuesEnum.X_DAYS) getMaxInDays() else getMaxInHours()
    }

    fun getMaxYs(): Long {
        return ys.map { it.yValues.max()!! }.max()!!
    }

    fun getMinYs(): Long {
        return ys.map { it.yValues.min()!! }.min()!!
    }

    fun xValuesIn(): List<Long> {
        return if (xValuesIn == XValuesEnum.X_DAYS) xValuesInDays else xValuesInHours
    }

}

data class YValue(
    val yValues: List<Long>,
    val type: String,
    val name: String,
    val color: Int,
    val avg: Double = 0.0
)

enum class XValuesEnum {
    X_DAYS,
    X_HOURS
}