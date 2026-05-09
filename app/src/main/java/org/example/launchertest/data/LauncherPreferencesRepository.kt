package org.example.launchertest.data

import android.content.Context
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
    private val hiddenKey = stringSetPreferencesKey("hidden_components")
    private val widgetIdsKey = stringPreferencesKey("widget_ids")

    val favorites: Flow<Set<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
    }

    val hiddenApps: Flow<Set<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[hiddenKey] ?: emptySet()
    }

    val widgetIds: Flow<List<Int>> = context.launcherPrefs.data.map { prefs ->
        prefs[widgetIdsKey]
            ?.split(",")
            ?.mapNotNull { rawId -> rawId.toIntOrNull() }
            ?: emptyList()
    }

    suspend fun toggleFavorite(componentId: String) {
        context.launcherPrefs.edit { prefs ->
            val current = prefs[favoritesKey]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(componentId)) {
                current.remove(componentId)
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
}
