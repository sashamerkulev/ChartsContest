package ru.merkulyevsasha.chartscontest.controls

interface OnActionIndicesChange {
    fun onActionIndicesChanged(startIndex: Int, stopIndex: Int)
    fun onActionStartIndexChanged(startIndex: Int)
    fun onActionStopIndexChanged(stopIndex: Int)
}