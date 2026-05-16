package org.example.launchertest.ui.home

internal data class HomeLayoutMetrics(
    val categoryPinOffsetDp: Float,
    val railHeightFraction: Float,
    val railYOffsetDp: Float,
    val homeRailHitYOffsetDp: Float,
    val homeRailHitHeightFraction: Float,
    val railEndPaddingDp: Float,
    val railWidthDp: Float,
    val searchButtonEndPaddingDp: Float,
    val searchButtonBottomPaddingDp: Float,
    val searchFieldHorizontalPaddingDp: Float,
    val appListContentStartPaddingDp: Float,
    val appListContentEndPaddingDp: Float,
    val appListContentTopPaddingDp: Float,
    val appListContentBottomPaddingDp: Float,
    val appListSearchTopPaddingDp: Float,
    val appListSearchBottomPaddingDp: Float,
    val favoritesTopMarginDp: Float,
    val favoritesCenterBiasUpDp: Float,
)

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
    val railHeightDp = (usableHeightDp * BASELINE_RAIL_HEIGHT_FRACTION)
        .coerceIn(300f, 440f)
    val homeRailHitHeightDp = (usableHeightDp * BASELINE_HOME_RAIL_HIT_HEIGHT_FRACTION)
        .coerceIn(92f, 150f)

    return HomeLayoutMetrics(
        categoryPinOffsetDp = scaleDp(340f, heightScale, min = 260f, max = 410f),
        railHeightFraction = (railHeightDp / usableHeightDp).coerceIn(0.38f, 0.58f),
        railYOffsetDp = scaleDp(95f, heightScale, min = 62f, max = 120f),
        homeRailHitYOffsetDp = -scaleDp(105f, heightScale, min = 72f, max = 132f),
        homeRailHitHeightFraction = (homeRailHitHeightDp / usableHeightDp).coerceIn(0.12f, 0.2f),
        railEndPaddingDp = scaleDp(28f, widthScale, min = 22f, max = 34f),
        railWidthDp = scaleDp(36f, widthScale, min = 34f, max = 40f),
        searchButtonEndPaddingDp = scaleDp(28f, widthScale, min = 22f, max = 34f),
        searchButtonBottomPaddingDp = maxOf(
            scaleDp(40f, heightScale, min = 28f, max = 52f),
            navigationBarBottomDp + 20f,
        ),
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
    )
}

private fun scaleDp(
    baselineDp: Float,
    scale: Float,
    min: Float,
    max: Float,
): Float = (baselineDp * scale).coerceIn(min, max)

private const val BASELINE_SCREEN_WIDTH_DP = 360f
private const val BASELINE_USABLE_HEIGHT_DP = 800f
private const val BASELINE_RAIL_HEIGHT_FRACTION = 0.5f
private const val BASELINE_HOME_RAIL_HIT_HEIGHT_FRACTION = 0.16f
private const val MIN_SCREEN_WIDTH_DP = 320f
private const val MIN_USABLE_HEIGHT_DP = 560f
