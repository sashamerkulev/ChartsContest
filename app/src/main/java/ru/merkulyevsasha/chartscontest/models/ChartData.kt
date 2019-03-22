package ru.merkulyevsasha.chartscontest.models

import java.util.*

data class ChartData(
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

    fun getMaxYAt(index: Int): Long {
        return ys[index].yValues.max()!!
    }

    fun getMinYAt(index: Int): Long {
        return ys[index].yValues.min()!!
    }
}

data class YValue(
    val yValues: List<Long>,
    val type: String,
    val name: String,
    val color: Int
)