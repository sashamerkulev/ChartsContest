package ru.merkulyevsasha.chartscontest

import android.graphics.Color
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.YValue
import ru.merkulyevsasha.chartscontest.sources.Colors
import ru.merkulyevsasha.chartscontest.sources.Example
import ru.merkulyevsasha.chartscontest.sources.Names
import ru.merkulyevsasha.chartscontest.sources.Types
import java.util.*

class SourceDataConverter {

    fun getChartData(title: String, example: Example): ChartData {
        val xValues = mutableListOf<Date>()
        val xValuesInDays = mutableListOf<Long>()
        val xValuesInHours = mutableListOf<Long>()
        val yValues = mutableListOf<YValue>()
        example.columns.forEachIndexed { index, element ->
            if (index == 0) {
                val dates = getDateValues(element)
                xValues.addAll(dates)
                val inDays = dates.map { getDateAsDays(it.time) }
                xValuesInDays.addAll(inDays)
                val inHours = dates.map { getDateAsHours(it.time) }
                xValuesInHours.addAll(inHours)
            } else {
                val type = getTypes(element[0], example.types)
                val name = getNames(element[0], example.names)
                val color = getColor(element[0], example.colors)
                val yLongValues = getValues(element)
                val yVal = YValue(
                    yLongValues,
                    type,
                    name,
                    color,
                    yLongValues.average()
                )
                yValues.add(yVal)
            }
        }
        return ChartData(
            title,
            example.y_scaled ?: false,
            example.percentage ?: false,
            example.stacked ?: false,
            xValues,
            xValuesInDays,
            xValuesInHours,
            yValues
        )
    }

    private fun getDateValues(stringValues: List<String>): List<Date> {
        val values = mutableListOf<Date>()
        for (s in stringValues) {
            try {
                val value = s.toLong()
                values.add(Date(value))
            } catch (_: Exception) {
            }
        }
        return values
    }

    private fun getValues(stringValues: List<String>): List<Long> {
        val values = mutableListOf<Long>()
        for (s in stringValues) {
            try {
                val value = s.toLong()
                values.add(value)
            } catch (_: Exception) {
            }
        }
        return values
    }

    private fun getNames(name: String, names: Names): String {
        return when (name) {
            "y0" -> names.y0
            "y1" -> names.y1
            "y2" -> names.y2
            "y3" -> names.y3
            "y4" -> names.y4
            "y5" -> names.y5
            "y6" -> names.y6
            else -> ""
        }
    }

    private fun getTypes(name: String, types: Types): String {
        return when (name) {
            "y0" -> types.y0
            "y1" -> types.y1
            "y2" -> types.y2
            "y3" -> types.y3
            "y4" -> types.y4
            "y5" -> types.y5
            "y6" -> types.y6
            else -> ""
        }
    }

    private fun getColor(name: String, colors: Colors): Int {
        return when (name) {
            "y0" -> Color.parseColor(colors.y0)
            "y1" -> Color.parseColor(colors.y1)
            "y2" -> Color.parseColor(colors.y2)
            "y3" -> Color.parseColor(colors.y3)
            "y4" -> Color.parseColor(colors.y4)
            "y5" -> Color.parseColor(colors.y5)
            "y6" -> Color.parseColor(colors.y6)
            else -> 0
        }
    }

    private fun getDateAsDays(date: Long): Long {
        return date / (1000 * 60 * 60 * 24)
    }

    private fun getDateAsHours(date: Long): Long {
        return date / (1000 * 60 * 60)
    }

}