package com.what386.waterfall.ui.home.azrail

import kotlin.math.exp
import kotlin.math.roundToInt

internal data class AzRailMotionPx(
    val baseAmplitudePx: Float,
    val leftPullAmplitudePx: Float,
    val rightPullAmplitudePx: Float,
    val leftPullSetpointPx: Float,
    val rightPullSetpointPx: Float,
)

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

internal fun railPullFor(
    touchX: Float,
    railWidthPx: Float,
    pullDistancePx: Float,
): Float {
    if (railWidthPx <= 0f || pullDistancePx <= 0f) return 0f
    val railCenterX = railWidthPx / 2f
    return ((railCenterX - touchX) / pullDistancePx)
        .coerceIn(AZ_RAIL_MIN_PULL, AZ_RAIL_MAX_PULL)
}

internal fun railLeftPull(pull: Float): Float = pull.coerceAtLeast(0f)

internal fun railRightPull(pull: Float): Float = (-pull).coerceAtLeast(0f)

internal fun railAmplitudeFor(
    leftPull: Float,
    rightPull: Float,
    motionPx: AzRailMotionPx,
): Float {
    return motionPx.baseAmplitudePx +
        motionPx.leftPullAmplitudePx * leftPull -
        motionPx.rightPullAmplitudePx * rightPull
}

internal fun railSetpointFor(
    leftPull: Float,
    rightPull: Float,
    motionPx: AzRailMotionPx,
): Float {
    return motionPx.leftPullSetpointPx * leftPull +
        motionPx.rightPullSetpointPx * rightPull
}

internal fun railPeakXFor(
    leftPull: Float,
    rightPull: Float,
    motionPx: AzRailMotionPx,
): Float {
    return railSetpointFor(leftPull, rightPull, motionPx) -
        railAmplitudeFor(leftPull, rightPull, motionPx)
}

internal fun railPeakWidthFor(leftPull: Float, rightPull: Float): Float {
    return AZ_RAIL_BASE_PEAK_WIDTH +
        AZ_RAIL_LEFT_PULL_PEAK_WIDTH * leftPull -
        AZ_RAIL_RIGHT_PULL_PEAK_WIDTH * rightPull
}

internal fun railScaleBoostFor(leftPull: Float, rightPull: Float): Float {
    return AZ_RAIL_BASE_SCALE_BOOST +
        AZ_RAIL_LEFT_PULL_SCALE_BOOST * leftPull -
        AZ_RAIL_RIGHT_PULL_SCALE_BOOST * rightPull
}

internal fun railInfluenceFor(
    distanceFromSelected: Float,
    activeFraction: Float,
    peakWidth: Float,
): Float {
    if (peakWidth <= 0f) return 0f
    return activeFraction * exp(
        -(distanceFromSelected * distanceFromSelected) / peakWidth
    )
}

internal fun railItemTranslationXFor(
    influence: Float,
    leftPull: Float,
    rightPull: Float,
    motionPx: AzRailMotionPx,
): Float {
    return railSetpointFor(leftPull, rightPull, motionPx) -
        railAmplitudeFor(leftPull, rightPull, motionPx) * influence
}
