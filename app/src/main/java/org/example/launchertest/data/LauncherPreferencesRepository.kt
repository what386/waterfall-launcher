package org.example.launchertest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.launchertest.widgets.WidgetStack
import org.example.launchertest.widgets.decodeWidgetStacks
import org.example.launchertest.widgets.encodeWidgetStacks
import org.example.launchertest.widgets.widgetStacksFromWidgetIds

private val Context.launcherPrefs by preferencesDataStore(name = "launcher_prefs")

class LauncherPreferencesRepository(
    private val context: Context,
) {
    private val favoritesKey = stringSetPreferencesKey("favorite_components")
    private val favoriteOrderKey = stringPreferencesKey("favorite_order")
    private val hiddenKey = stringSetPreferencesKey("hidden_components")
    private val widgetIdsKey = stringPreferencesKey("widget_ids")
    private val widgetStacksKey = stringPreferencesKey("widget_stacks")
    private val hideStatusBarKey = booleanPreferencesKey("hide_status_bar")
    private val hideAppIconsKey = booleanPreferencesKey("hide_app_icons")
    private val cleanHomeScreenKey = booleanPreferencesKey("clean_home_screen")
    private val homeRowNavigationModeKey = stringPreferencesKey("home_row_navigation_mode")
    private val fontKey = stringPreferencesKey("font")

    val favorites: Flow<Set<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
    }

    val hiddenApps: Flow<Set<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[hiddenKey] ?: emptySet()
    }

    val favoriteOrder: Flow<List<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[favoriteOrderKey].toComponentIdList()
    }

    val widgetIds: Flow<List<Int>> = context.launcherPrefs.data.map { prefs ->
        prefs[widgetIdsKey]
            ?.split(",")
            ?.mapNotNull { rawId -> rawId.toIntOrNull() }
            ?: emptyList()
    }

    val widgetStacks: Flow<List<WidgetStack>> = context.launcherPrefs.data.map { prefs ->
        prefs[widgetStacksKey]
            ?.let(::decodeWidgetStacks)
            ?: widgetStacksFromWidgetIds(prefs[widgetIdsKey].toWidgetIdList())
    }

    val settings: Flow<LauncherSettings> = context.launcherPrefs.data.map { prefs ->
        LauncherSettings(
            hideStatusBar = prefs[hideStatusBarKey] ?: false,
            hideAppIcons = prefs[hideAppIconsKey] ?: false,
            cleanHomeScreen = prefs[cleanHomeScreenKey] ?: false,
            homeRowNavigationMode = HomeRowNavigationMode.fromStorageValue(prefs[homeRowNavigationModeKey]),
            font = LauncherFont.fromStorageValue(prefs[fontKey]),
        )
    }

    suspend fun toggleFavorite(componentId: String) {
        context.launcherPrefs.edit { prefs ->
            val current = prefs[favoritesKey]?.toMutableSet() ?: mutableSetOf()
            val favoriteOrder = prefs[favoriteOrderKey].toComponentIdList()

            if (current.add(componentId)) {
                val updatedOrder = (favoriteOrder + componentId).dedupComponentIds()
                prefs[favoriteOrderKey] = updatedOrder.joinToString(",")
            } else {
                current.remove(componentId)
                val updatedOrder = favoriteOrder.filterNot { it == componentId }
                prefs[favoriteOrderKey] = updatedOrder.joinToString(",")
            }
            prefs[favoritesKey] = current
        }
    }

    suspend fun hideApp(componentId: String) {
        context.launcherPrefs.edit { prefs ->
            val hidden = prefs[hiddenKey]?.toMutableSet() ?: mutableSetOf()
            hidden.add(componentId)
            prefs[hiddenKey] = hidden

            val favorites = prefs[favoritesKey]?.toMutableSet() ?: mutableSetOf()
            if (favorites.remove(componentId)) {
                prefs[favoritesKey] = favorites
            }

            val favoriteOrder = prefs[favoriteOrderKey].toComponentIdList()
                .filterNot { it == componentId }
            prefs[favoriteOrderKey] = favoriteOrder.joinToString(",")
        }
    }

    suspend fun unhideApp(componentId: String) {
        context.launcherPrefs.edit { prefs ->
            val hidden = prefs[hiddenKey]?.toMutableSet() ?: mutableSetOf()
            hidden.remove(componentId)
            prefs[hiddenKey] = hidden
        }
    }

    suspend fun setFavoriteOrder(componentIds: List<String>) {
        context.launcherPrefs.edit { prefs ->
            prefs[favoriteOrderKey] = componentIds.dedupComponentIds().joinToString(",")
        }
    }

    suspend fun removeUnavailableApps(availableComponentIds: Set<String>) {
        if (availableComponentIds.isEmpty()) return

        context.launcherPrefs.edit { prefs ->
            val favorites = prefs[favoritesKey] ?: emptySet()
            val cleanedFavorites = favorites.filterTo(mutableSetOf()) { it in availableComponentIds }
            if (cleanedFavorites != favorites) {
                prefs[favoritesKey] = cleanedFavorites
            }

            val hiddenApps = prefs[hiddenKey] ?: emptySet()
            val cleanedHiddenApps = hiddenApps.filterTo(mutableSetOf()) { it in availableComponentIds }
            if (cleanedHiddenApps != hiddenApps) {
                prefs[hiddenKey] = cleanedHiddenApps
            }

            val favoriteOrder = prefs[favoriteOrderKey].toComponentIdList()
            val cleanedFavoriteOrder = favoriteOrder.filter { it in availableComponentIds }.dedupComponentIds()
            if (cleanedFavoriteOrder != favoriteOrder) {
                prefs[favoriteOrderKey] = cleanedFavoriteOrder.joinToString(",")
            }
        }
    }

    suspend fun addWidgetId(appWidgetId: Int) {
        context.launcherPrefs.edit { prefs ->
            val stacks = currentWidgetStacks(prefs) + WidgetStack(listOf(appWidgetId))
            prefs.setWidgetStacks(stacks)
        }
    }

    suspend fun addWidgetIdToStack(appWidgetId: Int, stackIndex: Int) {
        context.launcherPrefs.edit { prefs ->
            val stacks = currentWidgetStacks(prefs).toMutableList()
            if (stackIndex !in stacks.indices) {
                stacks.add(WidgetStack(listOf(appWidgetId)))
            } else if (appWidgetId !in stacks[stackIndex].widgetIds) {
                stacks[stackIndex] = stacks[stackIndex].copy(
                    widgetIds = stacks[stackIndex].widgetIds + appWidgetId,
                )
            }
            prefs.setWidgetStacks(stacks)
        }
    }

    suspend fun removeWidgetId(appWidgetId: Int) {
        context.launcherPrefs.edit { prefs ->
            val stacks = currentWidgetStacks(prefs)
                .map { stack -> stack.copy(widgetIds = stack.widgetIds.filterNot { it == appWidgetId }) }
                .filter { stack -> stack.widgetIds.isNotEmpty() }
            prefs.setWidgetStacks(stacks)
        }
    }

    suspend fun setWidgetIds(appWidgetIds: List<Int>) {
        context.launcherPrefs.edit { prefs ->
            prefs.setWidgetStacks(widgetStacksFromWidgetIds(appWidgetIds))
        }
    }

    suspend fun setWidgetStacks(stacks: List<WidgetStack>) {
        context.launcherPrefs.edit { prefs ->
            prefs.setWidgetStacks(stacks)
        }
    }

    suspend fun setHideStatusBar(enabled: Boolean) {
        context.launcherPrefs.edit { prefs ->
            prefs[hideStatusBarKey] = enabled
        }
    }

    suspend fun setHideAppIcons(enabled: Boolean) {
        context.launcherPrefs.edit { prefs ->
            prefs[hideAppIconsKey] = enabled
        }
    }

    suspend fun setCleanHomeScreen(enabled: Boolean) {
        context.launcherPrefs.edit { prefs ->
            prefs[cleanHomeScreenKey] = enabled
        }
    }

    suspend fun setHomeRowNavigationMode(mode: HomeRowNavigationMode) {
        context.launcherPrefs.edit { prefs ->
            prefs[homeRowNavigationModeKey] = mode.storageValue
        }
    }

    suspend fun setFont(font: LauncherFont) {
        context.launcherPrefs.edit { prefs ->
            prefs[fontKey] = font.storageValue
        }
    }

    suspend fun resetSettings() {
        context.launcherPrefs.edit { prefs ->
            prefs.remove(hideStatusBarKey)
            prefs.remove(hideAppIconsKey)
            prefs.remove(cleanHomeScreenKey)
            prefs.remove(homeRowNavigationModeKey)
            prefs.remove(fontKey)
        }
    }

    private fun currentWidgetStacks(
        prefs: androidx.datastore.preferences.core.Preferences,
    ): List<WidgetStack> {
        return prefs[widgetStacksKey]
            ?.let(::decodeWidgetStacks)
            ?: widgetStacksFromWidgetIds(prefs[widgetIdsKey].toWidgetIdList())
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.setWidgetStacks(
        stacks: List<WidgetStack>,
    ) {
        val cleanedStacks = stacks
            .map { stack -> stack.copy(widgetIds = stack.widgetIds.distinct()) }
            .filter { stack -> stack.widgetIds.isNotEmpty() }
        this[widgetStacksKey] = encodeWidgetStacks(cleanedStacks)
        this[widgetIdsKey] = cleanedStacks.flatMap { it.widgetIds }.distinct().joinToString(",")
    }

    private fun String?.toWidgetIdList(): List<Int> {
        return this
            ?.split(",")
            ?.mapNotNull { rawId -> rawId.toIntOrNull() }
            ?: emptyList()
    }

    private fun String?.toComponentIdList(): List<String> {
        return this
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    private fun List<String>.dedupComponentIds(): List<String> {
        return this.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}
