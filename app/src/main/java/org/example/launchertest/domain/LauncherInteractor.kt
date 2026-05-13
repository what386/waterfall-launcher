package org.example.launchertest.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.HomeRowNavigationMode
import org.example.launchertest.data.LauncherFont
import org.example.launchertest.data.LauncherPreferencesRepository
import org.example.launchertest.data.LauncherSettings
import org.example.launchertest.ui.model.LauncherApp

class LauncherInteractor(
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
) {
    fun favoriteOrderFlow(): Flow<List<String>> = preferencesRepository.favoriteOrder

    fun settingsFlow(): Flow<LauncherSettings> = preferencesRepository.settings

    fun launcherAppsFlow(
        query: Flow<String>,
        hiddenMode: Flow<Boolean>,
    ): Flow<List<LauncherApp>> {
        val allApps = appRepository.loadLauncherApps()
        return combine(preferencesRepository.favorites, preferencesRepository.hiddenApps, query, hiddenMode) {
                favorites, hiddenApps, rawQuery, isHiddenMode ->
            val normalizedQuery = rawQuery.trim().lowercase()
            allApps
                .asSequence()
                .filter { app -> (app.componentId() in hiddenApps) == isHiddenMode }
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

    suspend fun unhideApp(app: LauncherApp) {
        preferencesRepository.unhideApp(app.componentId())
    }

    suspend fun setFavoriteOrder(componentIds: List<String>) {
        preferencesRepository.setFavoriteOrder(componentIds)
    }

    suspend fun setHideStatusBar(enabled: Boolean) {
        preferencesRepository.setHideStatusBar(enabled)
    }

    suspend fun setHideAppIcons(enabled: Boolean) {
        preferencesRepository.setHideAppIcons(enabled)
    }

    suspend fun setHomeRowNavigationMode(mode: HomeRowNavigationMode) {
        preferencesRepository.setHomeRowNavigationMode(mode)
    }

    suspend fun setFont(font: LauncherFont) {
        preferencesRepository.setFont(font)
    }

    suspend fun resetSettings() {
        preferencesRepository.resetSettings()
    }

}

fun LauncherApp.componentId(): String = "$packageName/$activityName"
