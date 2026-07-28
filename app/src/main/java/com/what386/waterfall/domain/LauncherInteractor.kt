package com.what386.waterfall.domain

import com.what386.waterfall.data.AppRepository
import com.what386.waterfall.data.HomeRowNavigationMode
import com.what386.waterfall.data.LauncherFont
import com.what386.waterfall.data.LauncherPreferencesRepository
import com.what386.waterfall.data.LauncherSettings
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

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
        val launcherApps =
            appRepository
                .launcherAppsFlow()
                .onEach { apps ->
                    preferencesRepository.removeUnavailableApps(apps.map { it.componentId }.toSet())
                }

        return combine(
            launcherApps,
            preferencesRepository.favorites,
            preferencesRepository.hiddenApps,
            query,
            hiddenMode,
        ) { allApps, favorites, hiddenApps, rawQuery, isHiddenMode ->
            allApps
                .asSequence()
                .filter { app -> (app.componentId in hiddenApps) == isHiddenMode }
                .map { app ->
                    val componentId = app.componentId
                    app.copy(isFavorite = componentId in favorites)
                }.filter { app ->
                    app.matchesSearch(rawQuery)
                }.sortedWith(
                    compareBy(
                        { app -> normalizedSearchText(app.label) },
                        { app -> app.componentId },
                    ),
                ).toList()
        }
    }

    suspend fun toggleFavorite(app: LauncherApp) {
        preferencesRepository.toggleFavorite(app.componentId)
    }

    suspend fun hideApp(app: LauncherApp) {
        preferencesRepository.hideApp(app.componentId)
    }

    suspend fun unhideApp(app: LauncherApp) {
        preferencesRepository.unhideApp(app.componentId)
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

    suspend fun setHideSearchButton(enabled: Boolean) {
        preferencesRepository.setHideSearchButton(enabled)
    }

    suspend fun setCleanHomeScreen(enabled: Boolean) {
        preferencesRepository.setCleanHomeScreen(enabled)
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
