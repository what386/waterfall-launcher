package org.example.launchertest.ui.home

import org.example.launchertest.ui.home.applist.AppListLayout
import org.example.launchertest.ui.home.applist.buildAppListLayout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.domain.componentId
import org.example.launchertest.ui.model.LauncherApp

class LauncherHomeViewModel(
    private val interactor: LauncherInteractor,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val apps = interactor.launcherAppsFlow(query)
    private val favoriteOrder = interactor.favoriteOrderFlow()

    private val _jumpToTarget = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val jumpToTarget: SharedFlow<Int> = _jumpToTarget

    val uiState: StateFlow<LauncherHomeUiState> = combine(
        query,
        isSearchActive,
        apps,
        favoriteOrder,
    ) { search, searchActive, launcherApps, storedFavoriteOrder ->
        val listLayout = buildAppListLayout(launcherApps, storedFavoriteOrder)
        LauncherHomeUiState(
            query = search,
            isSearchActive = searchActive,
            listLayout = listLayout,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherHomeUiState(),
    )

    fun onQueryChanged(newQuery: String) { query.value = newQuery }
    fun onSearchActivated() { isSearchActive.value = true }
    fun onSearchDismissed() { isSearchActive.value = false; query.value = "" }

    fun onLetterSelected(letter: Char) {
        val targetIndex = uiState.value.listLayout.letterJumpTargets[letter] ?: return
        _jumpToTarget.tryEmit(targetIndex)
    }

    fun onToggleFavorite(app: LauncherApp) {
        viewModelScope.launch { interactor.toggleFavorite(app) }
    }

    fun onHideApp(app: LauncherApp) {
        viewModelScope.launch { interactor.hideApp(app) }
    }

    fun onFavoriteOrderChanged(orderedFavorites: List<LauncherApp>) {
        viewModelScope.launch {
            interactor.setFavoriteOrder(orderedFavorites.map { it.componentId() })
        }
    }
}

data class LauncherHomeUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val listLayout: AppListLayout = AppListLayout(
        favorites = emptyList(),
        apps = emptyList(),
        letterJumpTargets = emptyMap(),
    ),
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
