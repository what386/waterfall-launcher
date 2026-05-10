package org.example.launchertest.ui.home.applist

import org.example.launchertest.ui.model.LauncherApp

data class LetterJumpTarget(
    val lazyListIndex: Int,
)

data class AppListLayout(
    val favorites: List<LauncherApp>,
    val apps: List<LauncherApp>,
    val letterJumpTargets: Map<Char, LetterJumpTarget>,
)

internal fun buildAppListLayout(apps: List<LauncherApp>): AppListLayout {
    val favorites = apps.filter { it.isFavorite }

    val favoritesSectionItemCount = if (favorites.isNotEmpty()) {
        1 + favorites.size + 1 // header + rows + bottom spacer
    } else {
        0
    }

    val appListStartIndex = 1 + favoritesSectionItemCount // category_pin_spacer + favorites section

    val jumpTargets = linkedMapOf<Char, LetterJumpTarget>()

    apps.forEachIndexed { appIndex, app ->
        val bucket = bucketFor(app.label)

        if (bucket !in jumpTargets) {
            jumpTargets[bucket] = LetterJumpTarget(
                lazyListIndex = appListStartIndex + appIndex,
            )
        }
    }

    return AppListLayout(
        favorites = favorites,
        apps = apps,
        letterJumpTargets = jumpTargets,
    )
}
