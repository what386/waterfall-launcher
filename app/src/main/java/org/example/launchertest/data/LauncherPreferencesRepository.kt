package org.example.launchertest.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherPrefs by preferencesDataStore(name = "launcher_prefs")

class LauncherPreferencesRepository(
    private val context: Context,
) {
    private val favoritesKey = stringSetPreferencesKey("favorite_components")
    private val favoriteOrderKey = stringPreferencesKey("favorite_order")
    private val hiddenKey = stringSetPreferencesKey("hidden_components")
    private val widgetIdsKey = stringPreferencesKey("widget_ids")
    private val autoOpenUnambiguousSearchKey = booleanPreferencesKey("auto_open_unambiguous_search")

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

    val autoOpenUnambiguousSearch: Flow<Boolean> = context.launcherPrefs.data.map { prefs ->
        prefs[autoOpenUnambiguousSearchKey] ?: false
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

    suspend fun setFavoriteOrder(componentIds: List<String>) {
        context.launcherPrefs.edit { prefs ->
            prefs[favoriteOrderKey] = componentIds.dedupComponentIds().joinToString(",")
        }
    }

    suspend fun setAutoOpenUnambiguousSearch(enabled: Boolean) {
        context.launcherPrefs.edit { prefs ->
            prefs[autoOpenUnambiguousSearchKey] = enabled
        }
    }

    suspend fun addWidgetId(appWidgetId: Int) {
        context.launcherPrefs.edit { prefs ->
            val current = prefs[widgetIdsKey].toWidgetIdList()
            if (appWidgetId !in current) {
                prefs[widgetIdsKey] = (current + appWidgetId).joinToString(",")
            }
        }
    }

    suspend fun removeWidgetId(appWidgetId: Int) {
        context.launcherPrefs.edit { prefs ->
            val updated = prefs[widgetIdsKey].toWidgetIdList()
                .filterNot { it == appWidgetId }
            prefs[widgetIdsKey] = updated.joinToString(",")
        }
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
