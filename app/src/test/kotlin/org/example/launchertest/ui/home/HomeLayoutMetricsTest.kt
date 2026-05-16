package org.example.launchertest.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutMetricsTest {
    @Test
    fun baselineScreenKeepsCurrentLayoutValues() {
        val metrics = calculateHomeLayoutMetrics(
            screenWidthDp = 360f,
            screenHeightDp = 800f,
            statusBarTopDp = 0f,
            navigationBarBottomDp = 0f,
        )

        assertEquals(340f, metrics.categoryPinOffsetDp, FLOAT_DELTA)
        assertEquals(95f, metrics.railYOffsetDp, FLOAT_DELTA)
        assertEquals(-105f, metrics.homeRailHitYOffsetDp, FLOAT_DELTA)
        assertEquals(28f, metrics.railEndPaddingDp, FLOAT_DELTA)
        assertEquals(40f, metrics.searchButtonBottomPaddingDp, FLOAT_DELTA)
        assertEquals(88f, metrics.favoritesTopMarginDp, FLOAT_DELTA)
        assertEquals(44f, metrics.favoritesCenterBiasUpDp, FLOAT_DELTA)
    }

    @Test
    fun shortScreenCompactsVerticalAnchors() {
        val metrics = calculateHomeLayoutMetrics(
            screenWidthDp = 360f,
            screenHeightDp = 640f,
            statusBarTopDp = 24f,
            navigationBarBottomDp = 24f,
        )

        assertTrue(metrics.categoryPinOffsetDp < 340f)
        assertTrue(metrics.railYOffsetDp < 95f)
        assertTrue(metrics.favoritesTopMarginDp < 88f)
        assertTrue(metrics.favoritesCenterBiasUpDp < 44f)
        assertTrue(metrics.searchButtonBottomPaddingDp >= 44f)
    }

    @Test
    fun tallScreenExpandsVerticalAnchorsWithoutRunawayGrowth() {
        val metrics = calculateHomeLayoutMetrics(
            screenWidthDp = 360f,
            screenHeightDp = 960f,
            statusBarTopDp = 0f,
            navigationBarBottomDp = 0f,
        )

        assertTrue(metrics.categoryPinOffsetDp > 340f)
        assertTrue(metrics.categoryPinOffsetDp <= 410f)
        assertTrue(metrics.railYOffsetDp > 95f)
        assertTrue(metrics.favoritesTopMarginDp > 88f)
        assertTrue(metrics.favoritesTopMarginDp <= 112f)
    }

    @Test
    fun wideScreenExpandsHorizontalPaddingWithinBounds() {
        val metrics = calculateHomeLayoutMetrics(
            screenWidthDp = 480f,
            screenHeightDp = 800f,
            statusBarTopDp = 0f,
            navigationBarBottomDp = 0f,
        )

        assertTrue(metrics.appListContentStartPaddingDp > 28f)
        assertTrue(metrics.appListContentEndPaddingDp > 40f)
        assertTrue(metrics.railEndPaddingDp > 28f)
        assertTrue(metrics.railWidthDp <= 40f)
    }
}

private const val FLOAT_DELTA = 0.001f
