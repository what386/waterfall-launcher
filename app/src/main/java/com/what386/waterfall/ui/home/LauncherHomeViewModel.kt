package com.what386.waterfall.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.what386.waterfall.data.HomeRowNavigationMode
import com.what386.waterfall.data.LauncherFont
import com.what386.waterfall.data.LauncherSettings
import com.what386.waterfall.domain.LauncherInteractor
import com.what386.waterfall.ui.home.applist.AppListLayout
import com.what386.waterfall.ui.home.applist.buildAppListLayout
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherHomeViewModel(
    private val interactor: LauncherInteractor,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val isHiddenMode = MutableStateFlow(false)
    private val contentMode = MutableStateFlow(HomeContentMode.Favorites)
    private val apps = interactor.launcherAppsFlow(query, isHiddenMode)
    private val favoriteOrder = interactor.favoriteOrderFlow()
    private val settings = interactor.settingsFlow()

    private val _jumpToTarget =
        MutableSharedFlow<Int>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val jumpToTarget: SharedFlow<Int> = _jumpToTarget

    private val baseUiState =
        combine(
            query,
            isSearchActive,
            isHiddenMode,
            apps,
            favoriteOrder,
        ) { search, searchActive, hiddenMode, launcherApps, storedFavoriteOrder ->
            val listLayout = buildAppListLayout(launcherApps, storedFavoriteOrder)
            LauncherHomeUiState(
                query = search,
                isSearchActive = searchActive,
                isHiddenMode = hiddenMode,
                listLayout = listLayout,
            )
        }

    val uiState: StateFlow<LauncherHomeUiState> =
        combine(
            baseUiState,
            settings,
            contentMode,
        ) { state, launcherSettings, mode ->
            state.copy(
                settings = launcherSettings,
                contentMode = mode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherHomeUiState(),
        )

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun onSearchActivated() {
        contentMode.value = HomeContentMode.Search
        isSearchActive.value = true
    }

    fun onSearchDismissed() {
        isSearchActive.value = false
        query.value = ""
        contentMode.value = if (isHiddenMode.value) HomeContentMode.Apps else HomeContentMode.Favorites
    }

    fun onAppListActivated() {
        isSearchActive.value = false
        query.value = ""
        contentMode.value = HomeContentMode.Apps
    }

    fun onFavoritesActivated() {
        isSearchActive.value = false
        isHiddenMode.value = false
        query.value = ""
        contentMode.value = HomeContentMode.Favorites
    }

    fun onHiddenModeChanged(enabled: Boolean) {
        isHiddenMode.value = enabled
        contentMode.value = if (enabled) HomeContentMode.Apps else HomeContentMode.Favorites
    }

    fun onLetterSelected(letter: Char) {
        contentMode.value = HomeContentMode.Apps
        val targetIndex = uiState.value.listLayout.letterJumpTargets[letter] ?: return
        _jumpToTarget.tryEmit(targetIndex)
    }

    fun onToggleFavorite(app: LauncherApp) {
        viewModelScope.launch { interactor.toggleFavorite(app) }
    }

    fun onHideApp(app: LauncherApp) {
        viewModelScope.launch { interactor.hideApp(app) }
    }

    fun onUnhideApp(app: LauncherApp) {
        viewModelScope.launch { interactor.unhideApp(app) }
    }

    fun onFavoriteOrderChanged(orderedFavorites: List<LauncherApp>) {
        viewModelScope.launch {
            interactor.setFavoriteOrder(orderedFavorites.map { it.componentId })
        }
    }

    fun onHideStatusBarChanged(enabled: Boolean) {
        viewModelScope.launch { interactor.setHideStatusBar(enabled) }
    }

    fun onHideAppIconsChanged(enabled: Boolean) {
        viewModelScope.launch { interactor.setHideAppIcons(enabled) }
    }

    fun onHideSearchButtonChanged(enabled: Boolean) {
        viewModelScope.launch { interactor.setHideSearchButton(enabled) }
    }

    fun onCleanHomeScreenChanged(enabled: Boolean) {
        viewModelScope.launch { interactor.setCleanHomeScreen(enabled) }
    }

    fun onHomeRowNavigationModeChanged(mode: HomeRowNavigationMode) {
        viewModelScope.launch { interactor.setHomeRowNavigationMode(mode) }
    }

    fun onFontChanged(font: LauncherFont) {
        viewModelScope.launch { interactor.setFont(font) }
    }

    fun onResetSettings() {
        viewModelScope.launch { interactor.resetSettings() }
    }
}

data class LauncherHomeUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val isHiddenMode: Boolean = false,
    val contentMode: HomeContentMode = HomeContentMode.Favorites,
    val listLayout: AppListLayout =
        AppListLayout(
            favorites = emptyList(),
            apps = emptyList(),
            letterJumpTargets = emptyMap(),
        ),
    val settings: LauncherSettings = LauncherSettings(),
)

enum class HomeContentMode {
    Favorites,
    Apps,
    Search,
}

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
