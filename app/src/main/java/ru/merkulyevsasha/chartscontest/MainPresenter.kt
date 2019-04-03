package ru.merkulyevsasha.chartscontest

import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.sources.Example

class MainPresenter(private val sourceDataConverter: SourceDataConverter) {

    private var view: IMainView? = null

    fun onUnbind() {
        view = null
    }

    fun onBind(view: IMainView) {
        this.view = view
    }

    fun dealWithIt(source: List<Example>) {
        val result = mutableListOf<ChartData>()
        for (example in source) {
            val chartData = sourceDataConverter.getChartData(example)
            result.add(chartData)
        }
        view?.showCharts(result)
    }

}