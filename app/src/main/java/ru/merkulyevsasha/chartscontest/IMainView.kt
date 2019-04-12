package ru.merkulyevsasha.chartscontest

import ru.merkulyevsasha.chartscontest.models.ChartData

interface IMainView {
    fun showCharts(chartData: List<ChartData>)
    fun updateChart(index: Int, chartData: ChartData)
}