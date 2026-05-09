package org.example.launchertest.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.FavoritesRepository
import org.example.launchertest.ui.model.LauncherApp

class LauncherInteractor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    fun launcherAppsFlow(query: Flow<String>): Flow<List<LauncherApp>> {
        val allApps = appRepository.loadLauncherApps()
        return combine(favoritesRepository.favorites, query) { favorites, rawQuery ->
            val normalizedQuery = rawQuery.trim().lowercase()
            allApps
                .asSequence()
                .map { app ->
                    val componentId = app.componentId()
                    app.copy(isFavorite = componentId in favorites)
                }
                .filter { app ->
                    normalizedQuery.isBlank() || app.label.lowercase().contains(normalizedQuery)
                }
                .sortedWith(
                    compareByDescending<LauncherApp> { it.isFavorite }
                        .thenBy { it.label.lowercase() }
                )
                .toList()
        }
    }

    suspend fun toggleFavorite(app: LauncherApp) {
        favoritesRepository.toggleFavorite(app.componentId())
    }
}

fun LauncherApp.componentId(): String = "$packageName/$activityName"
