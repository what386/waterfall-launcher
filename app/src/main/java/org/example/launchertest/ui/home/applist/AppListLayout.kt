package org.example.launchertest.ui.home.applist

import org.example.launchertest.ui.model.LauncherApp

data class AppListLayout(
    val favorites: List<LauncherApp>,
    val apps: List<LauncherApp>,
    val letterJumpTargets: Map<Char, Int>,
)

internal fun buildAppListLayout(apps: List<LauncherApp>): AppListLayout {
    val favorites = apps.filter { it.isFavorite }
    val appListStartIndex = 1 // category_pin_spacer

    val jumpTargets = linkedMapOf<Char, Int>()

    apps.forEachIndexed { appIndex, app ->
        val bucket = bucketFor(app.label)

        if (bucket !in jumpTargets) {
            jumpTargets[bucket] = appListStartIndex + appIndex
        }
    }

    return AppListLayout(
        favorites = favorites,
        apps = apps,
        letterJumpTargets = jumpTargets,
    )
}
