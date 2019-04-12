package ru.merkulyevsasha.chartscontest

import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.sources.Example

class MainPresenter(private val sourceDataConverter: SourceDataConverter) {

    private var view: IMainView? = null

    private val chartNames = HashMap<Int, String>()

    init {
        chartNames.put(0, "Followers")
        chartNames.put(1, "Interactions")
        chartNames.put(2, "Fruits")
        chartNames.put(3, "Views")
        chartNames.put(4, "Fruits again")
    }

    fun onUnbind() {
        view = null
    }

    fun onBind(view: IMainView) {
        this.view = view
    }

    fun dealWithIt(source: List<Example>) {
        val result = mutableListOf<ChartData>()
        for (index in 0 until source.size) {
            val example = source[index]
            val chartData = sourceDataConverter.getChartData(chartNames[index]!!, example)
            result.add(chartData)
        }
        view?.showCharts(result)
    }

    fun dealWithIt(index: Int, example: Example) {
        val chartData = sourceDataConverter.getChartData("", example)
        view?.updateChart(index, chartData)
    }
}