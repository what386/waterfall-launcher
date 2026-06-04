package org.example.launchertest.ui.home

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.sqrt

internal data class HomeLayoutMetrics(
    val categoryPinOffsetDp: Float,
    val railHeightFraction: Float,
    val railYOffsetDp: Float,
    val homeRailHitYOffsetDp: Float,
    val homeRailHitHeightFraction: Float,
    val railEndPaddingDp: Float,
    val railWidthDp: Float,
    val searchButtonEdgePaddingDp: Float,
    val searchFieldHorizontalPaddingDp: Float,
    val appListContentStartPaddingDp: Float,
    val appListContentEndPaddingDp: Float,
    val appListContentTopPaddingDp: Float,
    val appListContentBottomPaddingDp: Float,
    val appListSearchTopPaddingDp: Float,
    val appListSearchBottomPaddingDp: Float,
    val favoritesTopMarginDp: Float,
    val favoritesCenterBiasUpDp: Float,
    val favoritesBottomSpacerDp: Float,
    val cleanHomeWidgetTopPaddingDp: Float,
    val searchDragThresholdDp: Float,
    val searchFieldVerticalPaddingDp: Float,
    val searchButtonSizeDp: Float,
    val searchButtonRailGapDp: Float,
    val sectionHeaderStartPaddingDp: Float,
    val sectionHeaderTopPaddingDp: Float,
    val sectionHeaderBottomPaddingDp: Float,
    val rowHorizontalPaddingDp: Float,
    val rowVerticalPaddingDp: Float,
    val rowIconSpacingDp: Float,
    val appRowIconSizeDp: Float,
    val favoriteRowIconSizeDp: Float,
    val appListBucketSpacerHeightDp: Float,
    val hiddenModeHorizontalPaddingDp: Float,
    val hiddenModeBottomPaddingDp: Float,
    val hiddenModeLabelHorizontalPaddingDp: Float,
    val hiddenModeLabelVerticalPaddingDp: Float,
    val sheetHorizontalPaddingDp: Float,
    val sheetBottomPaddingDp: Float,
    val sheetTitleBottomPaddingDp: Float,
    val sheetRowVerticalPaddingDp: Float,
    val sheetIconSizeDp: Float,
    val sheetContentStartPaddingDp: Float,
    val widgetRowStartPaddingDp: Float,
    val widgetRowEndPaddingDp: Float,
    val widgetRowTopPaddingDp: Float,
    val widgetRowBottomPaddingDp: Float,
    val widgetMinHeightDp: Float,
    val addWidgetBottomPaddingDp: Float,
    val widgetDragHandleTopPaddingDp: Float,
    val widgetDragHandleEndPaddingDp: Float,
    val widgetPageIndicatorSpacingDp: Float,
    val widgetPageIndicatorTopPaddingDp: Float,
    val widgetPageIndicatorSelectedSizeDp: Float,
    val widgetPageIndicatorSizeDp: Float,
    val widgetContextMenuDragThresholdDp: Float,
    val reorderActiveShadowYDp: Float,
    val editDragHandleHorizontalPaddingDp: Float,
    val azRailSpotlightSizeDp: Float,
    val azRailPullDistanceDp: Float,
    val azRailSpotlightGapDp: Float,
    val azRailBaseAmplitudeDp: Float,
    val azRailLeftPullAmplitudeDp: Float,
    val azRailRightPullAmplitudeDp: Float,
    val azRailLeftPullSetpointDp: Float,
    val azRailRightPullSetpointDp: Float,
)

internal val LocalHomeLayoutMetrics = staticCompositionLocalOf {
    calculateHomeLayoutMetrics(
        screenWidthDp = BASELINE_SCREEN_WIDTH_DP,
        screenHeightDp = BASELINE_USABLE_HEIGHT_DP,
        statusBarTopDp = 0f,
        navigationBarBottomDp = 0f,
    )
}

