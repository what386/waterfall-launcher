package org.example.launchertest.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.LauncherPreferencesRepository
import org.example.launchertest.ui.model.LauncherApp

class LauncherInteractor(
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
) {
    fun favoriteOrderFlow(): Flow<List<String>> = preferencesRepository.favoriteOrder
    fun autoOpenUnambiguousSearchFlow(): Flow<Boolean> = preferencesRepository.autoOpenUnambiguousSearch

    fun launcherAppsFlow(query: Flow<String>): Flow<List<LauncherApp>> {
        val allApps = appRepository.loadLauncherApps()
        return combine(preferencesRepository.favorites, preferencesRepository.hiddenApps, query) {
                favorites, hiddenApps, rawQuery ->
            val normalizedQuery = rawQuery.trim().lowercase()
            allApps
                .asSequence()
                .filter { app -> app.componentId() !in hiddenApps }
                .map { app ->
                    val componentId = app.componentId()
                    app.copy(isFavorite = componentId in favorites)
                }
                .filter { app ->
                    normalizedQuery.isBlank() || app.label.lowercase().contains(normalizedQuery)
                }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }

    suspend fun toggleFavorite(app: LauncherApp) {
        preferencesRepository.toggleFavorite(app.componentId())
    }

    suspend fun hideApp(app: LauncherApp) {
        preferencesRepository.hideApp(app.componentId())
    }

    suspend fun setFavoriteOrder(componentIds: List<String>) {
        preferencesRepository.setFavoriteOrder(componentIds)
    }

    suspend fun setAutoOpenUnambiguousSearch(enabled: Boolean) {
        preferencesRepository.setAutoOpenUnambiguousSearch(enabled)
    }
}

fun LauncherApp.componentId(): String = "$packageName/$activityName"
