package org.example.launchertest.ui.home.azrail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzRailMathTest {
    @Test
    fun railItemCenterUsesEvenSlots() {
        assertEquals(10f, railItemCenter(index = 0, itemCount = 5, railHeightPx = 100f))
        assertEquals(50f, railItemCenter(index = 2, itemCount = 5, railHeightPx = 100f))
        assertEquals(90f, railItemCenter(index = 4, itemCount = 5, railHeightPx = 100f))
    }

    @Test
    fun railItemCenterReturnsNanWhenEmpty() {
        assertTrue(railItemCenter(index = 0, itemCount = 0, railHeightPx = 100f).isNaN())
    }

    @Test
    fun pickRailIndexChoosesNearestCenter() {
        assertEquals(0, pickRailIndex(y = -20f, itemCount = 5, railHeightPx = 100f))
        assertEquals(0, pickRailIndex(y = 10f, itemCount = 5, railHeightPx = 100f))
        assertEquals(2, pickRailIndex(y = 50f, itemCount = 5, railHeightPx = 100f))
        assertEquals(4, pickRailIndex(y = 130f, itemCount = 5, railHeightPx = 100f))
    }

    @Test
    fun pickRailIndexReturnsNegativeOneWithoutGeometry() {
        assertEquals(-1, pickRailIndex(y = 10f, itemCount = 0, railHeightPx = 100f))
        assertEquals(-1, pickRailIndex(y = 10f, itemCount = 5, railHeightPx = 0f))
    }

    @Test
    fun shiftedRailOffsetMovesOnlyWhenFingerLeavesShiftedRail() {
        assertEquals(0f, shiftedRailOffsetFor(y = 50f, currentOffset = 0f, railHeightPx = 100f))
        assertEquals(20f, shiftedRailOffsetFor(y = 120f, currentOffset = 0f, railHeightPx = 100f))
        assertEquals(20f, shiftedRailOffsetFor(y = 80f, currentOffset = 20f, railHeightPx = 100f))
        assertEquals(-10f, shiftedRailOffsetFor(y = -10f, currentOffset = 20f, railHeightPx = 100f))
    }
}
