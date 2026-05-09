package org.example.launchertest.ui.home

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
    val favoritesSectionItemCount = if (favorites.isNotEmpty()) favorites.size + 2 else 0
    val appListStartIndex = 1 + favoritesSectionItemCount
    val jumpTargets = linkedMapOf<Char, LetterJumpTarget>()

    apps.forEachIndexed { appIndex, app ->
        if (app.isFavorite) return@forEachIndexed

        val bucket = bucketFor(app.label)
        if (bucket !in jumpTargets) {
            jumpTargets[bucket] = LetterJumpTarget(lazyListIndex = appListStartIndex + appIndex)
        }
    }

    return AppListLayout(
        favorites = favorites,
        apps = apps,
        letterJumpTargets = jumpTargets,
    )
}
