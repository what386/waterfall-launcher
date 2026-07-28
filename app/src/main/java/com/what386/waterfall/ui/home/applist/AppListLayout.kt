package com.what386.waterfall.ui.home.applist

import com.what386.waterfall.ui.model.LauncherApp

data class AppListLayout(
    val favorites: List<LauncherApp>,
    val apps: List<LauncherApp>,
    val letterJumpTargets: Map<Char, Int>,
)

internal fun buildAppListLayout(
    apps: List<LauncherApp>,
    favoriteOrder: List<String>,
): AppListLayout {
    val favoriteApps = apps.filter { it.isFavorite }
    val favoritesById = favoriteApps.associateBy { it.componentId }
    val orderedFavorites =
        favoriteOrder
            .mapNotNull(favoritesById::get)
    val orderedFavoriteIds = favoriteOrder.toSet()

    val appendedFavorites = favoriteApps.filter { it.componentId !in orderedFavoriteIds }
    val favorites = orderedFavorites + appendedFavorites
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
