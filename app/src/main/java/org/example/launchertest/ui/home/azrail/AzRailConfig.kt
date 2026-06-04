package org.example.launchertest.ui.home.azrail

private const val FavoritesRailItem = '★'

fun buildRailLetters(letterJumpTargets: Map<Char, Int>): List<Char> {
    return listOf(FavoritesRailItem) + letterJumpTargets.keys.toList()
}

fun isFavoritesRailItem(item: Char): Boolean = item == FavoritesRailItem

internal const val AZ_RAIL_MIN_PULL = -2f
internal const val AZ_RAIL_MAX_PULL = 2f

internal const val AZ_RAIL_PULL_SPRING_STIFFNESS = 420f
internal const val AZ_RAIL_PULL_SPRING_DAMPING = 0.82f

internal const val AZ_RAIL_SPOTLIGHT_Y_STIFFNESS = 600f
internal const val AZ_RAIL_SPOTLIGHT_Y_DAMPING = 0.75f
internal const val AZ_RAIL_SPOTLIGHT_ALPHA_STIFFNESS = 500f
internal const val AZ_RAIL_SPOTLIGHT_ALPHA_DAMPING = 1f

internal const val AZ_RAIL_SELECTED_INDEX_STIFFNESS = 420f
internal const val AZ_RAIL_SELECTED_INDEX_DAMPING = 0.82f

internal const val AZ_RAIL_ACTIVE_STIFFNESS = 380f
internal const val AZ_RAIL_ACTIVE_DAMPING = 0.9f

internal const val AZ_RAIL_DRAG_Y_STIFFNESS = 520f
internal const val AZ_RAIL_DRAG_Y_DAMPING = 0.78f

internal const val AZ_RAIL_BASE_PEAK_WIDTH = 8f
internal const val AZ_RAIL_LEFT_PULL_PEAK_WIDTH = 24f
internal const val AZ_RAIL_RIGHT_PULL_PEAK_WIDTH = 2f

internal const val AZ_RAIL_BASE_SCALE_BOOST = 0.5f
internal const val AZ_RAIL_LEFT_PULL_SCALE_BOOST = 0.08f
internal const val AZ_RAIL_RIGHT_PULL_SCALE_BOOST = 0.18f

internal const val AZ_RAIL_SELECTED_ALPHA_THRESHOLD = 0.9f
internal const val AZ_RAIL_INACTIVE_ALPHA = 0.85f
