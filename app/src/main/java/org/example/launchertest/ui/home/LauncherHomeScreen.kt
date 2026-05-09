package org.example.launchertest.ui.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.exp
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
                                    val isAtBottom =
                                        !listState.canScrollForward
                                    if (isAtBottom && dragAmount < -dragThresholdPx) {
                                        onSearchActivated()
                                    }
                                },
                            )
                        }
                    },
                state = listState,
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = if (state.isSearchActive) 80.dp else 24.dp,
                    end = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Favorites section
                if (state.favorites.isNotEmpty()) {
                    items(
                        items = state.favorites,
                        key = { app -> app.packageName + app.activityName + "_fav" },
                    ) { app ->
                        AppRow(
                            app = app,
                            isFavorite = true,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // A–Z app list with bucket headers
                itemsIndexed(
                    items = state.apps,
                    key = { _, app -> app.packageName + app.activityName },
                ) { index, app ->
                    val currentBucket = bucketFor(app.label)
                    val previousBucket = if (index > 0) bucketFor(state.apps[index - 1].label) else null

                    if (previousBucket == null || currentBucket != previousBucket) {
                        if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentBucket.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }

                    AppRow(
                        app = app,
                        isFavorite = false,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }

            // Search overlay anchored to bottom
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
                onLetterSelected = onLetterSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 12.dp, bottom = 12.dp),
            )
        }
    }
}

private fun bucketFor(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
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
    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun AzRail(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
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
                awaitEachGesture {
                    var down = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull()
                    while (down == null || !down.pressed) {
                        down = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull()
                    }
                    var currentLetter = pickLetter(down.position.y, size.height)
                    selectedIndex = railLetters.indexOf(currentLetter)
                    onLetterSelected(currentLetter)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        if (!change.pressed) break
                        val letter = pickLetter(change.position.y, size.height)
                        if (letter != currentLetter) {
                            currentLetter = letter
                            selectedIndex = railLetters.indexOf(letter)
                            onLetterSelected(letter)
                        }
                        change.consume()
                    }
                    waitForUpOrCancellation(PointerEventPass.Main)?.consume()
                    selectedIndex = -1
                }
            },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            railLetters.forEachIndexed { index, letter ->
                val influence = if (selectedIndex < 0) {
                    0f
                } else {
                    val d = abs(index - selectedIndex).toFloat()
                    exp(-(d * d) / 6f)
                }
                val xOffset by animateFloatAsState(
                    targetValue = -18f * influence,
                    animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                    label = "railOffset",
                )
                val scale by animateFloatAsState(
                    targetValue = 1f + (0.42f * influence),
                    animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                    label = "railScale",
                )
                val alpha by animateFloatAsState(
                    targetValue = 0.55f + (0.45f * influence),
                    animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                    label = "railAlpha",
                )
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.graphicsLayer {
                        translationX = xOffset
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: LauncherApp,
    isFavorite: Boolean,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(app.packageName)

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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isFavorite) 40.dp else 32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = app.label,
            style = if (isFavorite) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.titleMedium,
        )
    }
}
