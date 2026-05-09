package org.example.launchertest.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.model.LauncherApp

private val railLetters: List<Char> = ('A'..'Z').toList() + '#'

@Composable
fun LauncherHomeRoute(
    interactor: LauncherInteractor,
) {
    val vm: LauncherHomeViewModel = viewModel(
        factory = LauncherHomeViewModelFactory(interactor),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.jumpToIndex.collectLatest { index ->
            listState.animateScrollToItem(index)
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
    onToggleFavorite: (LauncherApp) -> Unit,
    onLetterSelected: (Char) -> Unit,
) {
    val dragThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }

    if (state.isSearchActive) {
        BackHandler(onBack = onSearchDismissed)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isSearchActive) {
                        if (!state.isSearchActive) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, dragAmount ->
                                    val isAtTop = listState.firstVisibleItemIndex == 0 &&
                                        listState.firstVisibleItemScrollOffset == 0
                                    if (isAtTop && dragAmount > dragThresholdPx) {
                                        onSearchActivated()
                                    }
                                },
                            )
                        }
                    },
                state = listState,
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp, end = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.apps, key = { it.packageName + it.activityName }) { app ->
                    AppRow(
                        app = app,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }

            if (state.isSearchActive) {
                SearchOverlay(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }

            AzRail(
                onLetterSelected = onLetterSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 12.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        label = { Text("Search apps") },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
private fun AzRail(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                fun pickLetter(y: Float, height: Int): Char {
                    if (height <= 0) return railLetters.first()
                    val step = height.toFloat() / railLetters.size.toFloat()
                    val raw = (y / step).toInt()
                    val idx = raw.coerceIn(0, railLetters.lastIndex)
                    return railLetters[idx]
                }

                detectDragGestures(
                    onDragStart = { offset -> onLetterSelected(pickLetter(offset.y, size.height)) },
                    onDrag = { change, _ -> onLetterSelected(pickLetter(change.position.y, size.height)) },
                )
            },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            railLetters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: LauncherApp,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    val context = LocalContext.current
    val favoritePrefix = if (app.isFavorite) "* " else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName(app.packageName, app.activityName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(launchIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to launch ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { onToggleFavorite(app) },
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = favoritePrefix + app.label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
