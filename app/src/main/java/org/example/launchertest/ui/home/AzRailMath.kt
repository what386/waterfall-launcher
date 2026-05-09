package org.example.launchertest.ui.home

import kotlin.math.roundToInt

internal fun railItemCenter(index: Int, itemCount: Int, railHeightPx: Float): Float {
    if (itemCount <= 0) return Float.NaN
    return (index + 0.5f) * railHeightPx / itemCount
}

internal fun pickRailIndex(y: Float, itemCount: Int, railHeightPx: Float): Int {
    if (itemCount <= 0 || railHeightPx <= 0f) return -1
    val itemHeight = railHeightPx / itemCount
    return ((y / itemHeight) - 0.5f)
        .roundToInt()
        .coerceIn(0, itemCount - 1)
}

internal fun shiftedRailOffsetFor(
    y: Float,
    currentOffset: Float,
    railHeightPx: Float,
): Float {
    val yInShiftedRail = y - currentOffset
    return when {
        yInShiftedRail < 0f -> currentOffset + yInShiftedRail
        railHeightPx > 0f && yInShiftedRail > railHeightPx ->
            currentOffset + yInShiftedRail - railHeightPx
        else -> currentOffset
    }
}