internal fun calculateHomeLayoutMetrics(
    screenWidthDp: Float,
    screenHeightDp: Float,
    statusBarTopDp: Float,
    navigationBarBottomDp: Float,
): HomeLayoutMetrics {
    val safeWidthDp = screenWidthDp.coerceAtLeast(MIN_SCREEN_WIDTH_DP)
    val usableHeightDp = (screenHeightDp - statusBarTopDp - navigationBarBottomDp)
        .coerceAtLeast(MIN_USABLE_HEIGHT_DP)
    val heightScale = (usableHeightDp / BASELINE_USABLE_HEIGHT_DP).coerceIn(0.78f, 1.18f)
    val widthScale = (safeWidthDp / BASELINE_SCREEN_WIDTH_DP).coerceIn(0.86f, 1.14f)
    val visualScale = sqrt(widthScale * heightScale).coerceIn(0.86f, 1.12f)
    val spacingScale = ((visualScale * 0.62f) + (heightScale * 0.38f)).coerceIn(0.82f, 1.16f)
    val horizontalScale = ((visualScale * 0.4f) + (widthScale * 0.6f)).coerceIn(0.84f, 1.14f)
    val touchScale = visualScale.coerceIn(0.9f, 1.12f)
    val railHeightDp = (usableHeightDp * BASELINE_RAIL_HEIGHT_FRACTION)
        .coerceIn(300f, 440f)
    val homeRailHitHeightDp = (usableHeightDp * BASELINE_HOME_RAIL_HIT_HEIGHT_FRACTION)
        .coerceIn(92f, 150f)

    return HomeLayoutMetrics(
        categoryPinOffsetDp = scaleDp(340f, heightScale, min = 260f, max = 410f),
        railHeightFraction = (railHeightDp / usableHeightDp).coerceIn(0.38f, 0.58f),
        railYOffsetDp = scaleDp(120f, heightScale, min = 84f, max = 148f),
        homeRailHitYOffsetDp = -scaleDp(130f, heightScale, min = 94f, max = 158f),
        homeRailHitHeightFraction = (homeRailHitHeightDp / usableHeightDp).coerceIn(0.12f, 0.2f),
        railEndPaddingDp = scaleDp(28f, widthScale, min = 22f, max = 34f),
        railWidthDp = scaleDp(36f, widthScale, min = 34f, max = 40f),
        searchButtonEdgePaddingDp = scaleDp(28f, widthScale, min = 22f, max = 34f),
        searchFieldHorizontalPaddingDp = scaleDp(28f, widthScale, min = 20f, max = 36f),
        appListContentStartPaddingDp = scaleDp(28f, widthScale, min = 22f, max = 34f),
        appListContentEndPaddingDp = scaleDp(40f, widthScale, min = 32f, max = 48f),
        appListContentTopPaddingDp = scaleDp(16f, heightScale, min = 12f, max = 22f),
        appListContentBottomPaddingDp = scaleDp(24f, heightScale, min = 18f, max = 32f),
        appListSearchTopPaddingDp = maxOf(
            scaleDp(96f, heightScale, min = 76f, max = 116f),
            statusBarTopDp + 56f,
        ),
        appListSearchBottomPaddingDp = maxOf(
            scaleDp(80f, heightScale, min = 62f, max = 104f),
            navigationBarBottomDp + 44f,
        ),
        favoritesTopMarginDp = scaleDp(88f, heightScale, min = 52f, max = 112f),
        favoritesCenterBiasUpDp = scaleDp(44f, heightScale, min = 26f, max = 56f),
        favoritesBottomSpacerDp = scaleDpByRatio(24f, spacingScale),
        cleanHomeWidgetTopPaddingDp = scaleDp(96f, heightScale, min = 64f, max = 116f),
        searchDragThresholdDp = scaleDpByRatio(18f, touchScale, minRatio = 0.9f, maxRatio = 1.12f),
        searchFieldVerticalPaddingDp = scaleDpByRatio(12f, spacingScale),
        searchButtonSizeDp = scaleDpByRatio(48f, touchScale, minRatio = 0.92f, maxRatio = 1.12f),
        searchButtonRailGapDp = scaleDpByRatio(12f, spacingScale),
        sectionHeaderStartPaddingDp = scaleDpByRatio(20f, horizontalScale),
        sectionHeaderTopPaddingDp = scaleDpByRatio(8f, spacingScale),
        sectionHeaderBottomPaddingDp = scaleDpByRatio(2f, spacingScale, minRatio = 0.8f, maxRatio = 1.25f),
        rowHorizontalPaddingDp = scaleDpByRatio(20f, horizontalScale),
        rowVerticalPaddingDp = scaleDpByRatio(9f, spacingScale),
        rowIconSpacingDp = scaleDpByRatio(16f, horizontalScale),
        appRowIconSizeDp = scaleDpByRatio(35.2f, touchScale, minRatio = 0.92f, maxRatio = 1.12f),
        favoriteRowIconSizeDp = scaleDpByRatio(41.6f, touchScale, minRatio = 0.92f, maxRatio = 1.12f),
        appListBucketSpacerHeightDp = scaleDpByRatio(8f, spacingScale),
        hiddenModeHorizontalPaddingDp = scaleDpByRatio(20f, horizontalScale),
        hiddenModeBottomPaddingDp = scaleDpByRatio(18f, spacingScale),
        hiddenModeLabelHorizontalPaddingDp = scaleDpByRatio(12f, horizontalScale),
        hiddenModeLabelVerticalPaddingDp = scaleDpByRatio(7f, spacingScale),
        sheetHorizontalPaddingDp = scaleDpByRatio(20f, horizontalScale),
        sheetBottomPaddingDp = scaleDpByRatio(28f, spacingScale),
        sheetTitleBottomPaddingDp = scaleDpByRatio(8f, spacingScale),
        sheetRowVerticalPaddingDp = scaleDpByRatio(12f, spacingScale),
        sheetIconSizeDp = scaleDpByRatio(40f, touchScale, minRatio = 0.9f, maxRatio = 1.1f),
        sheetContentStartPaddingDp = scaleDpByRatio(12f, horizontalScale),
        widgetRowStartPaddingDp = scaleDpByRatio(20f, horizontalScale),
        widgetRowEndPaddingDp = scaleDpByRatio(20f, horizontalScale),
        widgetRowTopPaddingDp = scaleDpByRatio(8f, spacingScale),
        widgetRowBottomPaddingDp = 0f,
        widgetMinHeightDp = scaleDpByRatio(120f, spacingScale, minRatio = 0.86f, maxRatio = 1.12f),
        addWidgetBottomPaddingDp = scaleDpByRatio(8f, spacingScale),
        widgetDragHandleTopPaddingDp = scaleDpByRatio(12f, spacingScale),
        widgetDragHandleEndPaddingDp = scaleDpByRatio(12f, horizontalScale),
        widgetPageIndicatorSpacingDp = scaleDpByRatio(6f, horizontalScale),
        widgetPageIndicatorTopPaddingDp = scaleDpByRatio(2f, spacingScale, minRatio = 0.8f, maxRatio = 1.25f),
        widgetPageIndicatorSelectedSizeDp = scaleDpByRatio(5f, touchScale, minRatio = 0.9f, maxRatio = 1.12f),
        widgetPageIndicatorSizeDp = scaleDpByRatio(4f, touchScale, minRatio = 0.9f, maxRatio = 1.12f),
        widgetContextMenuDragThresholdDp = scaleDpByRatio(24f, touchScale, minRatio = 0.92f, maxRatio = 1.12f),
        reorderActiveShadowYDp = scaleDpByRatio(8f, spacingScale),
        editDragHandleHorizontalPaddingDp = scaleDpByRatio(6f, horizontalScale),
        azRailSpotlightSizeDp = scaleDpByRatio(56f, touchScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailPullDistanceDp = scaleDpByRatio(120f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailSpotlightGapDp = scaleDpByRatio(-28f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailBaseAmplitudeDp = scaleDpByRatio(76f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailLeftPullAmplitudeDp = scaleDpByRatio(109f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailRightPullAmplitudeDp = scaleDpByRatio(33f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailLeftPullSetpointDp = scaleDpByRatio(-7f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
        azRailRightPullSetpointDp = scaleDpByRatio(38f, horizontalScale, minRatio = 0.9f, maxRatio = 1.12f),
    )
}

private fun scaleDp(
    baselineDp: Float,
    scale: Float,
    min: Float,
    max: Float,
): Float = (baselineDp * scale).coerceIn(min, max)

private fun scaleDpByRatio(
    baselineDp: Float,
    scale: Float,
    minRatio: Float = 0.86f,
    maxRatio: Float = 1.14f,
): Float {
    val min = baselineDp * minRatio
    val max = baselineDp * maxRatio
    return if (baselineDp >= 0f) {
        (baselineDp * scale).coerceIn(min, max)
    } else {
        (baselineDp * scale).coerceIn(max, min)
    }
}

private const val BASELINE_SCREEN_WIDTH_DP = 360f
private const val BASELINE_USABLE_HEIGHT_DP = 800f
private const val BASELINE_RAIL_HEIGHT_FRACTION = 0.5f
private const val BASELINE_HOME_RAIL_HIT_HEIGHT_FRACTION = 0.16f
private const val MIN_SCREEN_WIDTH_DP = 320f
private const val MIN_USABLE_HEIGHT_DP = 560f
