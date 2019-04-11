package ru.merkulyevsasha.chartscontest.models

import java.util.*

data class ChartData(
    val title: String,
    val yScaled: Boolean,
    val percentage: Boolean,
    val stacked: Boolean,
    val xValues: List<Date>,
    val xValuesInDays: List<Long>,
    val ys: List<YValue>
) {
    fun getMinInDays(): Long {
        return xValuesInDays.min()!!
    }

    fun getMaxInDays(): Long {
        return xValuesInDays.max()!!
    }

    fun getMaxYs(): Long {
        return ys.map { it.yValues.max()!! }.max()!!
    }

    fun getMinYs(): Long {
        return ys.map { it.yValues.min()!! }.min()!!
    }
}

data class YValue(
    val yValues: List<Long>,
    val type: String,
    val name: String,
    val color: Int
)