package com.what386.waterfall.domain

import com.what386.waterfall.ui.model.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSearchTest {
    @Test
    fun ranksExactThenPrefixThenWordThenInitials() {
        val apps =
            listOf(
                app("Material Files"),
                app("Maps"),
                app("City Maps"),
            )

        assertEquals("Maps", bestSearchMatch(apps, "maps")?.label)
        assertEquals("Material Files", bestSearchMatch(apps, "material")?.label)
        assertEquals("City Maps", bestSearchMatch(apps, "city m")?.label)
        assertEquals("Material Files", bestSearchMatch(apps, "mf")?.label)
    }

    @Test
    fun matchesCaseAndDiacritics() {
        assertEquals("Café", bestSearchMatch(listOf(app("Café")), "CAFE")?.label)
    }

    @Test
    fun blankQueryHasNoLaunchCandidate() {
        assertNull(bestSearchMatch(listOf(app("Maps")), "  "))
    }

    private fun app(label: String): LauncherApp =
        LauncherApp(
            label = label,
            packageName = "example.${label.replace(" ", "").lowercase()}",
            activityName = "MainActivity",
        )
}
