package org.example.launchertest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.model.LauncherApp

class LauncherHomeViewModel(
    private val interactor: LauncherInteractor,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val apps = interactor.launcherAppsFlow(query)

    val uiState: StateFlow<LauncherHomeUiState> = combine(query, apps) { search, launcherApps ->
        LauncherHomeUiState(
            query = search,
            favorites = launcherApps.filter { app -> app.isFavorite },
            apps = launcherApps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherHomeUiState(),
    )

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun onToggleFavorite(app: LauncherApp) {
        viewModelScope.launch {
            interactor.toggleFavorite(app)
        }
    }
}

data class LauncherHomeUiState(
    val query: String = "",
    val favorites: List<LauncherApp> = emptyList(),
    val apps: List<LauncherApp> = emptyList(),
)

class LauncherHomeViewModelFactory(
    private val interactor: LauncherInteractor,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LauncherHomeViewModel(interactor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
