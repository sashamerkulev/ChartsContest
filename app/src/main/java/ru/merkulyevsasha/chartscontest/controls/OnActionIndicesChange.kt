package ru.merkulyevsasha.chartscontest.controls

interface OnActionIndicesChange {
    fun onActionIndicesChanged(startIndex: Int, stopIndex: Int)
    fun onActionStartIndexChanged(startIndex: Int)
    fun onActionStopIndexChanged(stopIndex: Int)
}

interface OnDataChange {
    fun onDataChanged(
        minX: Long,
        minY: Long,
        maxX: Long,
        maxY: Long,
        xScale: Float,
        yScale: Float,
        chartLines: List<BaseChart.ChartLine>,
        yShouldVisible: Map<Int, Boolean>
    )
}