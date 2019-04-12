package ru.merkulyevsasha.chartscontest.controls

interface OnActionIndicesChange {
    fun onActionIndicesChanged(startIndex: Int, stopIndex: Int)
    fun onActionStartIndexChanged(startIndex: Int)
    fun onActionStopIndexChanged(stopIndex: Int)
}

interface OnDataChange {
    fun onDataChanged(
        startIndex: Int,
        stopIndex: Int,
        minX: Long,
        maxX: Long,
        xScale: Float,
        yMinMaxValues: Map<Int, BaseChart.MinMaxValues>,
        yScale: Map<Int, Float>,
        chartLines: List<BaseChart.ChartLineExt>,
        yShouldVisible: Map<Int, Boolean>
    )
}