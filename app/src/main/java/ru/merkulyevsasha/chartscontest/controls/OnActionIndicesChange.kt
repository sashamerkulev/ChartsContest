package ru.merkulyevsasha.chartscontest.controls

import ru.merkulyevsasha.chartscontest.models.ChartData

interface OnActionIndicesChange {
    fun onActionIndicesChanged(startIndex: Int, stopIndex: Int)
    fun onActionStartIndexChanged(startIndex: Int)
    fun onActionStopIndexChanged(stopIndex: Int)
}

interface OnLegendClicked {
    fun onLegendClicked(point: ChartLegend.Distance)
}

interface OnDataChange {
    fun onDataChanged(
        startIndex: Int,
        stopIndex: Int,
        chartData: ChartData,
        minX: Long,
        maxX: Long,
        xScale: Float,
        yMinMaxValues: Map<Int, BaseChart.MinMaxValues>,
        yScale: Map<Int, Float>,
        chartLines: List<BaseChart.ChartLineExt>,
        yShouldVisible: Map<Int, Boolean>
    )
}