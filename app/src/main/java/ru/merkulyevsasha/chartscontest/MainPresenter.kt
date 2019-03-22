package ru.merkulyevsasha.chartscontest

import ru.merkulyevsasha.chartscontest.sources.Example
import java.util.*

class MainPresenter(private val sourceDataConverter: SourceDataConverter) {

    private var view: IMainView? = null

    fun onUnbind() {
        view = null
    }

    fun onBind(view: IMainView) {
        this.view = view
    }

    fun dealWithIt(source: List<Example>) {
        val example = source[Random().nextInt(5)]
        val chartData = sourceDataConverter.getChartData(example)
        view?.showCharts(chartData)
    }

}