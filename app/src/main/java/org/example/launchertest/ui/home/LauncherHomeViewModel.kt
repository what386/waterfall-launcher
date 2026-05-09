package org.example.launchertest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
    private val isSearchActive = MutableStateFlow(false)
    private val apps = interactor.launcherAppsFlow(query)

    private val _jumpToIndex = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val jumpToIndex: SharedFlow<Int> = _jumpToIndex

    val uiState: StateFlow<LauncherHomeUiState> = combine(query, isSearchActive, apps) {
            search, searchActive, launcherApps ->
        val indexMap = buildLetterIndexMap(launcherApps)
        LauncherHomeUiState(
            query = search,
            isSearchActive = searchActive,
            favorites = launcherApps.filter { it.isFavorite },
            apps = launcherApps,
            letterIndexMap = indexMap,
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
        val idx = uiState.value.letterIndexMap[letter] ?: return
        _jumpToIndex.tryEmit(idx)
    }

    fun onToggleFavorite(app: LauncherApp) {
        viewModelScope.launch { interactor.toggleFavorite(app) }
    }
}

data class LauncherHomeUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val favorites: List<LauncherApp> = emptyList(),
    val apps: List<LauncherApp> = emptyList(),
    val letterIndexMap: Map<Char, Int> = emptyMap(),
)

private fun buildLetterIndexMap(apps: List<LauncherApp>): Map<Char, Int> {
    val indexMap = linkedMapOf<Char, Int>()
    apps.forEachIndexed { index, app ->
        if (app.isFavorite) return@forEachIndexed
        val letter = bucketFor(app.label) // uses AppListPanel.bucketFor (internal, same package)
        if (letter !in indexMap) indexMap[letter] = index
    }
    return indexMap
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
