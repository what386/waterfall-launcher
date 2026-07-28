package com.what386.waterfall.ui.home

import com.what386.waterfall.ui.home.applist.buildAppListLayout
import com.what386.waterfall.ui.model.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListLayoutTest {
    @Test
    fun buildsJumpTargetsWithoutFavorites() {
        val layout =
            buildAppListLayout(
                listOf(
                    app("Alpha"),
                    app("Alto"),
                    app("Beta"),
                    app("1Password"),
                ),
                favoriteOrder = emptyList(),
            )

        assertEquals(emptyList<LauncherApp>(), layout.favorites)
        assertEquals(1, layout.letterJumpTargets['A'])
        assertEquals(3, layout.letterJumpTargets['B'])
        assertEquals(4, layout.letterJumpTargets['#'])
    }

    @Test
    fun keepsAppListIndexingWithoutFavoritesSectionRows() {
        val layout =
            buildAppListLayout(
                listOf(
                    app("Alpha", favorite = true),
                    app("Camera", favorite = true),
                    app("Browser"),
                    app("Calculator"),
                    app("Delta"),
                ),
                favoriteOrder = emptyList(),
            )

        assertEquals(2, layout.favorites.size)
        assertEquals(1, layout.letterJumpTargets['A'])
        assertEquals(3, layout.letterJumpTargets['B'])
        assertEquals(2, layout.letterJumpTargets['C'])
        assertEquals(5, layout.letterJumpTargets['D'])
    }

    @Test
    fun rebuildsTargetsForFilteredLists() {
        val layout =
            buildAppListLayout(
                listOf(
                    app("Maps"),
                    app("Messages"),
                ),
                favoriteOrder = emptyList(),
            )

        assertEquals(mapOf('M' to 1), layout.letterJumpTargets)
    }

    @Test
    fun ordersFavoritesByStoredFavoriteOrderThenAppendsUnorderedFavorites() {
        val alpha = app("Alpha", favorite = true)
        val camera = app("Camera", favorite = true)
        val browser = app("Browser", favorite = true)

        val layout =
            buildAppListLayout(
                apps = listOf(alpha, browser, camera),
                favoriteOrder =
                    listOf(
                        "${camera.packageName}/${camera.activityName}",
                        "${alpha.packageName}/${alpha.activityName}",
                    ),
            )

        assertEquals(
            listOf(camera, alpha, browser),
            layout.favorites,
        )
    }

    private fun app(
        label: String,
        favorite: Boolean = false,
    ): LauncherApp {
        val id = label.lowercase()
        return LauncherApp(
            label = label,
            packageName = "pkg.$id",
            activityName = "pkg.$id.MainActivity",
            isFavorite = favorite,
        )
    }
}
