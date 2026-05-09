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
    val jumpTargets = linkedMapOf<Char, LetterJumpTarget>()

    var lazyListIndex = 1 // category_pin_spacer

    if (favorites.isNotEmpty()) {
        lazyListIndex += 1 // favorites_header
        lazyListIndex += favorites.size
        lazyListIndex += 1 // favorites bottom spacer
    }

    var previousBucket: Char? = null

    apps.forEach { app ->
        val bucket = bucketFor(app.label)

        if (previousBucket == null || bucket != previousBucket) {
            jumpTargets.putIfAbsent(bucket, LetterJumpTarget(lazyListIndex))
            lazyListIndex += 1 // section header
            previousBucket = bucket
        }

        lazyListIndex += 1 // app row
    }

    return AppListLayout(
        favorites = favorites,
        apps = apps,
        letterJumpTargets = jumpTargets,
    )
}
