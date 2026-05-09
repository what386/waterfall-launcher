package org.example.launchertest.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.example.launchertest.domain.LauncherInteractor

@Composable
fun LauncherHomeRoute(interactor: LauncherInteractor) {
    val vm: LauncherHomeViewModel = viewModel(
        factory = LauncherHomeViewModelFactory(interactor),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        vm.jumpToIndex.collectLatest { index ->
            listState.scrollToItem(index)
        }
    }
    LauncherHomeScreen(
        state = state,
        listState = listState,
        onQueryChanged = vm::onQueryChanged,
        onSearchActivated = vm::onSearchActivated,
        onSearchDismissed = vm::onSearchDismissed,
        onToggleFavorite = vm::onToggleFavorite,
        onLetterSelected = vm::onLetterSelected,
    )
}

@Composable
private fun LauncherHomeScreen(
    state: LauncherHomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onQueryChanged: (String) -> Unit,
    onSearchActivated: () -> Unit,
    onSearchDismissed: () -> Unit,
    onToggleFavorite: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onLetterSelected: (Char) -> Unit,
) {
    var scrubbingLetter by remember { mutableStateOf<Char?>(null) }

    if (state.isSearchActive) BackHandler(onBack = onSearchDismissed)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            AppListPanel(
                apps = state.apps,
                favorites = state.favorites,
                scrubbingLetter = scrubbingLetter,
                isSearchActive = state.isSearchActive,
                listState = listState,
                onSearchActivated = onSearchActivated,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isSearchActive) {
                SearchOverlay(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }

            AzRail(
                letters = state.letterIndexMap.keys.toList(),
                onLetterSelected = onLetterSelected,
                onScrubStart = { letter -> scrubbingLetter = letter },
                onScrubMove  = { letter -> scrubbingLetter = letter },
                onScrubEnd   = { scrubbingLetter = null },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.5f)
                    .padding(end = 4.dp),
            )
        }
    }
}
