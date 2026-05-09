package org.example.launchertest.ui.home

import org.example.launchertest.ui.model.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppListLayoutTest {
    @Test
    fun buildsJumpTargetsWithoutFavorites() {
        val layout = buildAppListLayout(
            listOf(
                app("Alpha"),
                app("Alto"),
                app("Beta"),
                app("1Password"),
            ),
        )

        assertEquals(emptyList<LauncherApp>(), layout.favorites)
        assertEquals(LetterJumpTarget(lazyListIndex = 0), layout.letterJumpTargets['A'])
        assertEquals(LetterJumpTarget(lazyListIndex = 2), layout.letterJumpTargets['B'])
        assertEquals(LetterJumpTarget(lazyListIndex = 3), layout.letterJumpTargets['#'])
    }

    @Test
    fun accountsForFavoriteRowsAndSpacerInLazyListIndex() {
        val layout = buildAppListLayout(
            listOf(
                app("Alpha", favorite = true),
                app("Camera", favorite = true),
                app("Browser"),
                app("Calculator"),
                app("Delta"),
            ),
        )

        assertEquals(2, layout.favorites.size)
        assertEquals(LetterJumpTarget(lazyListIndex = 5), layout.letterJumpTargets['B'])
        assertEquals(LetterJumpTarget(lazyListIndex = 6), layout.letterJumpTargets['C'])
        assertEquals(LetterJumpTarget(lazyListIndex = 7), layout.letterJumpTargets['D'])
        assertFalse(layout.letterJumpTargets.containsKey('A'))
    }

    @Test
    fun rebuildsTargetsForFilteredLists() {
        val layout = buildAppListLayout(
            listOf(
                app("Maps"),
                app("Messages"),
            ),
        )

        assertEquals(mapOf('M' to LetterJumpTarget(lazyListIndex = 0)), layout.letterJumpTargets)
    }

    private fun app(label: String, favorite: Boolean = false): LauncherApp {
        val id = label.lowercase()
        return LauncherApp(
            label = label,
            packageName = "pkg.$id",
            activityName = "pkg.$id.MainActivity",
            isFavorite = favorite,
        )
    }
}
