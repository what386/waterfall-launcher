package org.example.launchertest.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherPrefs by preferencesDataStore(name = "launcher_prefs")

class FavoritesRepository(
    private val context: Context,
) {
    private val favoritesKey = stringSetPreferencesKey("favorite_components")

    val favorites: Flow<Set<String>> = context.launcherPrefs.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
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
}
